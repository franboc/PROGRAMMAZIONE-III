package it.prog3.mailserver.protocol;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Conversione oggetti Java <-> testo JSON.
 *
 * Gson produce JSON su UNA SOLA RIGA (gli "a capo" dentro il testo di
 * un'email diventano la sequenza \n). Per questo il protocollo può usare
 * una riga per la richiesta e una riga per la risposta.
 *
 * L'oggetto Gson è thread-safe: un'unica istanza può essere usata
 * contemporaneamente da tutti i thread del server.
 */
public final class JsonUtil {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter().nullSafe())
            .create();

    /** Versione "indentata" su più righe: usata per i file, così sono leggibili. */
    private static final Gson PRETTY_GSON = GSON.newBuilder()
            .setPrettyPrinting()
            .create();

    private JsonUtil() {
        // classe di sole utility: non si istanzia
    }

    public static String toJson(Object obj) {
        return GSON.toJson(obj);
    }

    /** Da NON usare sul socket: il protocollo richiede una riga sola. */
    public static String toPrettyJson(Object obj) {
        return PRETTY_GSON.toJson(obj);
    }

    public static <T> T fromJson(String json, Class<T> type) {
        return GSON.fromJson(json, type);
    }

    /**
     * Gson non sa convertire LocalDateTime da solo (con i moduli Java non può
     * accedere ai suoi campi privati): gli spieghiamo noi come farlo,
     * usando il formato standard ISO, es. "2026-11-11T10:30:00".
     */
    private static class LocalDateTimeAdapter extends TypeAdapter<LocalDateTime> {
        @Override
        public void write(JsonWriter out, LocalDateTime value) throws IOException {
            out.value(value.toString());
        }

        @Override
        public LocalDateTime read(JsonReader in) throws IOException {
            return LocalDateTime.parse(in.nextString());
        }
    }
}
