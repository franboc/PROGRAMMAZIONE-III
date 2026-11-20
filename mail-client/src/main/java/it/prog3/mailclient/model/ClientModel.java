package it.prog3.mailclient.model;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Model del client: i dati che il client mantiene durante l'esecuzione.
 *
 *  - userEmail: l'indirizzo dell'utente, che lo identifica per tutta la sessione
 *  - connected: se l'ultima comunicazione con il server è andata a buon fine
 *  - inbox:     le email ricevute, dalla più recente alla più vecchia
 *  - unread:    gli ID delle email arrivate durante la sessione e non ancora aperte
 *
 * Property e lista sono esposte in SOLA LETTURA: la view può osservarle
 * (binding, setItems) ma non modificarle. Le modifiche passano dai metodi del model,
 * chiamati dai controller.
 *
 * Tutti i metodi vanno chiamati dal thread di JavaFX, perché la GUI osserva questi dati.
 */
public class ClientModel {

    private final ReadOnlyStringWrapper userEmail = new ReadOnlyStringWrapper("");
    private final ReadOnlyBooleanWrapper connected = new ReadOnlyBooleanWrapper(false);

    private final ObservableList<Email> inbox = FXCollections.observableArrayList();
    private final ObservableList<Email> inboxReadOnly = FXCollections.unmodifiableObservableList(inbox);

    /** ID delle email già in inbox: controllo dei duplicati in O(1) anche con caselle grandi. */
    private final Set<Long> knownIds = new HashSet<>();
    private final Set<Long> unread = new HashSet<>();
    private final ReadOnlyIntegerWrapper unreadCount = new ReadOnlyIntegerWrapper(0);

    // ---------- utente e connessione ----------

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

    // ---------- inbox ----------

    /** La inbox, dalla più recente alla più vecchia. Non modificabile dall'esterno. */
    public ObservableList<Email> getInbox() {
        return inboxReadOnly;
    }

    /**
     * Aggiunge in cima le email arrivate dal server.
     *
     * @param emails     in ordine di ID crescente, come le manda il server
     * @param markUnread true per le email arrivate durante la sessione (in grassetto e notificate);
     *                   false per la casella già presente al primo scaricamento
     * @return quante email sono state davvero aggiunte (i duplicati vengono scartati)
     */
    public int addEmails(List<Email> emails, boolean markUnread) {
        List<Email> added = new ArrayList<>();
        for (Email email : emails) {
            if (!knownIds.add(email.getId())) {
                continue; // difesa: la stessa email non compare mai due volte
            }
            added.add(email);
            if (markUnread) {
                unread.add(email.getId());
            }
        }
        // Il server le manda dalla più vecchia alla più nuova: le capovolgiamo
        // e le inseriamo in cima con UN solo addAll (una sola notifica alla ListView).
        Collections.reverse(added);
        inbox.addAll(0, added);
        unreadCount.set(unread.size());
        return added.size();
    }

    public boolean isUnread(Email email) {
        return unread.contains(email.getId());
    }

    /** Chiamato quando l'utente apre un'email. */
    public void markAsRead(Email email) {
        if (unread.remove(email.getId())) {
            unreadCount.set(unread.size());
        }
    }

    /** Numero di email non lette: la view lo mostra con un binding. */
    public ReadOnlyIntegerProperty unreadCountProperty() {
        return unreadCount.getReadOnlyProperty();
    }
}
