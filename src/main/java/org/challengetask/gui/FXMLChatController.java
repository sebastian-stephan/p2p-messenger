package org.challengetask.gui;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.Initializable;
import org.challengetask.MainApp;

public class FXMLChatController implements Initializable {

    private MainApp mainApp;

 
    public void setApplication(MainApp _mainApp) {
        mainApp = _mainApp;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {

    }
}
