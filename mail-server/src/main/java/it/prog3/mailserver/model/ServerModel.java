package it.prog3.mailserver.model;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.LocalTime;

/**
 * Model della GUI del server: il log degli eventi e lo stato del server.
 *
 * Entrambi sono OSSERVABILI (ObservableList e property): la view si aggiorna
 * da sola quando cambiano, senza che il model sappia che la view esiste.
 *
 * REGOLA DI JAVAFX: gli oggetti osservati dalla GUI vanno modificati SOLO dal
 * JavaFX Application Thread. Gli eventi però arrivano dai thread di rete:
 * per questo ogni modifica passa da runOnFxThread().
 */
public class ServerModel implements EventLogger {

    /** Oltre questo numero di righe le più vecchie vengono scartate (la memoria non cresce all'infinito). */
    private static final int MAX_LOG_SIZE = 5000;

    private final ObservableList<LogEntry> log = FXCollections.observableArrayList();
    private final ReadOnlyStringWrapper status = new ReadOnlyStringWrapper("Server non attivo");

    /** La lista è osservabile ma la view la usa solo in lettura. */
    public ObservableList<LogEntry> getLog() {
        return FXCollections.unmodifiableObservableList(log);
    }

    /** Versione "sola lettura" della property: la view può osservarla, non modificarla. */
    public ReadOnlyStringProperty statusProperty() {
        return status.getReadOnlyProperty();
    }

    /** Può essere chiamato da QUALSIASI thread. */
    @Override
    public void log(LogEntry.Category category, String message) {
        // L'orario si prende SUBITO (momento dell'evento), non quando la GUI lo mostra
        LogEntry entry = new LogEntry(LocalTime.now(), category, message);
        runOnFxThread(() -> {
            log.add(entry);
            if (log.size() > MAX_LOG_SIZE) {
                log.remove(0, log.size() - MAX_LOG_SIZE);
            }
        });
    }

    public void setStatus(String text) {
        runOnFxThread(() -> status.set(text));
    }

    public void clearLog() {
        runOnFxThread(log::clear);
    }

    /**
     * Se siamo già nel thread di JavaFX esegue subito; altrimenti mette il
     * compito in coda con Platform.runLater e lo farà eseguire a quel thread.
     */
    private static void runOnFxThread(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
        } else {
            Platform.runLater(action);
        }
    }
}
