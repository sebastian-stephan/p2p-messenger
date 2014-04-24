/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.challengetask.network;

import net.tomp2p.peers.PeerAddress;
import net.tomp2p.rpc.ObjectDataReply;
import org.challengetask.MainApp;

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
            mainApp.handleIncomingFriendRequest((FriendRequestMessage)o);
        }
        return null;
    }
}
