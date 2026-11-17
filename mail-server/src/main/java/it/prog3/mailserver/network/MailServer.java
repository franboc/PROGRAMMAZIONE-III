package it.prog3.mailserver.network;

import it.prog3.mailserver.ServerConfig;
import it.prog3.mailserver.model.EventLogger;
import it.prog3.mailserver.model.LogEntry.Category;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Il server di rete.
 *
 * Usa due tipi di thread:
 *  - un thread "accettatore" che aspetta le connessioni (accept() è bloccante:
 *    se lo chiamassimo dal thread di JavaFX, la finestra si bloccherebbe);
 *  - un pool di thread che gestiscono le connessioni in parallelo.
 *
 * Mentre un thread del pool serve un client, l'accettatore è già pronto
 * a ricevere il successivo.
 */
public class MailServer {

    private final int port;
    private final RequestProcessor processor;
    private final EventLogger logger;

    private ServerSocket serverSocket;
    private ExecutorService pool;

    public MailServer(int port, RequestProcessor processor, EventLogger logger) {
        this.port = port;
        this.processor = processor;
        this.logger = logger;
    }

    /** Apre la porta e avvia il thread accettatore. Ritorna subito. */
    public void start() throws IOException {
        serverSocket = new ServerSocket(port); // lancia BindException se la porta è già occupata
        pool = Executors.newFixedThreadPool(ServerConfig.THREAD_POOL_SIZE);

        Thread acceptor = new Thread(this::acceptLoop, "acceptor");
        acceptor.setDaemon(true); // non impedisce alla JVM di terminare
        acceptor.start();

        logger.log(Category.INFO, "In ascolto sulla porta " + port);
    }

    private void acceptLoop() {
        while (!serverSocket.isClosed()) {
            try {
                Socket socket = serverSocket.accept();  // aspetta il prossimo client
                pool.execute(new ClientHandler(socket, processor, logger)); // lo affida al pool
            } catch (IOException e) {
                // accept() fallisce anche quando chiudiamo il ServerSocket in stop(): in quel caso è normale
                if (!serverSocket.isClosed()) {
                    logger.log(Category.ERROR, "Errore nell'accettare una connessione: " + e.getMessage());
                }
            }
        }
    }

    /** Ferma il server: non accetta più connessioni e attende la fine di quelle in corso. */
    public void stop() {
        try {
            if (serverSocket != null) {
                serverSocket.close(); // sblocca accept() e fa terminare acceptLoop
            }
        } catch (IOException ignored) {
            // stiamo chiudendo comunque
        }
        if (pool != null) {
            pool.shutdown(); // niente nuovi compiti, finisce quelli in corso
            try {
                if (!pool.awaitTermination(3, TimeUnit.SECONDS)) {
                    pool.shutdownNow();
                }
            } catch (InterruptedException e) {
                pool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
