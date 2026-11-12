package it.prog3.mailserver.persistence;

import it.prog3.mailserver.ServerConfig;
import it.prog3.mailserver.model.Email;
import it.prog3.mailserver.model.Mailbox;
import it.prog3.mailserver.model.MailboxManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Programma di prova del Giorno 3 (lo cancelleremo al Giorno 4).
 * Usa una cartella separata (data-demo) per non sporcare le caselle vere.
 */
public class PersistenceDemo {

    private static final Path DEMO_DIR = Path.of("data-demo");
    private static final int EMAILS_PER_THREAD = 100;

    public static void main(String[] args) throws Exception {
        deleteDirectory(DEMO_DIR); // si riparte sempre da zero
        MailboxManager manager = new MailboxManager(ServerConfig.ACCOUNTS, new MailboxFileStore(DEMO_DIR));

        // --- 1. Un invio semplice a due destinatari ---------------------------
        System.out.println("1) alice scrive a bob e carol");
        manager.deliver(email("alice@mail.it", List.of("bob@mail.it", "carol@mail.it"), "Ciao a tutti"));
        Mailbox bob = manager.getMailbox("bob@mail.it");
        System.out.println("   inbox di bob: " + bob.getEmailsAfter(0));

        // --- 2. Invii CONTEMPORANEI alla stessa casella ------------------------
        // 3 thread inviano 100 email ciascuno a bob, nello stesso momento.
        // Senza lock alcune email andrebbero perse o avrebbero ID duplicati.
        System.out.println("\n2) 3 thread inviano " + EMAILS_PER_THREAD + " email ciascuno a bob, in parallelo...");
        ExecutorService pool = Executors.newFixedThreadPool(3);
        for (String sender : List.of("alice@mail.it", "bob@mail.it", "carol@mail.it")) {
            pool.submit(() -> {
                for (int i = 1; i <= EMAILS_PER_THREAD; i++) {
                    manager.deliver(email(sender, List.of("bob@mail.it"), "Messaggio " + i + " da " + sender));
                }
            });
        }
        pool.shutdown();
        pool.awaitTermination(1, TimeUnit.MINUTES);

        List<Email> all = bob.getEmailsAfter(0);
        long distinctIds = all.stream().map(Email::getId).distinct().count();
        System.out.println("   email in inbox di bob: " + all.size() + " (attese " + (1 + 3 * EMAILS_PER_THREAD) + ")");
        System.out.println("   ID diversi tra loro:   " + distinctIds + " (devono essere uguali al numero di email)");

        // --- 3. Aggiornamento incrementale ------------------------------------
        System.out.println("\n3) Il client ha già ricevuto fino all'ID 298: chiede solo le nuove");
        bob.getEmailsAfter(298).forEach(e -> System.out.println("   " + e));

        // --- 4. Gli ID non vengono mai riusati --------------------------------
        System.out.println("\n4) Cancello l'ultima email (ID 301) e ne arriva una nuova");
        bob.delete(301);
        manager.deliver(email("carol@mail.it", List.of("bob@mail.it"), "Dopo la cancellazione"));
        List<Email> last = bob.getEmailsAfter(300);
        System.out.println("   nuova email: " + last.get(last.size() - 1) + "   <-- ID 302, non 301");

        // --- 5. Persistenza: rileggo tutto dai file ---------------------------
        System.out.println("\n5) Ricarico le caselle dai file, come dopo un riavvio del server");
        MailboxManager reloaded = new MailboxManager(ServerConfig.ACCOUNTS, new MailboxFileStore(DEMO_DIR));
        System.out.println("   bob dopo il riavvio: " + reloaded.getMailbox("bob@mail.it").size() + " email");

        // --- 6. Destinatari inesistenti ---------------------------------------
        System.out.println("\n6) Indirizzi sconosciuti: "
                + manager.findUnknown(List.of("bob@mail.it", "dave@mail.it", "eve@mail.it")));

        System.out.println("\nApri i file in: " + DEMO_DIR.toAbsolutePath());
    }

    private static Email email(String from, List<String> to, String subject) {
        return new Email(0, from, to, subject, "Testo di prova", LocalDateTime.now().withNano(0));
    }

    private static void deleteDirectory(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(dir)) {
            for (Path p : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(p);
            }
        }
    }
}
