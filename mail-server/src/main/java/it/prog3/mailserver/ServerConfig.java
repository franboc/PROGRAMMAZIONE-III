package it.prog3.mailserver;

import java.nio.file.Path;
import java.util.List;

/**
 * Configurazione del server raccolta in un unico punto.
 */
public final class ServerConfig {

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
