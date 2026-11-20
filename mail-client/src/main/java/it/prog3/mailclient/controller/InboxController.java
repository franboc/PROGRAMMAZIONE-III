package it.prog3.mailclient.controller;

import it.prog3.mailclient.model.ClientModel;
import it.prog3.mailclient.model.Draft;
import it.prog3.mailclient.model.Email;
import it.prog3.mailclient.network.InboxPoller;
import it.prog3.mailclient.network.ServerConnection;
import it.prog3.mailclient.protocol.Request;
import it.prog3.mailclient.protocol.Response;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Controller della finestra principale: lista delle email, dettaglio del messaggio
 * selezionato, notifica dei nuovi arrivi, stato della connessione e azioni
 * (nuovo, rispondi, rispondi a tutti, inoltra, elimina).
 *
 * I dati arrivano dal server tramite l'InboxPoller (thread in background);
 * qui li passiamo al thread della GUI con Platform.runLater e li mettiamo nel model.
 * La view si aggiorna da sola perché osserva il model (setItems, binding).
 */
public class InboxController {

    private static final Paint GREEN = Color.web("#2e7d32");
    private static final Paint RED = Color.web("#c62828");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    private Label userLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Circle statusDot;
    @FXML
    private Label notificationLabel;
    @FXML
    private Label unreadLabel;
    @FXML
    private ListView<Email> emailList;

    // Dettaglio dell'email selezionata
    @FXML
    private Label noSelectionLabel;
    @FXML
    private VBox detailBox;
    @FXML
    private Label subjectLabel;
    @FXML
    private Label fromLabel;
    @FXML
    private Label toLabel;
    @FXML
    private Label dateLabel;
    @FXML
    private TextArea bodyArea;

    // Barra delle azioni
    @FXML
    private Button replyButton;
    @FXML
    private Button replyAllButton;
    @FXML
    private Button forwardButton;
    @FXML
    private Button deleteButton;

    private ClientModel model;
    private ServerConnection connection;
    private Executor executor;
    private Consumer<Draft> openCompose;
    private InboxPoller poller;

    /** true mentre una cancellazione è in corso: evita di premere "Elimina" due volte. */
    private final BooleanProperty deleting = new SimpleBooleanProperty(false);

    /** Nasconde la notifica dopo qualche secondo. Una sola istanza: un nuovo arrivo la fa ripartire. */
    private final PauseTransition hideNotification = new PauseTransition(Duration.seconds(6));

    /**
     * @param executor    dove eseguire le operazioni di rete (thread in background)
     * @param openCompose apre la finestra di composizione con la bozza indicata (lo decide ClientApp)
     */
    public void init(ClientModel model, ServerConnection connection, Executor executor, Consumer<Draft> openCompose) {
        this.model = model;
        this.connection = connection;
        this.executor = executor;
        this.openCompose = openCompose;

        // ---------- azioni ----------
        // Rispondi, inoltra ed elimina hanno senso solo con un'email selezionata
        BooleanBinding noSelection = emailList.getSelectionModel().selectedItemProperty().isNull();
        replyButton.disableProperty().bind(noSelection);
        replyAllButton.disableProperty().bind(noSelection);
        forwardButton.disableProperty().bind(noSelection);
        deleteButton.disableProperty().bind(noSelection.or(deleting));

        // ---------- barra superiore e barra di stato ----------
        userLabel.textProperty().bind(model.userEmailProperty());

        // Bindings.when(...).then(...).otherwise(...) è un "if" osservabile:
        // si ricalcola da solo ogni volta che cambia connectedProperty
        statusLabel.textProperty().bind(
                Bindings.when(model.connectedProperty()).then("Connesso").otherwise("Non connesso - nuovo tentativo in corso..."));
        statusDot.fillProperty().bind(
                Bindings.when(model.connectedProperty()).then(GREEN).otherwise(RED));

        unreadLabel.textProperty().bind(
                Bindings.when(model.unreadCountProperty().greaterThan(0))
                        .then(model.unreadCountProperty().asString("%d non lette"))
                        .otherwise(""));

        notificationLabel.setVisible(false);
        hideNotification.setOnFinished(event -> notificationLabel.setVisible(false));

        // ---------- lista delle email ----------
        // setItems: la ListView OSSERVA la lista del model. Quando il model aggiunge
        // un'email, la lista a video si aggiorna da sola.
        emailList.setItems(model.getInbox());
        emailList.setCellFactory(list -> new EmailCell());

        // ---------- dettaglio ----------
        // Se non c'è nulla di selezionato si vede il messaggio "Seleziona un'email",
        // altrimenti il dettaglio. Anche questo è un binding: nessun if nel codice.
        detailBox.visibleProperty().bind(emailList.getSelectionModel().selectedItemProperty().isNotNull());
        noSelectionLabel.visibleProperty().bind(detailBox.visibleProperty().not());

        emailList.getSelectionModel().selectedItemProperty().addListener((observable, oldEmail, newEmail) -> {
            if (newEmail != null) {
                showDetail(newEmail);
                model.markAsRead(newEmail);
                emailList.refresh(); // la cella non è più in grassetto
            }
        });

        // ---------- polling ----------
        poller = new InboxPoller(connection, model.getUserEmail());
        poller.start(new InboxPoller.Listener() {
            // Questi metodi girano nel thread del poller: NON si può toccare la GUI
            // (né il model, che la GUI osserva). Si accoda il lavoro al thread di JavaFX.
            @Override
            public void onNewEmails(List<Email> emails, boolean firstFetch) {
                Platform.runLater(() -> receive(emails, firstFetch));
            }

            @Override
            public void onConnectionStatus(boolean connected) {
                Platform.runLater(() -> model.setConnected(connected));
            }
        });
    }

    /** Eseguito nel thread di JavaFX. */
    private void receive(List<Email> emails, boolean firstFetch) {
        // Al primo scaricamento arriva la casella già esistente: non sono "nuove" email.
        int added = model.addEmails(emails, !firstFetch);
        if (!firstFetch && added > 0) {
            notifyNewEmails(added);
        }
    }

    private void notifyNewEmails(int count) {
        showBanner(count == 1 ? "Hai 1 nuovo messaggio" : "Hai " + count + " nuovi messaggi");
    }

    /** Mostra un messaggio nella barra superiore per qualche secondo. */
    public void showBanner(String text) {
        notificationLabel.setText(text);
        notificationLabel.setVisible(true);
        hideNotification.playFromStart();
    }

    // ---------- azioni (bottoni della barra) ----------

    @FXML
    private void onNew() {
        openCompose.accept(Draft.empty());
    }

    @FXML
    private void onReply() {
        openCompose.accept(Draft.reply(selected()));
    }

    @FXML
    private void onReplyAll() {
        openCompose.accept(Draft.replyAll(selected(), model.getUserEmail()));
    }

    @FXML
    private void onForward() {
        openCompose.accept(Draft.forward(selected()));
    }

    @FXML
    private void onDelete() {
        Email email = selected();

        // Conferma: la cancellazione è definitiva (il server la toglie dal file)
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Eliminare definitivamente il messaggio \"" + subjectOf(email) + "\"?",
                ButtonType.OK, ButtonType.CANCEL);
        confirm.setHeaderText(null);
        confirm.setTitle("Elimina messaggio");
        confirm.initOwner(emailList.getScene().getWindow());
        Optional<ButtonType> answer = confirm.showAndWait();
        if (answer.isEmpty() || answer.get() != ButtonType.OK) {
            return;
        }

        deleting.set(true);
        Task<Response> deleteTask = new Task<>() {
            @Override
            protected Response call() throws Exception {
                return connection.send(Request.delete(model.getUserEmail(), email.getId()));
            }
        };

        deleteTask.setOnSucceeded(event -> {
            deleting.set(false);
            model.setConnected(true);
            Response response = deleteTask.getValue();
            // Si toglie dalla lista SOLO dopo la conferma del server (non prima):
            // se il server non risponde, l'email deve restare visibile.
            // "Messaggio non trovato" = era già stata cancellata (es. da un altro client
            // dello stesso utente): la togliamo comunque, così la lista è coerente col server.
            model.removeEmail(email);
            showBanner(response.isOk() ? "Messaggio eliminato" : response.getMessage());
        });

        deleteTask.setOnFailed(event -> {
            deleting.set(false);
            model.setConnected(false);
            showError("Impossibile contattare il server: il messaggio non è stato eliminato.");
        });

        executor.execute(deleteTask);
    }

    private Email selected() {
        return emailList.getSelectionModel().getSelectedItem();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setHeaderText(null);
        alert.initOwner(emailList.getScene().getWindow());
        alert.show(); // show(), non showAndWait(): non blocca il resto dell'interfaccia
    }

    private void showDetail(Email email) {
        subjectLabel.setText(subjectOf(email));
        fromLabel.setText(email.getSender());
        toLabel.setText(String.join(", ", email.getRecipients()));
        dateLabel.setText(format(email.getDate()));
        bodyArea.setText(email.getText() == null ? "" : email.getText());
    }

    /** Chiamato da ClientApp alla chiusura della finestra. */
    public void shutdown() {
        if (poller != null) {
            poller.stop();
        }
    }

    private static String subjectOf(Email email) {
        String subject = email.getSubject();
        return subject == null || subject.isBlank() ? "(nessun oggetto)" : subject;
    }

    private static String format(LocalDateTime date) {
        return date == null ? "" : date.format(DATE_FORMAT);
    }

    /**
     * Come viene disegnata una riga della lista: mittente e data sopra, oggetto sotto.
     *
     * Le celle vengono RIUSATE durante lo scroll (come sul server): i nodi si creano
     * una volta nel costruttore e updateItem li riempie ogni volta, ripulendo lo stile.
     */
    private class EmailCell extends ListCell<Email> {

        private final Label sender = new Label();
        private final Label date = new Label();
        private final Label subject = new Label();
        private final VBox content;

        EmailCell() {
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            HBox firstRow = new HBox(8, sender, spacer, date);
            date.getStyleClass().add("email-date");
            subject.getStyleClass().add("email-subject");
            content = new VBox(2, firstRow, subject);
        }

        @Override
        protected void updateItem(Email email, boolean empty) {
            super.updateItem(email, empty);
            getStyleClass().remove("unread");
            if (empty || email == null) {
                setGraphic(null);
                return;
            }
            sender.setText(email.getSender());
            date.setText(format(email.getDate()));
            subject.setText(subjectOf(email));
            if (model.isUnread(email)) {
                getStyleClass().add("unread");
            }
            setGraphic(content);
        }
    }
}
