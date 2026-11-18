package it.prog3.mailclient.controller;

import it.prog3.mailclient.model.ClientModel;
import it.prog3.mailclient.model.EmailValidator;
import it.prog3.mailclient.network.ServerConnection;
import it.prog3.mailclient.protocol.Request;
import it.prog3.mailclient.protocol.Response;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;

import java.util.Locale;
import java.util.concurrent.Executor;

/**
 * Controller della schermata di accesso.
 *
 * Due controlli, in quest'ordine:
 *  1. SINTASSI (qui, con la regex): se l'indirizzo è malformato, il server non viene nemmeno contattato;
 *  2. ESISTENZA (sul server, con LOGIN): solo il server sa quali account esistono.
 */
public class LoginController {

    @FXML
    private TextField emailField;
    @FXML
    private Button loginButton;
    @FXML
    private ProgressIndicator progressIndicator;
    @FXML
    private Label errorLabel;

    /** true mentre aspettiamo la risposta del server: blocca il form. */
    private final BooleanProperty busy = new SimpleBooleanProperty(false);

    private ClientModel model;
    private ServerConnection connection;
    private Executor executor;
    private Runnable onLoginSuccess;

    /**
     * @param executor       dove eseguire le operazioni di rete (thread in background)
     * @param onLoginSuccess cosa fare dopo un accesso riuscito (passare alla inbox)
     */
    public void init(ClientModel model, ServerConnection connection, Executor executor, Runnable onLoginSuccess) {
        this.model = model;
        this.connection = connection;
        this.executor = executor;
        this.onLoginSuccess = onLoginSuccess;

        // BINDING: il bottone è disattivato se il campo è vuoto o se stiamo già aspettando il server
        loginButton.disableProperty().bind(emailField.textProperty().isEmpty().or(busy));
        emailField.disableProperty().bind(busy);
        progressIndicator.visibleProperty().bind(busy);

        // Mentre l'utente scrive: cancella l'errore precedente e colora il campo se non è ben formato
        emailField.textProperty().addListener((observable, oldText, newText) -> {
            errorLabel.setText("");
            boolean malformed = !newText.isBlank() && !EmailValidator.isValid(newText);
            emailField.getStyleClass().remove("invalid");
            if (malformed) {
                emailField.getStyleClass().add("invalid");
            }
        });
    }

    /** Bottone "Accedi" (o tasto Invio, perché è il defaultButton). */
    @FXML
    private void onLogin() {
        String address = emailField.getText().trim().toLowerCase(Locale.ROOT);

        // 1. Controllo sintattico: se fallisce, chiediamo di reinserire senza contattare il server
        if (!EmailValidator.isValid(address)) {
            errorLabel.setText("\"" + address + "\" non è un indirizzo email valido. Esempio: nome@dominio.it");
            emailField.requestFocus();
            emailField.selectAll();
            return;
        }

        // 2. Controllo di esistenza: chiediamo al server, IN BACKGROUND
        busy.set(true);
        errorLabel.setText("");

        // Task = un lavoro da eseguire in un altro thread.
        // call() gira in background; i gestori setOnSucceeded/setOnFailed
        // vengono eseguiti da JavaFX nel thread della GUI, quindi lì possiamo toccare la view.
        Task<Response> loginTask = new Task<>() {
            @Override
            protected Response call() throws Exception {
                return connection.send(Request.login(address));
            }
        };

        loginTask.setOnSucceeded(event -> {
            busy.set(false);
            model.setConnected(true); // il server ha risposto: è raggiungibile
            Response response = loginTask.getValue();
            if (response.isOk()) {
                model.setUserEmail(address);
                onLoginSuccess.run();
            } else {
                // es. "L'account dave@mail.it non esiste"
                errorLabel.setText(response.getMessage());
                emailField.requestFocus();
                emailField.selectAll();
            }
        });

        loginTask.setOnFailed(event -> {
            busy.set(false);
            model.setConnected(false);
            errorLabel.setText("Impossibile contattare il server. Verifica che sia avviato e riprova.");
        });

        executor.execute(loginTask);
    }
}
