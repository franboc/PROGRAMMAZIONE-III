package it.prog3.mailclient.model;

import java.util.regex.Pattern;

/**
 * Verifica che un indirizzo email sia SINTATTICAMENTE corretto (ben formato).
 *
 * NON verifica che l'account esista: quello è compito del server.
 * Il client non sa quali utenti sono registrati.
 */
public final class EmailValidator {

    /**
     * Espressione regolare, pezzo per pezzo:
     *
     *   ^                        inizio della stringa
     *   [A-Za-z0-9._%+-]+        parte locale: lettere, cifre e . _ % + -  (almeno un carattere)
     *   @                        la chiocciola, esattamente una
     *   [A-Za-z0-9-]+            primo pezzo del dominio (es. "mail")
     *   (\.[A-Za-z0-9-]+)*       eventuali altri pezzi (es. ".mia" in "mia.mail.com")
     *   \.[A-Za-z]{2,}           estensione finale: punto + almeno 2 lettere (es. ".it", ".com")
     *   $                        fine della stringa
     *
     * Il Pattern è compilato una volta sola (è costoso) ed è immutabile,
     * quindi si può usare da più thread contemporaneamente.
     */
    private static final Pattern EMAIL = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$");

    private EmailValidator() {
    }

    public static boolean isValid(String address) {
        return address != null && EMAIL.matcher(address.trim()).matches();
    }
}
