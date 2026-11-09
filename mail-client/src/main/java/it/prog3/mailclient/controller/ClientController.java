package it.prog3.mailclient.controller;

import it.prog3.mailclient.model.ClientModel;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * Controller della finestra principale del client.
 */
public class ClientController {

    @FXML
    private Label statusLabel;

    private ClientModel model;

    public void init(ClientModel model) {
        this.model = model;
        // BINDING: il testo della label segue la property del model.
        // Se il model cambia stato, la label si aggiorna da sola.
        statusLabel.textProperty().bind(model.connectionStatusProperty());
    }
}
