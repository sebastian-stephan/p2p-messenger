/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.challengetask.network;

import java.io.Serializable;
import net.tomp2p.peers.PeerAddress;
import org.challengetask.PrivateUserProfile;
import org.challengetask.PublicUserProfile;

/**
 *
 * @author Sebastian
 */
public class FriendRequestMessage implements Serializable {
    private final String senderUserID;
    private final String messageText;

    public FriendRequestMessage (PrivateUserProfile _senderProfile, String _messageText) {
        senderUserID = _senderProfile.getUserID();
        messageText = _messageText;
    }
    
    /**
     * @return the senderUserID
     */
    public String getSenderUserID() {
        return senderUserID;
    }

    /**
     * @return the messageText
     */
    public String getMessageText() {
        return messageText;
    }
    
}
