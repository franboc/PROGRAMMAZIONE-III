package it.prog3.mailserver.controller;

import it.prog3.mailserver.model.LogEntry;
import it.prog3.mailserver.model.ServerModel;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;

import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * Controller della finestra del server: fa da ponte tra la view (FXML) e il model.
 *
 * Collega una volta sola le parti osservabili del model ai controlli grafici;
 * da lì in poi gli aggiornamenti avvengono da soli (pattern Observer).
 */
public class ServerController {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    // Iniettati da FXMLLoader: i nomi coincidono con gli fx:id nel file FXML
    @FXML
    private ListView<LogEntry> logListView;
    @FXML
    private CheckBox showPollingCheckBox;
    @FXML
    private Label statusLabel;
    @FXML
    private Label countLabel;

    private ServerModel model;

    /** Chiamato da ServerApp dopo il caricamento dell'FXML. */
    public void init(ServerModel model) {
        this.model = model;

        // Stato del server: la label segue la property del model
        statusLabel.textProperty().bind(model.statusProperty());

        // FilteredList = una "vista filtrata" della lista del model. Non è una copia:
        // quando il model aggiunge un evento, compare qui solo se supera il filtro.
        FilteredList<LogEntry> visibleEntries = new FilteredList<>(model.getLog());

        // Il filtro dipende dalla checkbox: quando l'utente la spunta,
        // il binding ricalcola il predicato e la lista si aggiorna da sola.
        visibleEntries.predicateProperty().bind(Bindings.createObjectBinding(
                this::currentFilter, showPollingCheckBox.selectedProperty()));

        logListView.setItems(visibleEntries);
        logListView.setCellFactory(listView -> new LogCell());

        countLabel.textProperty().bind(Bindings.size(visibleEntries).asString("%d eventi"));

        // Scorre automaticamente all'ultimo evento quando ne arriva uno nuovo
        visibleEntries.addListener((ListChangeListener<LogEntry>) change -> {
            if (!visibleEntries.isEmpty()) {
                logListView.scrollTo(visibleEntries.size() - 1);
            }
        });
    }

    private Predicate<LogEntry> currentFilter() {
        if (showPollingCheckBox.isSelected()) {
            return entry -> true;
        }
        return entry -> entry.category() != LogEntry.Category.POLLING;
    }

    /** Collegato al bottone "Pulisci log" (onAction="#onClearLog" nell'FXML). */
    @FXML
    private void onClearLog() {
        model.clearLog();
    }

    /**
     * Come disegnare UNA riga della lista. JavaFX crea poche celle e le riusa
     * mentre si scorre: per questo updateItem deve sempre ripulire lo stile precedente.
     */
    private static class LogCell extends ListCell<LogEntry> {
        @Override
        protected void updateItem(LogEntry entry, boolean empty) {
            super.updateItem(entry, empty);
            getStyleClass().removeIf(styleClass -> styleClass.startsWith("log-"));
            if (empty || entry == null) {
                setText(null);
            } else {
                setText("[" + entry.time().format(TIME) + "] " + entry.message());
                // es. Category.ERROR -> classe CSS "log-error" (vedi server.css)
                getStyleClass().add("log-" + entry.category().name().toLowerCase(Locale.ROOT));
            }
        }
    }
}
