package it.prog3.mailserver.network;

import it.prog3.mailserver.ServerConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Client di prova del Giorno 4 (lo cancelleremo quando avremo il client vero).
 *
 * Da avviare MENTRE il server è acceso. Invia richieste scritte a mano come
 * testo, per mostrare che il protocollo è davvero solo testo su socket.
 */
public class TestClient {

    public static void main(String[] args) {
        send("{\"command\":\"LOGIN\",\"user\":\"alice@mail.it\"}");
        send("{\"command\":\"LOGIN\",\"user\":\"dave@mail.it\"}");
        send("{\"command\":\"SEND\",\"user\":\"alice@mail.it\",\"email\":{"
                + "\"recipients\":[\"bob@mail.it\",\"carol@mail.it\"],"
                + "\"subject\":\"Riunione\",\"text\":\"Ci vediamo alle 15?\"}}");
        send("{\"command\":\"SEND\",\"user\":\"alice@mail.it\",\"email\":{"
                + "\"recipients\":[\"bob@mail.it\",\"dave@mail.it\"],"
                + "\"subject\":\"Prova\",\"text\":\"...\"}}");
        send("{\"command\":\"FETCH\",\"user\":\"bob@mail.it\",\"lastId\":0}");
        send("{\"command\":\"DELETE\",\"user\":\"bob@mail.it\",\"emailId\":999}");
        send("questo non è JSON");
    }

    /** Una richiesta = una connessione: apre, scrive una riga, legge una riga, chiude. */
    private static void send(String requestLine) {
        System.out.println("→ " + requestLine);
        try (Socket socket = new Socket("localhost", ServerConfig.PORT);
             PrintWriter out = new PrintWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            out.println(requestLine);
            System.out.println("← " + in.readLine() + "\n");

        } catch (IOException e) {
            System.out.println("✗ Server non raggiungibile: " + e.getMessage() + "\n");
        }
    }
}
