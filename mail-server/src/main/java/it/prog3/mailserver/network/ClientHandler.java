package it.prog3.mailserver.network;

import com.google.gson.JsonParseException;
import it.prog3.mailserver.ServerConfig;
import it.prog3.mailserver.model.EventLogger;
import it.prog3.mailserver.model.LogEntry.Category;
import it.prog3.mailserver.protocol.Command;
import it.prog3.mailserver.protocol.JsonUtil;
import it.prog3.mailserver.protocol.Request;
import it.prog3.mailserver.protocol.Response;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestisce UNA connessione con un client, dall'apertura alla chiusura.
 * Viene eseguito da un thread del pool (è un Runnable).
 *
 * Ciclo di vita, come HTTP:
 *   1. legge una riga (la richiesta JSON)
 *   2. la fa elaborare al RequestProcessor
 *   3. scrive una riga (la risposta JSON)
 *   4. chiude la connessione
 *
 * Gli eventi della connessione vengono raccolti e inviati al log tutti insieme
 * alla fine: solo allora sappiamo se era un semplice polling senza novità
 * (in quel caso li marchiamo come POLLING, così la GUI può nasconderli).
 */
public class ClientHandler implements Runnable {

    /** Un evento in attesa di essere inviato al log. */
    private record PendingEvent(Category category, String message) {
    }

    private final Socket socket;
    private final RequestProcessor processor;
    private final EventLogger logger;

    public ClientHandler(Socket socket, RequestProcessor processor, EventLogger logger) {
        this.socket = socket;
        this.processor = processor;
        this.logger = logger;
    }

    @Override
    public void run() {
        List<PendingEvent> events = new ArrayList<>(); // usata solo da questo thread: nessun lock necessario
        EventLogger connectionLog = (category, message) -> events.add(new PendingEvent(category, message));

        String client = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
        connectionLog.log(Category.CONNECTION, "Connessione aperta con " + client);
        boolean emptyPolling = false;

        // try-with-resources: socket e stream vengono chiusi automaticamente alla fine
        try (socket;
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {

            // Se il client si connette ma non scrive nulla, non teniamo il thread bloccato per sempre
            socket.setSoTimeout(ServerConfig.READ_TIMEOUT_MS);

            Request request = parse(in.readLine(), connectionLog);
            Response response = (request == null)
                    ? Response.error("Richiesta vuota o malformata")
                    : processor.process(request, connectionLog);
            out.println(JsonUtil.toJson(response)); // una riga di risposta

            emptyPolling = request != null
                    && request.getCommand() == Command.FETCH
                    && response.isOk()
                    && response.getEmails().isEmpty();

        } catch (SocketTimeoutException e) {
            connectionLog.log(Category.ERROR, "Timeout: " + client + " non ha inviato la richiesta");
        } catch (IOException e) {
            connectionLog.log(Category.ERROR, "Errore di comunicazione con " + client + ": " + e.getMessage());
        } finally {
            connectionLog.log(Category.CONNECTION, "Connessione chiusa con " + client);
            for (PendingEvent event : events) {
                logger.log(emptyPolling ? Category.POLLING : event.category(), event.message());
            }
        }
    }

    /** @return la richiesta decodificata, oppure null se la riga è vuota o non è JSON valido */
    private Request parse(String line, EventLogger log) {
        if (line == null || line.isBlank()) {
            log.log(Category.ERROR, "Ricevuta una richiesta vuota");
            return null;
        }
        try {
            return JsonUtil.fromJson(line, Request.class);
        } catch (JsonParseException e) {
            log.log(Category.ERROR, "Richiesta non comprensibile: " + line);
            return null;
        }
    }
}
