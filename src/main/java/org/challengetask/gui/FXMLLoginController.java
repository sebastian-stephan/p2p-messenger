package org.challengetask.gui;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.challengetask.MainApp;

public class FXMLLoginController implements Initializable {
    private MainApp mainApp;
    
    @FXML private TextField textUserID;
    @FXML private TextField textUserPassword;
    @FXML private Button buttonLogin;
    @FXML private Button buttonCreateAccount;
    @FXML private Label labelErrorMessage;
    
    @FXML private void handleLoginButtonClick (ActionEvent event) {
        boolean success = mainApp.login("mentos", "passwort");
        
        if(!success) {
            labelErrorMessage.setText("Could not log into the system. UserID/Password wrong?");
            textUserPassword.setText("");
        }
    }
    
    @FXML private void handleCreateAccountButtonClick (ActionEvent event) {
        mainApp.createAccount(textUserID.getText(), textUserPassword.getText());
    }
    
    public void setMessage(String s) {
        labelErrorMessage.setText(s);
    }
    
    public void setApplication(MainApp _mainApp) {
        mainApp = _mainApp;
    }
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        
    }    
}
