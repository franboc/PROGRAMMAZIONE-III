package it.prog3.mailclient;

/**
 * Configurazione del client raccolta in un unico punto.
 */
public final class ClientConfig {

    /** Indirizzo e porta del mail server (devono coincidere con ServerConfig del server). */
    public static final String SERVER_HOST = "localhost";
    public static final int SERVER_PORT = 4444;

    /** Tempo massimo per stabilire la connessione (millisecondi). */
    public static final int CONNECT_TIMEOUT_MS = 2000;

    /** Tempo massimo di attesa della risposta del server (millisecondi). */
    public static final int READ_TIMEOUT_MS = 5000;

    private ClientConfig() {
    }
}
