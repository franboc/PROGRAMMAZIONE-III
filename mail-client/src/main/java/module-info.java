/**
 * Descrittore del modulo del client (stessa logica del server).
 */
module it.prog3.mailclient {
    requires javafx.controls;
    requires javafx.fxml;

    opens it.prog3.mailclient.controller to javafx.fxml;

    exports it.prog3.mailclient;
}
