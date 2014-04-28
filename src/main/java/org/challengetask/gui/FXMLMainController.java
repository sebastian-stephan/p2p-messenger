package org.challengetask.gui;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Callback;
import javafx.util.Pair;
import org.challengetask.FriendsListEntry;
import org.challengetask.MainApp;
import org.challengetask.messages.FriendRequestMessage;
import org.controlsfx.control.Notifications;
import org.controlsfx.control.PopOver;
import org.controlsfx.dialog.Dialogs;
import org.controlsfx.glyphfont.GlyphFontRegistry;

public class FXMLMainController implements Initializable {

    private MainApp mainApp;
    
    private PopOver friendRequestsPopover;
    private ListView friendRequestList;

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

    public void setFriendsListSource(ObservableList list) {
        friendsList.setItems(list);
    }
    
    public void updateFriendsListView() {
        ObservableList list = friendsList.getItems();
        list.sort(new FriendsListComparator());
        friendsList.setItems(null);
        friendsList.setItems(list);
    }
    
    public void setFriendRequestListSource(ObservableList observableFriendRequestList) {
        // Bind friend requests list to data
        friendRequestList.setItems(observableFriendRequestList);
        
        // Update button text with number of pending requests
        buttonFriendRequests.setText(String.valueOf(observableFriendRequestList.size()));
        
        // Whenever a change occures (new or less friend requests, update button)
        observableFriendRequestList.addListener(new ListChangeListener() {
            @Override
            public void onChanged(ListChangeListener.Change c) {
                buttonFriendRequests.setText(String.valueOf(c.getList().size()));
            }
        });
    }

    public void setTitleText(String s) {
        labelUserIDTitel.setText(s);
    }

    public void showIncomingFriendRequest(FriendRequestMessage requestMessage) {
        // Show notification
        String message = "User " + requestMessage.getSenderUserID() + " wants "
                + " to add you: \n" + requestMessage.getMessageText();
        Notifications.create().title("Friend request").text(message).showConfirm();
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Set up custom friendslist view
        friendsList.setCellFactory(new Callback<ListView<FriendsListEntry>, ListCell<FriendsListEntry>>() {

            @Override
            public ListCell<FriendsListEntry> call(ListView<FriendsListEntry> param) {
                return new FriendsListCell();
            }

        });
        
        // Set up custom friend request list vew
        friendRequestList = new ListView();
        friendRequestList.setPrefWidth(350);
        friendRequestList.setCellFactory(new Callback<ListView<FriendRequestMessage>, ListCell<FriendRequestMessage>>() {
            @Override
            public ListCell<FriendRequestMessage> call(ListView<FriendRequestMessage> param) {
                return new FriendRequestListCell();
            }
        });

        // Add fancy icons to the buttons
        buttonAddFriend.setGraphic(GlyphFontRegistry.glyph("FontAwesome|PLUS"));
        buttonLogout.setGraphic(GlyphFontRegistry.glyph("FontAwesome|SIGNOUT"));
        buttonFriendRequests.setGraphic(GlyphFontRegistry.glyph("FontAwesome|USER"));
        buttonFriendRequests.setText("0");

        // Set up popover for friends request
        friendRequestsPopover = new PopOver(friendRequestList);
        friendRequestsPopover.setArrowLocation(PopOver.ArrowLocation.TOP_LEFT);
        friendRequestsPopover.setDetachable(false);
        friendRequestsPopover.setAutoHide(true);
        
        
    }
    
    class FriendRequestListCell extends ListCell<FriendRequestMessage> {
        
        @Override
        public void updateItem(FriendRequestMessage _item, boolean _empty) {
            super.updateItem(_item, _empty);
            
            if(!_empty) {
                VBox vBox = new VBox();
                vBox.setSpacing(10);
                VBox.setVgrow(vBox, Priority.ALWAYS);

                HBox leftHbox = new HBox();
                leftHbox.setSpacing(10);
                leftHbox.setAlignment(Pos.TOP_LEFT);

                HBox rightHbox = new HBox();
                rightHbox.setSpacing(10);
                rightHbox.setAlignment(Pos.TOP_RIGHT);
                rightHbox.setMinWidth(130);
                HBox.setHgrow(rightHbox, Priority.ALWAYS);

                Label userName = new Label(_item.getSenderUserID());
                Button acceptButton = new Button("Accept");
                acceptButton.setOnAction((ActionEvent event) -> {
                    mainApp.acceptFriendRequest(_item);
                    friendRequestsPopover.hide();
                });
                
                Button declineButton = new Button("Decline");
                declineButton.setOnAction((ActionEvent event) -> {
                    mainApp.declineFriendRequest(_item);
                    friendRequestsPopover.hide();
                });
                acceptButton.getStyleClass().add("buttonFriendRequest");
                declineButton.getStyleClass().add("buttonFriendRequest");
                
                rightHbox.getChildren().addAll(acceptButton, declineButton);
                leftHbox.getChildren().addAll(userName, rightHbox);
                
                Label message = new Label(_item.getMessageText());
                message.setWrapText(true);
                
                vBox.getChildren().addAll(leftHbox, message);
                
                setGraphic(vBox);
                
                setPrefHeight(100);
                setPrefWidth(friendRequestList.getWidth());
                
            } else {
                setGraphic(null);
                setText(null);
            }
        }
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
                            //mainApp.call(_item.getUserID());
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
    private void handleFriendRequestsButtonClick(MouseEvent e) {
        final Scene scene = buttonFriendRequests.getScene();
        final Point2D windowCoord = new Point2D(scene.getWindow().getX(), scene.getWindow().getY());
        final Point2D nodeCoord = buttonFriendRequests.localToScene(0.0, 0.0);
        final double xBase = windowCoord.getX() + nodeCoord.getX();
        final double yBase = windowCoord.getY() + nodeCoord.getY();
        final double xOff = 28; //TODO: fix magic numbers
        final double yOff = 71;
        
        if (friendRequestsPopover.isShowing())
            friendRequestsPopover.hide();
        else
            friendRequestsPopover.show(buttonFriendRequests, xBase + xOff, yBase + yOff);

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
            } else if (mainApp.getUserID().equals(userID)) {
                Dialogs.create()
                        .title("Add new friend")
                        .message("You cannot add yourself")
                        .showError();
            } else {
                // Ask for a message
                String addMessage = Dialogs.create()
                        .title("Add new friend")
                        .message("Please enter a message")
                        .showTextInput("Hey " + userID + ",\n\n please accept my friend request.");
                if (addMessage != null && !addMessage.isEmpty()) {
                    Pair<Boolean, String> result = mainApp.sendFriendRequest(userID, addMessage);
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
