package it.prog3.mailserver.protocol;

import it.prog3.mailserver.model.Email;

/**
 * Richiesta inviata dal client al server.
 *
 * Contiene sempre il comando e l'utente che lo esegue; gli altri campi
 * vengono usati solo da alcuni comandi:
 *   LOGIN  -> nessun campo aggiuntivo
 *   FETCH  -> lastId  (ultimo ID già ricevuto dal client)
 *   SEND   -> email   (il messaggio da spedire)
 *   DELETE -> emailId (il messaggio da cancellare)
 *
 * Gli oggetti si creano con i metodi statici (login, fetch, ...), così non
 * si può costruire per errore una richiesta incompleta.
 */
public class Request {

    private Command command;
    private String user;
    private long lastId;
    private long emailId;
    private Email email;

    private Request() {
    }

    private Request(Command command, String user) {
        this.command = command;
        this.user = user;
    }

    public static Request login(String user) {
        return new Request(Command.LOGIN, user);
    }

    public static Request fetch(String user, long lastId) {
        Request r = new Request(Command.FETCH, user);
        r.lastId = lastId;
        return r;
    }

    public static Request send(String user, Email email) {
        Request r = new Request(Command.SEND, user);
        r.email = email;
        return r;
    }

    public static Request delete(String user, long emailId) {
        Request r = new Request(Command.DELETE, user);
        r.emailId = emailId;
        return r;
    }

    public Command getCommand() {
        return command;
    }

    public String getUser() {
        return user;
    }

    public long getLastId() {
        return lastId;
    }

    public long getEmailId() {
        return emailId;
    }

    public Email getEmail() {
        return email;
    }
}
