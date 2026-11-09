package it.prog3.mailclient.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Model del client. Per ora contiene solo lo stato della connessione.
 *
 * Una StringProperty è un valore "osservabile": chi è collegato tramite binding
 * vede ogni cambiamento in automatico. È l'equivalente moderno di
 * Observable/Observer (classi deprecate che la traccia vieta).
 */
public class ClientModel {

    private final StringProperty connectionStatus = new SimpleStringProperty("Non connesso");

    // Convenzione JavaFX: getter della property + getter/setter del valore
    public StringProperty connectionStatusProperty() {
        return connectionStatus;
    }

    public String getConnectionStatus() {
        return connectionStatus.get();
    }

    public void setConnectionStatus(String status) {
        connectionStatus.set(status);
    }
}
