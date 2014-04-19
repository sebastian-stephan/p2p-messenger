package org.challengetask.gui;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import org.challengetask.MainApp;

public class FXMLMainController implements Initializable {

    private MainApp mainApp;

    @FXML
    private Label messageLabel;
    @FXML
    private ListView friendsList;

    public void setApplication(MainApp _mainApp) {
        mainApp = _mainApp;
    }

    public void setMessage(String message) {
        messageLabel.setText(message);
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Fill with some fake friends
        List<String> friends = Arrays.asList("Audrey Whetzel", "Rodrick Falzone", "Dorthea Camacho", "Tianna Pennell", "Essie Colclough", "Sharleen Berrey", "Regan Neisler", "Rosa Furtado", "Jacob Rhymer", "Maximo Blick", "Lorette Dumas", "Donovan Delahanty", "Loyd Fuentes", "Booker Hassell", "Anamaria Asaro", "Genevive Stanford", "Rich Kivi", "Dotty Fouche", "Rupert Vancleve", "Nydia Buchwald");
        friendsList.setItems(FXCollections.observableList(friends));
    }
}
