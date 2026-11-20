package it.prog3.mailclient.controller;

import it.prog3.mailclient.model.ClientModel;
import it.prog3.mailclient.model.Draft;
import it.prog3.mailclient.model.Email;
import it.prog3.mailclient.model.EmailValidator;
import it.prog3.mailclient.network.ServerConnection;
import it.prog3.mailclient.protocol.Request;
import it.prog3.mailclient.protocol.Response;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Controller della finestra di composizione (nuovo messaggio, risposta, inoltro).
 *
 * Come nel login, due controlli in quest'ordine:
 *  1. SINTASSI dei destinatari, qui con la regex: se uno è malformato non si contatta il server;
 *  2. ESISTENZA dei destinatari, sul server: se anche uno solo non esiste, il server
 *     rifiuta l'invio a TUTTI e risponde con l'elenco degli indirizzi sbagliati.
 */
public class ComposeController {

    @FXML
    private TextField toField;
    @FXML
    private TextField subjectField;
    @FXML
    private TextArea bodyArea;
    @FXML
    private Button sendButton;
    @FXML
    private Button cancelButton;
    @FXML
    private ProgressIndicator progressIndicator;
    @FXML
    private Label errorLabel;

    /** true mentre aspettiamo la risposta del server: blocca il form (niente doppi invii). */
    private final BooleanProperty busy = new SimpleBooleanProperty(false);

    private ClientModel model;
    private ServerConnection connection;
    private Executor executor;
    private Runnable onSent;

    /**
     * @param draft  il contenuto iniziale (vuoto, risposta, inoltro...)
     * @param onSent cosa fare dopo un invio riuscito (lo decide chi apre la finestra)
     */
    public void init(ClientModel model, ServerConnection connection, Executor executor, Draft draft, Runnable onSent) {
        this.model = model;
        this.connection = connection;
        this.executor = executor;
        this.onSent = onSent;

        toField.setText(draft.recipients());
        subjectField.setText(draft.subject());
        bodyArea.setText(draft.text());

        sendButton.disableProperty().bind(toField.textProperty().isEmpty().or(busy));
        cancelButton.disableProperty().bind(busy);
        toField.disableProperty().bind(busy);
        subjectField.disableProperty().bind(busy);
        bodyArea.disableProperty().bind(busy);
        progressIndicator.visibleProperty().bind(busy);

        // Mentre si scrive nel campo "A": cancella l'errore e colora il campo se c'è un indirizzo malformato
        toField.textProperty().addListener((observable, oldText, newText) -> {
            errorLabel.setText("");
            toField.getStyleClass().remove("invalid");
            if (!invalidAddresses(Draft.parseRecipients(newText)).isEmpty()) {
                toField.getStyleClass().add("invalid");
            }
        });

        // Cursore nel punto giusto: nel campo "A" se è vuoto (nuovo, inoltro), altrimenti all'inizio del testo.
        // runLater: il focus si può dare solo quando la finestra è già visibile.
        Platform.runLater(() -> {
            if (draft.recipients().isEmpty()) {
                toField.requestFocus();
            } else {
                bodyArea.requestFocus();
                bodyArea.positionCaret(0);
            }
        });
    }

    @FXML
    private void onSend() {
        List<String> recipients = Draft.parseRecipients(toField.getText());

        // 1. Controllo sintattico, nel client
        if (recipients.isEmpty()) {
            errorLabel.setText("Inserisci almeno un destinatario.");
            return;
        }
        List<String> invalid = invalidAddresses(recipients);
        if (!invalid.isEmpty()) {
            errorLabel.setText("Indirizzi non validi: " + String.join(", ", invalid));
            toField.requestFocus();
            return;
        }

        // 2. Invio al server, IN BACKGROUND.
        // Mittente e data li mettiamo per completezza, ma il server li sovrascrive:
        // non si fida di quello che dice il client.
        Email email = new Email(0, model.getUserEmail(), recipients,
                subjectField.getText().trim(), bodyArea.getText(), null);

        busy.set(true);
        errorLabel.setText("");

        Task<Response> sendTask = new Task<>() {
            @Override
            protected Response call() throws Exception {
                return connection.send(Request.send(model.getUserEmail(), email));
            }
        };

        sendTask.setOnSucceeded(event -> {
            busy.set(false);
            model.setConnected(true);
            Response response = sendTask.getValue();
            if (response.isOk()) {
                onSent.run();
                close();
            } else {
                // es. "Destinatari inesistenti: dave@mail.it". La finestra resta aperta
                // con tutto il testo: l'utente corregge e riprova.
                errorLabel.setText(response.getMessage());
            }
        });

        sendTask.setOnFailed(event -> {
            busy.set(false);
            model.setConnected(false);
            errorLabel.setText("Impossibile contattare il server: il messaggio NON è stato inviato. Riprova tra poco.");
        });

        executor.execute(sendTask);
    }

    @FXML
    private void onCancel() {
        close();
    }

    private void close() {
        ((Stage) sendButton.getScene().getWindow()).close();
    }

    private static List<String> invalidAddresses(List<String> recipients) {
        List<String> invalid = new ArrayList<>();
        for (String address : recipients) {
            if (!EmailValidator.isValid(address)) {
                invalid.add(address);
            }
        }
        return invalid;
    }
}
