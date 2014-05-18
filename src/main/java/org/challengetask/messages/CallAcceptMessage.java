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
public class CallAcceptMessage extends Message {
    
    private boolean accept;

    public CallAcceptMessage(PeerAddress _senderPeerAddress, String _senderUserID) {
        super(_senderPeerAddress, _senderUserID, "");
    }

    /**
     * @return the accept
     */
    public boolean isAccept() {
        return accept;
    }

    /**
     * @param accept the accept to set
     */
    public void setAccept(boolean accept) {
        this.accept = accept;
    }
}
