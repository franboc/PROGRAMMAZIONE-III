package it.prog3.mailserver.persistence;

import it.prog3.mailserver.protocol.JsonUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;

/**
 * Lettura e scrittura delle caselle su file JSON: un file per account
 * (es. data/bob@mail.it.json).
 *
 * Questa classe NON gestisce la concorrenza: è responsabilità di chi la usa
 * (la classe Mailbox) chiamare save() tenendo il lock della casella.
 * Caselle diverse usano file diversi, quindi possono essere salvate in parallelo.
 */
public class MailboxFileStore {

    private final Path directory;

    public MailboxFileStore(Path directory) throws IOException {
        this.directory = directory;
        Files.createDirectories(directory); // crea la cartella se non esiste
    }

    /** Legge la casella dal file; se il file non esiste ancora, crea una casella vuota. */
    public MailboxData loadOrCreate(String account) throws IOException {
        Path file = fileFor(account);
        if (!Files.exists(file)) {
            MailboxData empty = new MailboxData(account, 1, new ArrayList<>());
            save(empty);
            return empty;
        }
        String json = Files.readString(file, StandardCharsets.UTF_8);
        return JsonUtil.fromJson(json, MailboxData.class);
    }

    /**
     * Salva la casella con una SCRITTURA ATOMICA:
     *   1. scrive tutto su un file temporaneo (bob@mail.it.json.tmp)
     *   2. lo rinomina sopra il file vero con un'unica operazione
     * Se il server si spegne durante il passo 1, il file originale è intatto:
     * non resta mai un file scritto a metà.
     */
    public void save(MailboxData data) throws IOException {
        Path file = fileFor(data.getAccount());
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");

        Files.writeString(tmp, JsonUtil.toPrettyJson(data), StandardCharsets.UTF_8);
        try {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            // Alcuni file system non supportano lo spostamento atomico
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private Path fileFor(String account) {
        return directory.resolve(account + ".json");
    }
}
