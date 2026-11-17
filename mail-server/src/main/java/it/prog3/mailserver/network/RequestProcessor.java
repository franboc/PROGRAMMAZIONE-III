package it.prog3.mailserver.network;

import it.prog3.mailserver.model.Email;
import it.prog3.mailserver.model.Mailbox;
import it.prog3.mailserver.model.MailboxManager;
import it.prog3.mailserver.protocol.Request;
import it.prog3.mailserver.protocol.Response;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

/**
 * La "logica" del server: riceve una Request già decodificata e produce la Response.
 *
 * Non sa nulla di socket né di JSON (se ne occupa ClientHandler) e nulla di
 * JavaFX: gli eventi da mostrare nel log li passa al "logger" ricevuto nel
 * costruttore. Così questa classe si può usare e provare anche senza GUI.
 *
 * È condivisa da tutti i thread del pool: non ha stato proprio da proteggere,
 * la mutua esclusione è gestita dentro le singole Mailbox.
 */
public class RequestProcessor {

    private final MailboxManager mailboxes;
    private final Consumer<String> logger;

    public RequestProcessor(MailboxManager mailboxes, Consumer<String> logger) {
        this.mailboxes = mailboxes;
        this.logger = logger;
    }

    public Response process(Request request) {
        if (request == null || request.getCommand() == null) {
            return Response.error("Richiesta non valida");
        }

        // Tutti i comandi sono eseguiti "a nome" di un utente: deve esistere
        String user = request.getUser();
        Mailbox mailbox = mailboxes.getMailbox(user);
        if (mailbox == null) {
            logger.accept(request.getCommand() + " rifiutato: account inesistente " + user);
            return Response.error("L'account " + user + " non esiste");
        }

        try {
            return switch (request.getCommand()) {
                case LOGIN -> login(mailbox);
                case FETCH -> fetch(mailbox, request.getLastId());
                case SEND -> send(mailbox, request.getEmail());
                case DELETE -> delete(mailbox, request.getEmailId());
            };
        } catch (IOException e) {
            logger.accept("ERRORE di scrittura su disco per " + mailbox.getAccount() + ": " + e.getMessage());
            return Response.error("Errore interno del server, riprova più tardi");
        }
    }

    private Response login(Mailbox mailbox) {
        logger.accept("Login di " + mailbox.getAccount());
        return Response.ok("Benvenuto " + mailbox.getAccount());
    }

    private Response fetch(Mailbox mailbox, long lastId) {
        List<Email> newEmails = mailbox.getEmailsAfter(lastId);
        logger.accept(mailbox.getAccount() + " chiede aggiornamenti: " + newEmails.size() + " nuove email");
        return Response.ok(newEmails);
    }

    private Response send(Mailbox senderBox, Email email) {
        String sender = senderBox.getAccount();
        if (email == null || email.getRecipients().isEmpty()) {
            return Response.error("Il messaggio deve avere almeno un destinatario");
        }

        // L'esistenza dei destinatari la verifica il server (requisito della traccia)
        List<String> unknown = mailboxes.findUnknown(email.getRecipients());
        if (!unknown.isEmpty()) {
            logger.accept("Invio da " + sender + " rifiutato: destinatari inesistenti " + unknown);
            return Response.error("Destinatari inesistenti: " + String.join(", ", unknown));
        }

        // Mittente e data li stabilisce il server: non ci si fida di quelli del client
        Email toDeliver = new Email(0, sender, email.getRecipients(),
                nullToEmpty(email.getSubject()), nullToEmpty(email.getText()),
                LocalDateTime.now().withNano(0));

        List<String> failed = mailboxes.deliver(toDeliver);
        if (!failed.isEmpty()) {
            logger.accept("ERRORE nella consegna dell'email di " + sender + " a " + failed);
            return Response.error("Consegna non riuscita a: " + String.join(", ", failed));
        }

        logger.accept(sender + " ha inviato \"" + toDeliver.getSubject() + "\" a " + toDeliver.getRecipients());
        return Response.ok("Messaggio inviato");
    }

    private Response delete(Mailbox mailbox, long emailId) throws IOException {
        if (!mailbox.delete(emailId)) {
            return Response.error("Messaggio non trovato");
        }
        logger.accept(mailbox.getAccount() + " ha cancellato l'email " + emailId);
        return Response.ok("Messaggio cancellato");
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
