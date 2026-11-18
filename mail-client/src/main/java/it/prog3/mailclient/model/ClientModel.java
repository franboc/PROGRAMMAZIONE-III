package it.prog3.mailclient.model;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;

/**
 * Model del client: i dati che il client mantiene durante l'esecuzione.
 *
 *  - userEmail: l'indirizzo dell'utente, che lo identifica per tutta la sessione
 *  - connected: se l'ultima comunicazione con il server è andata a buon fine
 *  (dal Giorno 7: anche la inbox)
 *
 * Le property sono esposte in SOLA LETTURA: la view può osservarle (binding)
 * ma non modificarle. Le modifiche passano dai metodi set..., chiamati dai controller.
 *
 * I setter vanno chiamati dal thread di JavaFX, perché la GUI osserva queste property.
 */
public class ClientModel {

    private final ReadOnlyStringWrapper userEmail = new ReadOnlyStringWrapper("");
    private final ReadOnlyBooleanWrapper connected = new ReadOnlyBooleanWrapper(false);

    public ReadOnlyStringProperty userEmailProperty() {
        return userEmail.getReadOnlyProperty();
    }

    public String getUserEmail() {
        return userEmail.get();
    }

    public void setUserEmail(String email) {
        userEmail.set(email);
    }

    public ReadOnlyBooleanProperty connectedProperty() {
        return connected.getReadOnlyProperty();
    }

    public boolean isConnected() {
        return connected.get();
    }

    public void setConnected(boolean value) {
        connected.set(value);
    }
}
