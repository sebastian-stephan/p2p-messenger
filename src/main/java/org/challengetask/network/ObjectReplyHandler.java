/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.challengetask.network;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import net.tomp2p.peers.PeerAddress;
import net.tomp2p.rpc.ObjectDataReply;
import org.challengetask.MainApp;
import org.challengetask.messages.AudioFrame;
import org.challengetask.messages.CallAcceptMessage;
import org.challengetask.messages.CallRequestMessage;
import org.challengetask.messages.ChatMessage;
import org.challengetask.messages.FriendRequestMessage;
import org.challengetask.messages.OnlineStatusMessage;
import org.challengetask.usermanagement.FriendsListEntry;
import org.controlsfx.control.Notifications;

/**
 *
 * @author Sebastian
 */
public class ObjectReplyHandler implements ObjectDataReply {

    private MainApp mainApp;

    public ObjectReplyHandler(MainApp _mainApp) {
        mainApp = _mainApp;
    }

    @Override
    public Object reply(PeerAddress pa, Object o) throws Exception {
        if (o instanceof FriendRequestMessage) {
            Runnable task = () -> {
                mainApp.handleIncomingFriendRequest((FriendRequestMessage) o);
            };
            Platform.runLater(task);
        } else if (o instanceof OnlineStatusMessage) {
            Runnable task = () -> {
                mainApp.handleIncomingOnlineStatus((OnlineStatusMessage) o);
            };
            Platform.runLater(task);
        } else if (o instanceof ChatMessage) {
            Runnable task = () -> {
                ChatMessage msg = (ChatMessage) o;
                mainApp.handleIncomingChatMessage(msg);
            };
            Platform.runLater(task);
        } else if (o instanceof CallRequestMessage) {
            Runnable task = () -> {
                CallRequestMessage msg = (CallRequestMessage) o;
                mainApp.handleIncomingCallRequestMessage(msg);
            };
            Platform.runLater(task);
        } else if (o instanceof CallAcceptMessage) {
            Runnable task = () -> {
                CallAcceptMessage msg = (CallAcceptMessage) o;
                mainApp.handleIncomingCallAcceptMessage(msg);
            };
            Platform.runLater(task);
        } else if (o instanceof AudioFrame) {
            AudioFrame msg = (AudioFrame)o;
            mainApp.handleIncomingAudioFrame(msg);
        }
        return null;
    }
}
