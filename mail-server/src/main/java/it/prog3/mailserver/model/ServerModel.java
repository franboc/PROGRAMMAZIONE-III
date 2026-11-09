package it.prog3.mailserver.model;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Model del server. Per ora contiene solo il log degli eventi.
 *
 * Il log è una ObservableList: chi la "osserva" (la ListView della GUI)
 * viene avvisato automaticamente a ogni modifica. Il model NON conosce
 * la view: si limita a modificare i propri dati (pattern Observer).
 */
public class ServerModel {

    private final ObservableList<String> log = FXCollections.observableArrayList();

    public ObservableList<String> getLog() {
        return log;
    }

    public void addLog(String message) {
        log.add(message);
    }
}
