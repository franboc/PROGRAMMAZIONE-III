package it.prog3.mailserver.protocol;

import it.prog3.mailserver.model.Email;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Programma di prova del Giorno 2 (lo cancelleremo al Giorno 4).
 * Mostra come richieste e risposte diventano testo JSON e tornano oggetti.
 */
public class ProtocolDemo {

    public static void main(String[] args) {
        Email email = new Email(0, "alice@mail.it", List.of("bob@mail.it", "carol@mail.it"),
                "Ciao", "Prima riga\nSeconda riga", LocalDateTime.now().withNano(0));

        // Oggetto -> testo (quello che viaggerà sul socket)
        String jsonRequest = JsonUtil.toJson(Request.fetch("alice@mail.it", 7));
        System.out.println("Richiesta SEND come testo:\n" + jsonRequest + "\n");

        // Testo -> oggetto (quello che farà il server quando la riceve)
        Request received = JsonUtil.fromJson(jsonRequest, Request.class);
        System.out.println("Ricostruita: comando=" + received.getCommand()
                + ", email=" + received.getEmail() + "\n");

        System.out.println("Risposta FETCH:\n" + JsonUtil.toJson(Response.ok(List.of(email.withId(1)))) + "\n");
        System.out.println("Risposta di errore:\n" + JsonUtil.toJson(Response.error("Destinatario inesistente: dave@mail.it")));
    }
}
