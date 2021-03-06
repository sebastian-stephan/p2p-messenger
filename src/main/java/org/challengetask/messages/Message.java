/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.challengetask.messages;

import java.io.Serializable;
import net.tomp2p.peers.PeerAddress;
import org.challengetask.usermanagement.PrivateUserProfile;

/**
 *
 * @author Sebastian
 */
public abstract class Message implements Serializable {
    private final String senderUserID;
    private final PeerAddress senderPeerAddress;
    protected final String messageText;


    
    public Message (PeerAddress _senderPeerAddress, String _senderUserID, String _messageText) {
        senderPeerAddress = _senderPeerAddress;
        senderUserID = _senderUserID;
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

    /**
     * @return the senderPeerAddress
     */
    public PeerAddress getSenderPeerAddress() {
        return senderPeerAddress;
    }
    
}
