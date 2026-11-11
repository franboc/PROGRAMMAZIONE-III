/**
 * Descrittore del modulo del client (stessa logica del server).
 */
module it.prog3.mailclient {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson;

    opens it.prog3.mailclient.controller to javafx.fxml;

    // Gson usa la reflection sui campi privati di Email, Request, Response
    opens it.prog3.mailclient.model to com.google.gson;
    opens it.prog3.mailclient.protocol to com.google.gson;

    exports it.prog3.mailclient;
}
