package it.prog3.mailserver.model;

import it.prog3.mailserver.persistence.MailboxFileStore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Gestisce l'insieme delle caselle del server.
 *
 * La mappa account -> casella viene costruita una volta all'avvio e poi non
 * cambia più (gli account sono fissi). Una mappa immutabile si può leggere da
 * più thread contemporaneamente senza lock: la mutua esclusione serve solo
 * DENTRO ogni casella (vedi Mailbox).
 */
public class MailboxManager {

    private final Map<String, Mailbox> mailboxes;

    public MailboxManager(Collection<String> accounts, MailboxFileStore store) throws IOException {
        Map<String, Mailbox> map = new HashMap<>();
        for (String account : accounts) {
            map.put(normalize(account), new Mailbox(store.loadOrCreate(account), store));
        }
        this.mailboxes = Map.copyOf(map); // copia immutabile
    }

    /** Gli indirizzi email non distinguono maiuscole e minuscole. */
    public static String normalize(String address) {
        return address == null ? "" : address.trim().toLowerCase(Locale.ROOT);
    }

    public boolean exists(String account) {
        return mailboxes.containsKey(normalize(account));
    }

    /** @return la casella, oppure null se l'account non esiste */
    public Mailbox getMailbox(String account) {
        return mailboxes.get(normalize(account));
    }

    public int count() {
        return mailboxes.size();
    }

    /** Restituisce gli indirizzi della lista che NON corrispondono a nessun account. */
    public List<String> findUnknown(Collection<String> addresses) {
        List<String> unknown = new ArrayList<>();
        for (String address : addresses) {
            if (!exists(address)) {
                unknown.add(address);
            }
        }
        return unknown;
    }

    /**
     * Consegna l'email a ogni destinatario (una sola copia anche se un indirizzo
     * è ripetuto). Ogni casella assegna alla propria copia il proprio ID.
     *
     * Le caselle vengono bloccate UNA ALLA VOLTA (mai due lock insieme):
     * così non può verificarsi un deadlock tra invii incrociati.
     *
     * @return i destinatari a cui la consegna è fallita (lista vuota = tutto ok)
     */
    public List<String> deliver(Email email) {
        Set<String> recipients = new LinkedHashSet<>();
        for (String r : email.getRecipients()) {
            recipients.add(normalize(r));
        }

        List<String> failed = new ArrayList<>();
        for (String recipient : recipients) {
            Mailbox mailbox = mailboxes.get(recipient);
            try {
                if (mailbox == null) {
                    failed.add(recipient);
                } else {
                    mailbox.add(email);
                }
            } catch (IOException e) {
                failed.add(recipient);
            }
        }
        return failed;
    }
}
