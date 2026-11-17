package it.prog3.mailserver.model;

/**
 * "Qualcuno a cui segnalare un evento".
 *
 * La parte di rete conosce solo questa interfaccia, non la GUI: nell'app vera
 * gli eventi finiscono nel ServerModel (e da lì sullo schermo), nei test
 * possono essere semplicemente stampati su console.
 */
@FunctionalInterface
public interface EventLogger {

    void log(LogEntry.Category category, String message);
}
