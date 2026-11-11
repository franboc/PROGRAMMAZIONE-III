package it.prog3.mailserver.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Un messaggio di posta elettronica.
 *
 * La classe non ha setter: una volta creata, un'Email non cambia
 * (così può essere condivisa tra thread senza problemi).
 *
 * NB: questa classe esiste identica anche nel progetto client. I due progetti
 * non condividono codice: si scambiano solo testo JSON, quindi basta che i
 * nomi dei campi coincidano.
 */
public class Email {

    private long id;                 // assegnato dal server (0 = non ancora assegnato)
    private String sender;
    private List<String> recipients;
    private String subject;
    private String text;
    private LocalDateTime date;      // data di spedizione

    /** Costruttore vuoto usato solo da Gson quando ricostruisce l'oggetto dal JSON. */
    private Email() {
    }

    public Email(long id, String sender, List<String> recipients,
                 String subject, String text, LocalDateTime date) {
        this.id = id;
        this.sender = sender;
        this.recipients = new ArrayList<>(recipients); // copia difensiva
        this.subject = subject;
        this.text = text;
        this.date = date;
    }

    /** Restituisce una copia di questa email con l'ID indicato (usato dal server). */
    public Email withId(long newId) {
        return new Email(newId, sender, recipients, subject, text, date);
    }

    public long getId() {
        return id;
    }

    public String getSender() {
        return sender;
    }

    /** Lista non modificabile: chi la riceve non può alterare l'email. */
    public List<String> getRecipients() {
        return recipients == null ? List.of() : Collections.unmodifiableList(recipients);
    }

    public String getSubject() {
        return subject;
    }

    public String getText() {
        return text;
    }

    public LocalDateTime getDate() {
        return date;
    }

    @Override
    public String toString() {
        return "Email{id=" + id + ", da=" + sender + ", a=" + recipients
                + ", oggetto='" + subject + "', data=" + date + "}";
    }
}
