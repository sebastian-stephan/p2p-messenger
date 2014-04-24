package org.challengetask.gui;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Callback;
import javafx.util.Pair;
import org.challengetask.FriendsListEntry;
import org.challengetask.MainApp;
import org.controlsfx.control.Notifications;
import org.controlsfx.dialog.Dialogs;

public class FXMLMainController implements Initializable {

    private MainApp mainApp;

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

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        friendsList.setCellFactory(new Callback<ListView<FriendsListEntry>, ListCell<FriendsListEntry>>() {

            @Override
            public ListCell<FriendsListEntry> call(ListView<FriendsListEntry> param) {
                return new FriendsListCell();
            }

        });
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
                    Dialogs resultDialog = Dialogs.create()
                            .title("Add new friend")
                            .masthead(null)
                            .message(result.getValue());
                    if (result.getKey() == true) {
                        resultDialog.showInformation();
                    } else {
                        resultDialog.showError();
                    }
                }
            }
        }
    }
}
