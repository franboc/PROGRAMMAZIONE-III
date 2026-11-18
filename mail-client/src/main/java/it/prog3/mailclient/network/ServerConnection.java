package it.prog3.mailclient.network;

import com.google.gson.JsonParseException;
import it.prog3.mailclient.ClientConfig;
import it.prog3.mailclient.protocol.JsonUtil;
import it.prog3.mailclient.protocol.Request;
import it.prog3.mailclient.protocol.Response;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Comunicazione con il server: UNA richiesta = UNA connessione (come HTTP).
 *
 * Il metodo send() è BLOCCANTE (aspetta la rete): non va mai chiamato dal
 * thread di JavaFX, altrimenti la finestra si congela. I controller lo
 * eseguono dentro un Task in background.
 *
 * La classe non ha stato modificabile: più thread possono usarla insieme.
 */
public class ServerConnection {

    private final String host;
    private final int port;

    public ServerConnection(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Apre la connessione, invia la richiesta, legge la risposta e chiude.
     *
     * @throws IOException se il server non è raggiungibile o non risponde correttamente
     */
    public Response send(Request request) throws IOException {
        try (Socket socket = new Socket()) {
            // connect con timeout: senza, un server irraggiungibile può bloccare per molti secondi
            socket.connect(new InetSocketAddress(host, port), ClientConfig.CONNECT_TIMEOUT_MS);
            socket.setSoTimeout(ClientConfig.READ_TIMEOUT_MS);

            PrintWriter out = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

            out.println(JsonUtil.toJson(request)); // una riga di richiesta
            String line = in.readLine();            // una riga di risposta

            if (line == null) {
                throw new IOException("Il server ha chiuso la connessione senza rispondere");
            }
            Response response = JsonUtil.fromJson(line, Response.class);
            if (response == null) {
                throw new IOException("Risposta vuota dal server");
            }
            return response;

        } catch (JsonParseException e) {
            throw new IOException("Risposta del server non comprensibile", e);
        }
        // chiudendo il socket (try-with-resources) si chiudono anche i suoi stream
    }
}
