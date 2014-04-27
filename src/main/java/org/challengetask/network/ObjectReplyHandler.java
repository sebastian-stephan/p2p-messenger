/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.challengetask.network;

import javafx.application.Platform;
import net.tomp2p.peers.PeerAddress;
import net.tomp2p.rpc.ObjectDataReply;
import org.challengetask.FriendsListEntry;
import org.challengetask.MainApp;
import org.challengetask.messages.FriendRequestMessage;
import org.challengetask.messages.OnlineStatusMessage;
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
                OnlineStatusMessage msg = (OnlineStatusMessage) o;
                FriendsListEntry e = mainApp.getFriendsListEntry(msg.getSenderUserID());
                // If friend is in friendslist
                if (e != null) {
                    // Set online
                    e.setOnline(msg.isOnline());
                    e.setPeerAddress(pa);
                    mainApp.getFriendsList().remove(e);
                    mainApp.getFriendsList().add(e);

                    // Show notification
                    if (msg.isOnline()) {
                        Notifications.create().text("User " + msg.getSenderUserID() + " just came online")
                                .showInformation();
                    }

                    // Send pong back if wanted
                    if (msg.isReplyPongExpected()) {
                        mainApp.pingUser(pa, true, false);
                    }
                }
            };
            Platform.runLater(task);
        }
        return null;
    }
}
