package it.prog3.mailclient.protocol;

/**
 * Le operazioni che il client può chiedere al server.
 * Ogni operazione = una connessione (come in HTTP).
 */
public enum Command {
    /** Verifica che l'indirizzo esista sul server. */
    LOGIN,
    /** Chiede le email con ID maggiore di lastId (solo quelle nuove). */
    FETCH,
    /** Invia un'email ai destinatari indicati. */
    SEND,
    /** Cancella un'email dalla inbox dell'utente. */
    DELETE
}
