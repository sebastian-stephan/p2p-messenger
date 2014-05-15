package org.challengetask.gui;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.challengetask.usermanagement.FriendsListEntry;
import org.challengetask.MainApp;

public class FXMLChatController implements Initializable {

    private MainApp mainApp;
    private Stage stage;
    private FriendsListEntry friendsListEntry;

    @FXML
    private TextField textMessage;
    @FXML
    private Button buttonSend;
    @FXML
    private VBox vBoxChatHistory;
    @FXML
    private ScrollPane scrollPane;

    @Override
    public void initialize(URL url, ResourceBundle rb) {

    }

    /**
     * @param stage the stage to set
     */
    public void initController(MainApp _mainApp, Stage stage, FriendsListEntry _friendsListEntry) {
        this.mainApp = _mainApp;
        this.stage = stage;
        this.friendsListEntry = _friendsListEntry;

        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                closeChat();
            }
        });

        textMessage.setOnAction((event) -> sendMessage());

        vBoxChatHistory.heightProperty().addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                scrollPane.setVvalue((double) newValue);
            }
        });
    }

    @FXML
    private void handleSendButtonClick(ActionEvent e) {
        sendMessage();
    }

    public void showIncomingChatMessage(String friendUserID, String text) {
        Label messageLabel = new Label(friendUserID + ": " + text);
        messageLabel.getStyleClass().add("chatMessageFriend");
        vBoxChatHistory.getChildren().add(messageLabel);
    }

    private void sendMessage() {
        String chatMessage = textMessage.getText();
        mainApp.sendChatMessage(chatMessage, friendsListEntry);
        
        Label messageLabel = new Label(mainApp.getUserID() + ": " + chatMessage);
        messageLabel.getStyleClass().add("chatMessageSelf");
        vBoxChatHistory.getChildren().add(messageLabel);
        textMessage.clear();
    }

    public void closeChat() {
        mainApp.removeChatWindow(friendsListEntry);
        stage.close();
    }

    /**
     * @return the stage
     */
    public Stage getStage() {
        return stage;
    }
}
