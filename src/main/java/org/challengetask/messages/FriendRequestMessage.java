/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.challengetask.messages;

import net.tomp2p.peers.PeerAddress;
import org.challengetask.usermanagement.PrivateUserProfile;

/**
 *
 * @author Sebastian
 */
public class FriendRequestMessage extends Message {

    
    public FriendRequestMessage (PeerAddress _senderPeerAddress, String _senderUserID, String _messageText) {
        super(_senderPeerAddress, _senderUserID, _messageText);
    }
   
}
