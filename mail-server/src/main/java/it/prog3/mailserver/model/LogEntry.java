package it.prog3.mailserver.model;

import java.time.LocalTime;

/**
 * Un evento del log del server.
 *
 * È un "record": una classe immutabile i cui campi, costruttore, getter
 * (time(), category(), message()), equals e toString vengono generati da Java.
 */
public record LogEntry(LocalTime time, Category category, String message) {

    /** Tipo di evento: decide colore e visibilità nella GUI. */
    public enum Category {
        /** Avvio del server e informazioni generali. */
        INFO,
        /** Apertura / chiusura di una connessione. */
        CONNECTION,
        /** Operazioni dei client: login, invio, cancellazione, nuove email scaricate. */
        OPERATION,
        /** Connessioni di aggiornamento automatico che non hanno trovato email nuove. */
        POLLING,
        /** Errori: login falliti, destinatari inesistenti, problemi di rete o di disco. */
        ERROR
    }
}
