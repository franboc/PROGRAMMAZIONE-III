package it.prog3.mailserver;

import it.prog3.mailserver.controller.ServerController;
import it.prog3.mailserver.model.MailboxManager;
import it.prog3.mailserver.model.ServerModel;
import it.prog3.mailserver.network.MailServer;
import it.prog3.mailserver.network.RequestProcessor;
import it.prog3.mailserver.persistence.MailboxFileStore;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.BindException;

/**
 * Punto di ingresso del server. Il suo unico compito è "montare" i pezzi:
 * crea il Model, carica la View (FXML), collega il Controller e avvia la rete.
 */
public class ServerApp extends Application {

    private MailServer mailServer;

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

        stage.setTitle("Mail Server");
        stage.setScene(scene);
        stage.show();

        // 4. Caricamento delle caselle e avvio del server di rete.
        //    Il logger passato alla rete è model::addLog: la rete scrive nel log
        //    senza sapere che esiste una GUI.
        try {
            MailboxManager mailboxes = new MailboxManager(
                    ServerConfig.ACCOUNTS, new MailboxFileStore(ServerConfig.DATA_DIR));
            model.addLog("Caricate " + mailboxes.count() + " caselle da "
                    + ServerConfig.DATA_DIR.toAbsolutePath());

            RequestProcessor processor = new RequestProcessor(mailboxes, model::addLog);
            mailServer = new MailServer(ServerConfig.PORT, processor, model::addLog);
            mailServer.start();
        } catch (BindException e) {
            model.addLog("ERRORE: la porta " + ServerConfig.PORT
                    + " è già in uso (il server è già aperto in un'altra finestra?)");
        } catch (IOException e) {
            model.addLog("ERRORE all'avvio del server: " + e.getMessage());
        }
    }

    /** Chiamato da JavaFX quando si chiude la finestra: fermiamo i thread di rete. */
    @Override
    public void stop() {
        if (mailServer != null) {
            mailServer.stop();
        }
    }

    public static void main(String[] args) {
        launch(args); // avvia il runtime JavaFX, che poi chiama start()
    }
}
