package it.prog3.mailclient;

import it.prog3.mailclient.controller.ClientController;
import it.prog3.mailclient.model.ClientModel;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Punto di ingresso del client: monta Model, View e Controller.
 */
public class ClientApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(ClientApp.class.getResource("view/client-view.fxml"));
        Scene scene = new Scene(loader.load());

        ClientModel model = new ClientModel();

        ClientController controller = loader.getController();
        controller.init(model);

        stage.setTitle("Mail Client");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
