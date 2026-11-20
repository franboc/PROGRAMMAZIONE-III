package it.prog3.mailclient;

import it.prog3.mailclient.controller.InboxController;
import it.prog3.mailclient.controller.LoginController;
import it.prog3.mailclient.model.ClientModel;
import it.prog3.mailclient.network.ServerConnection;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Punto di ingresso del client: crea il model e le risorse condivise
 * e gestisce il passaggio dalla schermata di login alla inbox.
 */
public class ClientApp extends Application {

    private Stage stage;
    private ClientModel model;
    private ServerConnection connection;
    private InboxController inboxController; // null finché l'utente non ha fatto login

    /**
     * Thread in background per le operazioni di rete. Sono thread "daemon":
     * non impediscono la chiusura del programma quando si chiude la finestra.
     */
    private final ExecutorService backgroundExecutor = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "client-network");
        thread.setDaemon(true);
        return thread;
    });

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        this.model = new ClientModel();
        this.connection = new ServerConnection(ClientConfig.SERVER_HOST, ClientConfig.SERVER_PORT);
        showLogin();
        stage.show();
    }

    private void showLogin() {
        FXMLLoader loader = load("view/login-view.fxml");
        LoginController controller = loader.getController();
        // Quando il login riesce, il controller chiama this::showInbox.
        // Il LoginController non sa cosa ci sia dopo: decide ClientApp.
        controller.init(model, connection, backgroundExecutor, this::showInbox);
        stage.setTitle("Mail Client - Accesso");
    }

    private void showInbox() {
        FXMLLoader loader = load("view/inbox-view.fxml");
        inboxController = loader.getController();
        // init avvia anche il polling: da qui il client chiede le email al server ogni pochi secondi
        inboxController.init(model, connection);
        stage.setTitle("Mail Client - " + model.getUserEmail());
        stage.centerOnScreen();
    }

    /** Carica un file FXML e lo mette nella finestra. */
    private FXMLLoader load(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(ClientApp.class.getResource(fxml));
            stage.setScene(new Scene(loader.load()));
            return loader;
        } catch (IOException e) {
            throw new UncheckedIOException("Impossibile caricare " + fxml, e);
        }
    }

    @Override
    public void stop() {
        if (inboxController != null) {
            inboxController.shutdown(); // ferma il polling
        }
        backgroundExecutor.shutdownNow();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
