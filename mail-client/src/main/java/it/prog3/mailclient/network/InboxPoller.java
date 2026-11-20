package it.prog3.mailclient.network;

import it.prog3.mailclient.ClientConfig;
import it.prog3.mailclient.model.Email;
import it.prog3.mailclient.protocol.Request;
import it.prog3.mailclient.protocol.Response;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Chiede periodicamente al server le email nuove (polling con FETCH incrementale).
 *
 * Ogni POLL_INTERVAL_SECONDS secondi manda FETCH con lastId = ID più alto già ricevuto:
 * il server risponde solo con le email più recenti, mai con l'intera casella.
 *
 * THREAD CONFINEMENT: lastId e firstFetch sono letti e scritti solo dal thread
 * dello scheduler (che è uno solo), quindi non servono lock.
 *
 * Come la rete del server, questa classe NON conosce JavaFX: consegna i risultati
 * a un Listener. È chi crea il listener (InboxController) a passare al thread
 * della GUI con Platform.runLater.
 */
public class InboxPoller {

    /** Chi riceve i risultati del polling. I metodi vengono chiamati dal thread del poller. */
    public interface Listener {
        /**
         * @param emails     le email nuove, in ordine di ID crescente (come le manda il server)
         * @param firstFetch true per il primo scaricamento: è la casella già esistente, niente notifica
         */
        void onNewEmails(List<Email> emails, boolean firstFetch);

        /** true se il server ha risposto, false se non è raggiungibile. */
        void onConnectionStatus(boolean connected);
    }

    private final ServerConnection connection;
    private final String user;

    /** Un solo thread, daemon: non impedisce la chiusura del programma. */
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "inbox-poller");
        thread.setDaemon(true);
        return thread;
    });

    private Listener listener;
    private long lastId = 0;          // ID più alto ricevuto finora (0 = nessuno)
    private boolean firstFetch = true;

    public InboxPoller(ServerConnection connection, String user) {
        this.connection = connection;
        this.user = user;
    }

    /** Avvia il polling: il primo FETCH parte subito, i successivi ogni POLL_INTERVAL_SECONDS. */
    public void start(Listener listener) {
        this.listener = listener;
        // scheduleWithFixedDelay (non AtFixedRate): il giro successivo parte N secondi
        // DOPO la fine del precedente, quindi due FETCH non si sovrappongono mai,
        // nemmeno se il server è lento o la connessione va in timeout.
        scheduler.scheduleWithFixedDelay(this::poll, 0, ClientConfig.POLL_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    /** Ferma il polling (alla chiusura della finestra). */
    public void stop() {
        scheduler.shutdownNow();
    }

    private void poll() {
        // Tutto dentro try/catch: se un'eccezione uscisse da qui, lo scheduler
        // annullerebbe in silenzio tutte le esecuzioni future e il polling si fermerebbe.
        try {
            Response response = connection.send(Request.fetch(user, lastId));
            listener.onConnectionStatus(true);
            if (!response.isOk()) {
                return; // es. account inesistente: niente da aggiungere
            }

            List<Email> emails = response.getEmails();
            for (Email email : emails) {
                lastId = Math.max(lastId, email.getId());
            }
            if (!emails.isEmpty() || firstFetch) {
                listener.onNewEmails(emails, firstFetch);
            }
            firstFetch = false;

        } catch (IOException | RuntimeException e) {
            // Server spento o risposta illeggibile: lo segnaliamo e riproviamo al giro dopo.
            // Quando il server torna, il FETCH successivo riesce e il pallino torna verde.
            listener.onConnectionStatus(false);
        }
    }
}
