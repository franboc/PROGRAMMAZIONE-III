package it.prog3.mailclient.protocol;

import it.prog3.mailclient.model.Email;

import java.util.ArrayList;
import java.util.List;

/**
 * Risposta inviata dal server al client.
 *
 *   status  -> OK oppure ERROR
 *   message -> descrizione leggibile (es. "Destinatario inesistente: x@y.it")
 *   emails  -> usato solo dalla risposta a FETCH: le email nuove
 */
public class Response {

    public enum Status { OK, ERROR }

    private Status status;
    private String message;
    private List<Email> emails;

    private Response() {
    }

    private Response(Status status, String message, List<Email> emails) {
        this.status = status;
        this.message = message;
        this.emails = emails;
    }

    public static Response ok(String message) {
        return new Response(Status.OK, message, null);
    }

    public static Response ok(List<Email> emails) {
        return new Response(Status.OK, null, new ArrayList<>(emails));
    }

    public static Response error(String message) {
        return new Response(Status.ERROR, message, null);
    }

    public boolean isOk() {
        return status == Status.OK;
    }

    public Status getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public List<Email> getEmails() {
        return emails == null ? List.of() : emails;
    }
}
