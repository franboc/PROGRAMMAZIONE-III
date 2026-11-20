package it.prog3.mailclient.model;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Una bozza: il contenuto con cui si apre la finestra di composizione.
 *
 * Qui stanno le REGOLE di reply, reply-all e forward. Sono logica del model,
 * non della view: non dipendono da JavaFX e si possono verificare senza finestre.
 *
 * È un record: immutabile, con costruttore, getter, equals e toString generati da Java.
 *
 * @param recipients destinatari, separati da virgola (come li vede l'utente nel campo "A")
 */
public record Draft(String recipients, String subject, String text) {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** Nuovo messaggio: tutto vuoto. */
    public static Draft empty() {
        return new Draft("", "", "");
    }

    /** Rispondi: solo al mittente del messaggio originale. */
    public static Draft reply(Email original) {
        return new Draft(original.getSender(), prefixed("Re: ", original.getSubject()), quoted(original));
    }

    /**
     * Rispondi a tutti: al mittente e a tutti gli altri destinatari, TRANNE me stesso
     * (non ha senso mandarmi una copia della mia risposta).
     *
     * LinkedHashSet: elimina i doppioni e mantiene l'ordine (mittente per primo).
     */
    public static Draft replyAll(Email original, String me) {
        Set<String> recipients = new LinkedHashSet<>();
        recipients.add(original.getSender());
        recipients.addAll(original.getRecipients());
        recipients.removeIf(address -> address.equalsIgnoreCase(me));
        if (recipients.isEmpty()) {
            // Caso limite: l'email l'avevo mandata solo a me stesso
            recipients.add(original.getSender());
        }
        return new Draft(String.join(", ", recipients), prefixed("Re: ", original.getSubject()), quoted(original));
    }

    /** Inoltra: destinatari da scegliere, il messaggio originale riportato per intero. */
    public static Draft forward(Email original) {
        String text = "\n\n---------- Messaggio inoltrato ----------\n"
                + "Da: " + original.getSender() + "\n"
                + "A: " + String.join(", ", original.getRecipients()) + "\n"
                + "Data: " + formatDate(original) + "\n"
                + "Oggetto: " + nullToEmpty(original.getSubject()) + "\n\n"
                + nullToEmpty(original.getText());
        return new Draft("", prefixed("Fwd: ", original.getSubject()), text);
    }

    /**
     * Divide il campo "A" in una lista di indirizzi.
     * Accetta virgola, punto e virgola e spazi come separatori; toglie i doppioni.
     */
    public static List<String> parseRecipients(String field) {
        Set<String> result = new LinkedHashSet<>();
        if (field != null) {
            for (String piece : field.split("[,;\\s]+")) {
                if (!piece.isBlank()) {
                    result.add(piece.trim().toLowerCase(Locale.ROOT));
                }
            }
        }
        return new ArrayList<>(result);
    }

    /** Aggiunge "Re: " o "Fwd: " solo se non c'è già (evita "Re: Re: Re: ..."). */
    private static String prefixed(String prefix, String subject) {
        String s = nullToEmpty(subject);
        if (s.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT))) {
            return s;
        }
        return prefix + s;
    }

    /** Il testo originale citato, ogni riga preceduta da "> ". */
    private static String quoted(Email original) {
        StringBuilder sb = new StringBuilder("\n\nIl " + formatDate(original) + " " + original.getSender() + " ha scritto:\n");
        for (String line : nullToEmpty(original.getText()).split("\n", -1)) {
            sb.append("> ").append(line).append('\n');
        }
        return sb.toString();
    }

    private static String formatDate(Email email) {
        return email.getDate() == null ? "" : email.getDate().format(DATE_FORMAT);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
