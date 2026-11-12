package it.prog3.mailserver;

import it.prog3.mailserver.controller.ServerController;
import it.prog3.mailserver.model.MailboxManager;
import it.prog3.mailserver.model.ServerModel;
import it.prog3.mailserver.persistence.MailboxFileStore;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Punto di ingresso del server. Il suo unico compito è "montare" i pezzi MVC:
 * crea il Model, carica la View (FXML) e collega il Controller al Model.
 */
public class ServerApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // 1. VIEW: FXMLLoader legge il file FXML e crea anche il controller indicato in fx:controller
        FXMLLoader loader = new FXMLLoader(ServerApp.class.getResource("view/server-view.fxml"));
        Scene scene = new Scene(loader.load());

        // 2. MODEL: i dati dell'applicazione, indipendenti dalla grafica
        ServerModel model = new ServerModel();

        // 3. CONTROLLER: gli passiamo il model, così può collegarlo alla view
        ServerController controller = loader.getController();
        controller.init(model);

        model.addLog("Server avviato");

        // 4. Caricamento delle caselle dai file (verranno usate dal server di rete, Giorno 4)
        try {
            MailboxManager mailboxes = new MailboxManager(
                    ServerConfig.ACCOUNTS, new MailboxFileStore(ServerConfig.DATA_DIR));
            model.addLog("Caricate " + mailboxes.count() + " caselle da "
                    + ServerConfig.DATA_DIR.toAbsolutePath());
        } catch (IOException e) {
            model.addLog("ERRORE nel caricamento delle caselle: " + e.getMessage());
        }

        stage.setTitle("Mail Server");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args); // avvia il runtime JavaFX, che poi chiama start()
    }
}
