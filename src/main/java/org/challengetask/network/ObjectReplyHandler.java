/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.challengetask.network;

import javafx.application.Platform;
import net.tomp2p.peers.PeerAddress;
import net.tomp2p.rpc.ObjectDataReply;
import org.challengetask.MainApp;
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
                mainApp.handleIncomingFriendRequest((FriendRequestMessage)o);
            };
            Platform.runLater(task);
        }
        return null;
    }
}
