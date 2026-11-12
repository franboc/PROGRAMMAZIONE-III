package it.prog3.mailserver.model;

import it.prog3.mailserver.persistence.MailboxData;
import it.prog3.mailserver.persistence.MailboxFileStore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Casella di posta di un singolo account, tenuta in memoria e salvata su file
 * a ogni modifica.
 *
 * CONCORRENZA: ogni casella ha il PROPRIO lock di lettura/scrittura.
 *  - più thread possono LEGGERE contemporaneamente (read lock condiviso);
 *  - una SCRITTURA (aggiunta/cancellazione) è esclusiva (write lock):
 *    nessun altro legge o scrive quella casella nel frattempo.
 * Caselle diverse hanno lock diversi: un invio a bob non blocca chi legge
 * la casella di alice. È questo che permette al server di scalare.
 *
 * INVARIANTE: la lista emails è sempre ordinata per ID crescente
 * (gli ID vengono assegnati in ordine e aggiunti in coda).
 */
public class Mailbox {

    private final String account;
    private final List<Email> emails;
    private long nextId;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final MailboxFileStore store;

    public Mailbox(MailboxData data, MailboxFileStore store) {
        this.account = data.getAccount();
        this.nextId = data.getNextId();
        this.emails = new ArrayList<>(data.getEmails());
        this.emails.sort(Comparator.comparingLong(Email::getId)); // garantisce l'invariante
        this.store = store;
    }

    public String getAccount() {
        return account;
    }

    /**
     * Restituisce SOLO le email con ID > lastId (aggiornamento incrementale).
     * Restituisce una copia: chi la riceve può usarla senza tenere il lock.
     */
    public List<Email> getEmailsAfter(long lastId) {
        lock.readLock().lock();
        try {
            int from = firstIndexAfter(lastId);
            return new ArrayList<>(emails.subList(from, emails.size()));
        } finally {
            lock.readLock().unlock(); // nel finally: il lock viene rilasciato anche in caso di eccezione
        }
    }

    /**
     * Consegna un'email in questa casella assegnandole un nuovo ID.
     * @return l'email salvata, con l'ID definitivo
     */
    public Email add(Email email) throws IOException {
        lock.writeLock().lock();
        try {
        Email stored = email.withId(nextId);
        emails.add(stored);
        nextId++;
        try {
        save();
        } catch (IOException e) {
        // Salvataggio fallito: annullo la modifica in memoria per restare
        // coerente con quello che c'è su disco
        emails.remove(emails.size() - 1);
        nextId--;
        throw e;
        }
        return stored;
        } finally {
        lock.writeLock().unlock();
        }
    }

    /**
     * Cancella l'email con l'ID indicato.
     * @return false se l'email non esiste
     */
    public boolean delete(long id) throws IOException {
        lock.writeLock().lock();
        try {
            int index = indexOf(id);
            if (index < 0) {
                return false;
            }
            Email removed = emails.remove(index);
            try {
                save();
            } catch (IOException e) {
                emails.add(index, removed);
                throw e;
            }
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int size() {
        lock.readLock().lock();
        try {
            return emails.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    /** Da chiamare SOLO tenendo il write lock. */
    private void save() throws IOException {
        store.save(new MailboxData(account, nextId, emails));
    }

    /**
     * Ricerca binaria (la lista è ordinata per ID): O(log n) invece di O(n).
     * Restituisce l'indice della prima email con ID > lastId.
     */
    private int firstIndexAfter(long lastId) {
        int low = 0;
        int high = emails.size();
        while (low < high) {
            int mid = (low + high) >>> 1;
            if (emails.get(mid).getId() <= lastId) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return low;
    }

    /** Indice dell'email con l'ID dato, oppure -1 se non c'è. */
    private int indexOf(long id) {
        int index = firstIndexAfter(id - 1);
        if (index < emails.size() && emails.get(index).getId() == id) {
            return index;
        }
        return -1;
    }
}
