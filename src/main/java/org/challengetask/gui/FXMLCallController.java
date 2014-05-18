package org.challengetask.gui;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import javax.sound.sampled.LineUnavailableException;
import org.challengetask.MainApp;
import org.challengetask.audio.CallHandler;
import org.challengetask.messages.AudioFrame;
import org.challengetask.messages.CallAcceptMessage;
import org.challengetask.network.P2POverlay;
import org.challengetask.usermanagement.FriendsListEntry;

public class FXMLCallController implements Initializable {

    private MainApp mainApp;
    private P2POverlay p2p;
    private Stage stage;
    private FriendsListEntry friendsListEntry;
    private CallHandler callHandler;
    private boolean isIncomingCall;

    @FXML
    private Label labelFriendID;
    @FXML
    private Button buttonAccept;
    @FXML
    private Button buttonDecline;

    @Override
    public void initialize(URL url, ResourceBundle rb) {

    }

    /**
     * @param stage the stage to set
     */
    public void initController(MainApp _mainApp, P2POverlay _p2p,
            Stage _stage, FriendsListEntry _friendsListEntry, boolean _incoming) {
        mainApp = _mainApp;
        p2p = _p2p;
        stage = _stage;
        friendsListEntry = _friendsListEntry;
        isIncomingCall = _incoming;

        labelFriendID.setText(getFriendsListEntry().getUserID());

        if (isIncomingCall) {
            buttonAccept.setText("Accept Call");
            buttonDecline.setText("Decline Call");
        } else {
            buttonAccept.setText("Call");
            buttonAccept.setDisable(true);
            buttonDecline.setText("Hang up");
        }

        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                stopTransmitting();
                sendAcceptMessage(false);
                closeCall();
            }
        });

    }

    public void sendAcceptMessage(boolean _accept) {
        CallAcceptMessage callAcceptMessage = new CallAcceptMessage(p2p.getPeerAddress(), mainApp.getUserID());
        callAcceptMessage.setAccept(_accept);
        p2p.sendNonBlocking(getFriendsListEntry().getPeerAddress(), callAcceptMessage);
    }

    @FXML
    private void handleAcceptButtonClick(ActionEvent e) {
        if (callHandler == null && isIncomingCall) {
            sendAcceptMessage(true);
            buttonAccept.setDisable(true);
            buttonAccept.setText("Call");
            buttonDecline.setText("Hang up");
            startTransmitting();
        }
    }

    @FXML
    private void handleDeclineButtonClick(ActionEvent e) {
        if (callHandler == null && isIncomingCall) {
            sendAcceptMessage(false);
            closeCall();
        } else if (callHandler == null && !isIncomingCall) {
            sendAcceptMessage(false);
            closeCall();
        } else if (callHandler != null) {
            sendAcceptMessage(false);
            stopTransmitting();
            closeCall();
        }
    }

    public void handleIncomingCallAcceptMessage(CallAcceptMessage msg) {
        if (callHandler != null && msg.isAccept() == false) {
            stopTransmitting();
            closeCall();
        } else {
            if (msg.isAccept()) {
                startTransmitting();
            } else {
                closeCall();
            }
        }
    }
    
    public void handleIncomingAudioFrame(AudioFrame msg) {
        callHandler.addAudioFrame(msg.getData());
    }

    private void startTransmitting() {
        // Create new call
        callHandler = new CallHandler(mainApp, p2p, getFriendsListEntry());
        try {
            callHandler.start();
        } catch (LineUnavailableException ex) {
            stopTransmitting();
            System.out.println("LineUnavailableException");
        }
    }

    public void stopTransmitting() {
        if (callHandler == null) {
            return;
        }
        callHandler.stop();
        try {
            Thread.sleep(100);
        } catch (InterruptedException ex) {
            Logger.getLogger(FXMLCallController.class.getName()).log(Level.SEVERE, null, ex);
        }
        callHandler = null;
    }

    public void closeCall() {
        mainApp.closeActiveCall();
        stage.close();
    }

    /**
     * @return the friendsListEntry
     */
    public FriendsListEntry getFriendsListEntry() {
        return friendsListEntry;
    }

}
