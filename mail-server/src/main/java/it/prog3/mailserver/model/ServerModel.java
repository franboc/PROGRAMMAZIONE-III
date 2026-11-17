package it.prog3.mailserver.model;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Model del server. Per ora contiene solo il log degli eventi.
 *
 * Il log è una ObservableList: chi la "osserva" (la ListView della GUI)
 * viene avvisato automaticamente a ogni modifica. Il model NON conosce
 * la view: si limita a modificare i propri dati (pattern Observer).
 */
public class ServerModel {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ObservableList<String> log = FXCollections.observableArrayList();

    public ObservableList<String> getLog() {
        return log;
    }

    /**
     * Aggiunge un evento al log. Può essere chiamato da QUALSIASI thread.
     *
     * La lista è osservata dalla GUI, e la GUI di JavaFX si può modificare solo
     * dal suo thread (JavaFX Application Thread). Platform.runLater mette la
     * modifica in coda e la fa eseguire a quel thread appena possibile.
     */
    public void addLog(String message) {
        String line = "[" + LocalTime.now().format(TIME) + "] " + message;
        Platform.runLater(() -> log.add(line));
    }
}
