package it.prog3.mailserver;

import java.nio.file.Path;
import java.util.List;

/**
 * Configurazione del server raccolta in un unico punto.
 */
public final class ServerConfig {

    /** Porta TCP su cui il server accetta le connessioni dei client. */
    public static final int PORT = 4444;

    /**
     * Numero di thread che gestiscono le richieste in parallelo.
     * Le richieste in più aspettano in coda: il server non crea mai thread
     * senza limite, anche se arrivano moltissime connessioni insieme.
     */
    public static final int THREAD_POOL_SIZE = 10;

    /** Tempo massimo di attesa della richiesta da un client (millisecondi). */
    public static final int READ_TIMEOUT_MS = 5000;

    /** Cartella in cui vengono salvate le caselle (relativa alla cartella mail-server). */
    public static final Path DATA_DIR = Path.of("data");

    /**
     * Account registrati sul server. La traccia chiede un numero fisso di
     * account precompilati: non è prevista la registrazione da client.
     */
    public static final List<String> ACCOUNTS = List.of(
            "alice@mail.it",
            "bob@mail.it",
            "carol@mail.it"
    );

    private ServerConfig() {
    }
}
