/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.challengetask.messages;

import net.tomp2p.peers.PeerAddress;

/**
 *
 * @author sstephan
 */
public class CallRequestMessage extends Message {

    public CallRequestMessage(PeerAddress _senderPeerAddress, String _senderUserID) {
        super(_senderPeerAddress, _senderUserID, "");
    }
}
