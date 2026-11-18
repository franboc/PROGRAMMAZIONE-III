package it.prog3.mailclient.controller;

import it.prog3.mailclient.model.ClientModel;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;

/**
 * Controller della finestra principale (la inbox, che completeremo al Giorno 7).
 */
public class InboxController {

    private static final Paint GREEN = Color.web("#2e7d32");
    private static final Paint RED = Color.web("#c62828");

    @FXML
    private Label userLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Circle statusDot;

    private ClientModel model;

    public void init(ClientModel model) {
        this.model = model;

        userLabel.textProperty().bind(model.userEmailProperty());

        // Bindings.when(...).then(...).otherwise(...) è un "if" osservabile:
        // si ricalcola da solo ogni volta che cambia connectedProperty
        statusLabel.textProperty().bind(
                Bindings.when(model.connectedProperty()).then("Connesso").otherwise("Non connesso"));
        statusDot.fillProperty().bind(
                Bindings.when(model.connectedProperty()).then(GREEN).otherwise(RED));
    }
}
