package org.challengetask.gui;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.util.Callback;
import javafx.util.Pair;
import org.challengetask.FriendsListEntry;
import org.challengetask.MainApp;
import org.challengetask.network.FriendRequestMessage;
import org.controlsfx.control.Notifications;
import org.controlsfx.control.PopOver;
import org.controlsfx.dialog.Dialogs;
import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.GlyphFont;
import org.controlsfx.glyphfont.GlyphFontRegistry;

public class FXMLMainController implements Initializable {

    private MainApp mainApp;

    @FXML
    private Label labelUserIDTitel;
    @FXML
    private Button buttonLogout;
    @FXML
    private Button buttonFriendRequests;
    @FXML
    private Button buttonAddFriend;
    @FXML
    private ListView friendsList;

    public void setApplication(MainApp _mainApp) {
        mainApp = _mainApp;
    }

    public void setFriendsList(ObservableList observableFriendsList) {
        friendsList.setItems(observableFriendsList);
    }
    
    public void setTitleText(String s) {
        labelUserIDTitel.setText(s);
    }
    
    public void showIncomingFriendRequest(FriendRequestMessage requestMessage) {
        String message = "User " + requestMessage.getSenderUserID() + " wants " +
                        " to add you: \n" + requestMessage.getMessageText();
        Runnable task = () -> { 
            Notifications.create().title("Friend request")
                    .text(message).showConfirm();
        };
        Platform.runLater(task);
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Set up custom friendslist
        friendsList.setCellFactory(new Callback<ListView<FriendsListEntry>, ListCell<FriendsListEntry>>() {

            @Override
            public ListCell<FriendsListEntry> call(ListView<FriendsListEntry> param) {
                return new FriendsListCell();
            }

        });
        
        // Add fancy icons to the buttons
        buttonAddFriend.setGraphic(GlyphFontRegistry.glyph("FontAwesome|PLUS"));
        buttonLogout.setGraphic(GlyphFontRegistry.glyph("FontAwesome|SIGNOUT"));
        buttonFriendRequests.setGraphic(GlyphFontRegistry.glyph("FontAwesome|USER"));
     

    }

    class FriendsListCell extends ListCell<FriendsListEntry> {

        @Override
        public void updateItem(FriendsListEntry _item, boolean _empty) {
            super.updateItem(_item, _empty);

            if (!_empty) {
                HBox leftHbox = new HBox();
                leftHbox.setSpacing(10);
                leftHbox.setAlignment(Pos.CENTER_LEFT);

                HBox rightHbox = new HBox();
                rightHbox.setSpacing(10);
                rightHbox.setAlignment(Pos.CENTER_RIGHT);
                HBox.setHgrow(rightHbox, Priority.ALWAYS);

                Label label = new Label(_item.getUserID());
                Circle circle = new Circle(8);
                circle.setFill(Color.GREY);

                if (_item.isOnline()) {
                    circle.setFill(Color.GREEN);
                    Button callButton = new Button("Call");
                    callButton.getStyleClass().add("callButton");
                    callButton.setGraphic(GlyphFontRegistry.glyph("FontAwesome|PHONE"));
                    callButton.setOnAction(new EventHandler<ActionEvent>() {

                        @Override
                        public void handle(ActionEvent event) {
                            mainApp.call(_item.getUserID());
                            Notifications.create().title("Call")
                                    .text("Calling " + _item.getUserID())
                                    .show();
                        }

                    });
                    rightHbox.getChildren().add(callButton);
                }

                leftHbox.getChildren().addAll(circle, label, rightHbox);
                setGraphic(leftHbox);

                // Context menu to remove friends
                MenuItem removeMenu = new MenuItem("Remove");
                removeMenu.setOnAction(new EventHandler<ActionEvent>() {

                    @Override
                    public void handle(ActionEvent event) {
                        Pair<Boolean, String> result = mainApp.removeFriend(_item);
                        Notifications resultNotification = Notifications.create()
                                .title("Remove friend")
                                .text(result.getValue());
                        if (result.getKey() == true) {
                            resultNotification.showInformation();
                        } else {
                            resultNotification.showError();
                        }
                    }

                });
                ContextMenu contextMenu = new ContextMenu(removeMenu);
                setContextMenu(contextMenu);

            } else {
                setGraphic(null);
                setText(null);
            }
        }
    }

    @FXML
    private void handleAddFriendButtonClick(ActionEvent event) {
        Dialogs addUserDialog = Dialogs.create()
                .title("Add new friend")
                .message("Please enter the UserID of the person you want to add");
        String userID = addUserDialog.showTextInput();
        if (userID != null && !userID.isEmpty()) {
            if (mainApp.existsUser(userID) == false) {
                Dialogs.create()
                        .title("Add new friend")
                        .message("Could not find the user " + userID)
                        .showError();
            } else {
                // Ask for a message
                String addMessage = Dialogs.create()
                        .title("Add new friend")
                        .message("Please enter a message")
                        .showTextInput("Hey " + userID + ",\n\n please accept my friend request.");
                if (addMessage != null && !addMessage.isEmpty()) {
                    Pair<Boolean, String> result = mainApp.addFriend(userID, addMessage);
                    Notifications resultNotification = Notifications.create()
                            .title("Add new friend")
                            .text(result.getValue());
                    if (result.getKey() == true) {
                        resultNotification.showInformation();
                    } else {
                        resultNotification.showError();
                    }
                }
            }
        }
    }
    
    @FXML
    private void handleLogoutButtonClick(ActionEvent event) {
        mainApp.logout();
    }
    
}
