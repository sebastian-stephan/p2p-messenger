package org.challengetask.gui;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PopupControl;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Pair;
import org.challengetask.MainApp;
import org.controlsfx.control.Notifications;
import org.controlsfx.control.PopOver;

public class FXMLLoginController implements Initializable {

    private MainApp mainApp;

    @FXML
    private TextField textUserID;
    @FXML
    private TextField textUserPassword;
    @FXML
    private Button buttonLogin;
    @FXML
    private Button buttonCreateAccount;
    @FXML
    private Label labelErrorMessage;
    @FXML
    private Label labelUserID;
    @FXML
    private Label labelUserPassword;

    @FXML
    private void handleLoginButtonClick(ActionEvent event) {
        if (validateUserInput() == false) {
            return;
        }

        Pair<Boolean, String> result = mainApp.login(textUserID.getText(),
                textUserPassword.getText());

        if (result.getKey() == false) {
            Notifications.create().title("Login")
                    .text(result.getValue())
                    .showError();
        }

    }

    @FXML
    private void handleCreateAccountButtonClick(ActionEvent event) {
        if (validateUserInput() == false) {
            return;
        }

        Pair<Boolean, String> result = mainApp.createAccount(textUserID.getText(),
                textUserPassword.getText());

        if (result.getKey() == true) {
            Notifications.create().title("Account creation")
                    .text(result.getValue())
                    .showConfirm();
        } else {
            Notifications.create().title("Account creation")
                    .text(result.getValue())
                    .showError();
        }

    }

    private boolean validateUserInput() {
        boolean valid = true;
        if (textUserID.getText().length() == 0) {
            valid = false;
            labelUserID.getStyleClass().add("invalid");
        } else {
            labelUserID.getStyleClass().remove("invalid");
        }
        if (textUserPassword.getText().length() == 0) {
            valid = false;
            labelUserPassword.getStyleClass().add("invalid");
        } else {
            labelUserPassword.getStyleClass().remove("invalid");
        }

        return valid;
    }

    public void setApplication(MainApp _mainApp) {
        mainApp = _mainApp;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {

    }
}
