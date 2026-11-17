package it.prog3.mailserver.network;

import com.google.gson.JsonParseException;
import it.prog3.mailserver.ServerConfig;
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
import java.util.function.Consumer;

/**
 * Gestisce UNA connessione con un client, dall'apertura alla chiusura.
 * Viene eseguito da un thread del pool (è un Runnable).
 *
 * Ciclo di vita, come HTTP:
 *   1. legge una riga (la richiesta JSON)
 *   2. la fa elaborare al RequestProcessor
 *   3. scrive una riga (la risposta JSON)
 *   4. chiude la connessione
 */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private final RequestProcessor processor;
    private final Consumer<String> logger;

    public ClientHandler(Socket socket, RequestProcessor processor, Consumer<String> logger) {
        this.socket = socket;
        this.processor = processor;
        this.logger = logger;
    }

    @Override
    public void run() {
        String client = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
        logger.accept("Connessione aperta con " + client);

        // try-with-resources: socket e stream vengono chiusi automaticamente alla fine
        try (socket;
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {

            // Se il client si connette ma non scrive nulla, non teniamo il thread bloccato per sempre
            socket.setSoTimeout(ServerConfig.READ_TIMEOUT_MS);

            String line = in.readLine();
            Response response = handle(line);
            out.println(JsonUtil.toJson(response)); // una riga di risposta

        } catch (SocketTimeoutException e) {
            logger.accept("Timeout: " + client + " non ha inviato la richiesta");
        } catch (IOException e) {
            logger.accept("ERRORE di comunicazione con " + client + ": " + e.getMessage());
        } finally {
            logger.accept("Connessione chiusa con " + client);
        }
    }

    private Response handle(String line) {
        if (line == null || line.isBlank()) {
            return Response.error("Richiesta vuota");
        }
        try {
            Request request = JsonUtil.fromJson(line, Request.class);
            return processor.process(request);
        } catch (JsonParseException e) {
            logger.accept("Richiesta non comprensibile ricevuta: " + line);
            return Response.error("Richiesta malformata");
        }
    }
}
