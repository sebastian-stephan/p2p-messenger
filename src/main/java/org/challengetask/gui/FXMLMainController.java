package org.challengetask.gui;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Callback;
import org.challengetask.FriendsListEntry;
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

    static class FriendsListCell extends ListCell<FriendsListEntry> {

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
                Circle circle = new Circle(5);
                circle.setFill(Color.GREY);

                if (_item.isOnline() && _item.isFriendshipConfiremed()) {
                    circle.setFill(Color.GREEN);
                    rightHbox.getChildren().add(new Button("Call"));
                } else if (!_item.isFriendshipConfiremed()) {
                    circle.setFill(Color.YELLOW);
                }


                leftHbox.getChildren().addAll(circle, label, rightHbox);
                setGraphic(leftHbox);
            }
        }
    }
}
