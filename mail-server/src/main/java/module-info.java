/**
 * Descrittore del modulo: dichiara di quali moduli abbiamo bisogno (requires)
 * e a chi rendiamo visibili i nostri package (exports / opens).
 */
module it.prog3.mailserver {
    requires javafx.controls;
    requires javafx.fxml;

    // FXMLLoader usa la reflection per iniettare i campi @FXML nel controller:
    // per questo il package dei controller deve essere "aperto" a javafx.fxml.
    opens it.prog3.mailserver.controller to javafx.fxml;

    // JavaFX deve poter istanziare la classe Application.
    exports it.prog3.mailserver;
}
