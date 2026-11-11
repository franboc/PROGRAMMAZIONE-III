/**
 * Descrittore del modulo: dichiara di quali moduli abbiamo bisogno (requires)
 * e a chi rendiamo visibili i nostri package (exports / opens).
 */
module it.prog3.mailserver {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson;

    // FXMLLoader usa la reflection per iniettare i campi @FXML nel controller:
    // per questo il package dei controller deve essere "aperto" a javafx.fxml.
    opens it.prog3.mailserver.controller to javafx.fxml;

    // Anche Gson usa la reflection per leggere/scrivere i campi privati
    // delle classi che converte in JSON.
    opens it.prog3.mailserver.model to com.google.gson;
    opens it.prog3.mailserver.protocol to com.google.gson;

    // JavaFX deve poter istanziare la classe Application.
    exports it.prog3.mailserver;
}
