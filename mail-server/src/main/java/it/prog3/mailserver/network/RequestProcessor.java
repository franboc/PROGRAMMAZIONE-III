package it.prog3.mailserver.network;

import it.prog3.mailserver.model.Email;
import it.prog3.mailserver.model.EventLogger;
import it.prog3.mailserver.model.Mailbox;
import it.prog3.mailserver.model.MailboxManager;
import it.prog3.mailserver.protocol.Request;
import it.prog3.mailserver.protocol.Response;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static it.prog3.mailserver.model.LogEntry.Category.ERROR;
import static it.prog3.mailserver.model.LogEntry.Category.OPERATION;

/**
 * La "logica" del server: riceve una Request già decodificata e produce la Response.
 *
 * Non sa nulla di socket né di JSON (se ne occupa ClientHandler) e nulla di
 * JavaFX: gli eventi li segnala all'EventLogger ricevuto. Così questa classe
 * si può usare e provare anche senza GUI.
 *
 * È condivisa da tutti i thread del pool: non ha stato proprio da proteggere,
 * la mutua esclusione è gestita dentro le singole Mailbox.
 */
public class RequestProcessor {

    private final MailboxManager mailboxes;

    public RequestProcessor(MailboxManager mailboxes) {
        this.mailboxes = mailboxes;
    }

    /**
     * @param log dove segnalare gli eventi di QUESTA richiesta
     */
    public Response process(Request request, EventLogger log) {
        if (request == null || request.getCommand() == null) {
            log.log(ERROR, "Richiesta senza comando");
            return Response.error("Richiesta non valida");
        }

        // Tutti i comandi sono eseguiti "a nome" di un utente: deve esistere
        String user = request.getUser();
        Mailbox mailbox = mailboxes.getMailbox(user);
        if (mailbox == null) {
            log.log(ERROR, request.getCommand() + " rifiutato: account inesistente " + user);
            return Response.error("L'account " + user + " non esiste");
        }

        try {
            return switch (request.getCommand()) {
                case LOGIN -> login(mailbox, log);
                case FETCH -> fetch(mailbox, request.getLastId(), log);
                case SEND -> send(mailbox, request.getEmail(), log);
                case DELETE -> delete(mailbox, request.getEmailId(), log);
            };
        } catch (IOException e) {
            log.log(ERROR, "Errore di scrittura su disco per " + mailbox.getAccount() + ": " + e.getMessage());
            return Response.error("Errore interno del server, riprova più tardi");
        }
    }

    private Response login(Mailbox mailbox, EventLogger log) {
        log.log(OPERATION, "Login di " + mailbox.getAccount());
        return Response.ok("Benvenuto " + mailbox.getAccount());
    }

    private Response fetch(Mailbox mailbox, long lastId, EventLogger log) {
        List<Email> newEmails = mailbox.getEmailsAfter(lastId);
        if (newEmails.isEmpty()) {
            log.log(OPERATION, mailbox.getAccount() + " chiede aggiornamenti: nessuna novità");
        } else {
            log.log(OPERATION, mailbox.getAccount() + " ha scaricato " + newEmails.size() + " nuove email");
        }
        return Response.ok(newEmails);
    }

    private Response send(Mailbox senderBox, Email email, EventLogger log) {
        String sender = senderBox.getAccount();
        if (email == null || email.getRecipients().isEmpty()) {
            log.log(ERROR, "Invio da " + sender + " rifiutato: nessun destinatario");
            return Response.error("Il messaggio deve avere almeno un destinatario");
        }

        // L'esistenza dei destinatari la verifica il server (requisito della traccia)
        List<String> unknown = mailboxes.findUnknown(email.getRecipients());
        if (!unknown.isEmpty()) {
            log.log(ERROR, "Invio da " + sender + " rifiutato: destinatari inesistenti " + unknown);
            return Response.error("Destinatari inesistenti: " + String.join(", ", unknown));
        }

        // Mittente e data li stabilisce il server: non ci si fida di quelli del client
        Email toDeliver = new Email(0, sender, email.getRecipients(),
                nullToEmpty(email.getSubject()), nullToEmpty(email.getText()),
                LocalDateTime.now().withNano(0));

        List<String> failed = mailboxes.deliver(toDeliver);
        if (!failed.isEmpty()) {
            log.log(ERROR, "Consegna dell'email di " + sender + " fallita per " + failed);
            return Response.error("Consegna non riuscita a: " + String.join(", ", failed));
        }

        log.log(OPERATION, sender + " ha inviato \"" + toDeliver.getSubject() + "\" a " + toDeliver.getRecipients());
        return Response.ok("Messaggio inviato");
    }

    private Response delete(Mailbox mailbox, long emailId, EventLogger log) throws IOException {
        if (!mailbox.delete(emailId)) {
            log.log(ERROR, mailbox.getAccount() + " ha chiesto di cancellare l'email " + emailId + ", che non esiste");
            return Response.error("Messaggio non trovato");
        }
        log.log(OPERATION, mailbox.getAccount() + " ha cancellato l'email " + emailId);
        return Response.ok("Messaggio cancellato");
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
