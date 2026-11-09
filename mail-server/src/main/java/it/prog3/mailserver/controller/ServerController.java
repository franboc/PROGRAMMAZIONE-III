package it.prog3.mailserver.controller;

import it.prog3.mailserver.model.ServerModel;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;

/**
 * Controller della finestra del server: fa da ponte tra la view (FXML) e il model.
 */
public class ServerController {

    // Iniettato da FXMLLoader: il nome deve coincidere con fx:id nel file FXML
    @FXML
    private ListView<String> logListView;

    private ServerModel model;

    /** Chiamato da ServerApp dopo il caricamento dell'FXML. */
    public void init(ServerModel model) {
        this.model = model;
        // La ListView mostra direttamente la ObservableList del model:
        // ogni addLog() comparirà a schermo senza altro codice.
        logListView.setItems(model.getLog());
    }
}
