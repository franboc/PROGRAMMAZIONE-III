package it.prog3.mailserver.persistence;

import it.prog3.mailserver.model.Email;

import java.util.ArrayList;
import java.util.List;

/**
 * Il contenuto di un file di casella postale, così come viene salvato su disco:
 *
 * {
 *   "account": "bob@mail.it",
 *   "nextId": 4,
 *   "emails": [ {...}, {...}, {...} ]
 * }
 *
 * nextId è il prossimo ID da assegnare. Viene salvato su file perché deve
 * crescere SEMPRE, anche dopo cancellazioni e riavvii del server
 * (altrimenti un ID verrebbe riusato e il client perderebbe dei messaggi).
 */
public class MailboxData {

    private String account;
    private long nextId;
    private List<Email> emails;

    private MailboxData() {
    }

    public MailboxData(String account, long nextId, List<Email> emails) {
        this.account = account;
        this.nextId = nextId;
        this.emails = emails;
    }

    public String getAccount() {
        return account;
    }

    public long getNextId() {
        return nextId;
    }

    public List<Email> getEmails() {
        return emails == null ? new ArrayList<>() : emails;
    }
}
