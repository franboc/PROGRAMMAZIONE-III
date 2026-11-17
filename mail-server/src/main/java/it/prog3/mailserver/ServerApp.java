package it.prog3.mailserver;

import it.prog3.mailserver.controller.ServerController;
import it.prog3.mailserver.model.LogEntry.Category;
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
        //    Alla rete passiamo il model come EventLogger: la rete segnala gli
        //    eventi senza sapere che esiste una GUI.
        try {
            MailboxManager mailboxes = new MailboxManager(
                    ServerConfig.ACCOUNTS, new MailboxFileStore(ServerConfig.DATA_DIR));
            model.log(Category.INFO, "Caricate " + mailboxes.count() + " caselle da "
                    + ServerConfig.DATA_DIR.toAbsolutePath());

            mailServer = new MailServer(ServerConfig.PORT, new RequestProcessor(mailboxes), model);
            mailServer.start();
            model.setStatus("in ascolto sulla porta " + ServerConfig.PORT);
        } catch (BindException e) {
            model.log(Category.ERROR, "La porta " + ServerConfig.PORT
                    + " è già in uso (il server è già aperto in un'altra finestra?)");
            model.setStatus("NON ATTIVO (porta occupata)");
        } catch (IOException e) {
            model.log(Category.ERROR, "Errore all'avvio del server: " + e.getMessage());
            model.setStatus("NON ATTIVO (errore all'avvio)");
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
