/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.challengetask.usermanagement;

import java.io.Serializable;
import java.security.PublicKey;
import java.util.ArrayList;
import net.tomp2p.peers.PeerAddress;
import org.challengetask.messages.FriendRequestMessage;

/**
 *
 * @author sstephan
 */
public class PublicUserProfile implements Serializable {
    private final String userID;
    private final PublicKey publicKey;
    private PeerAddress peerAddress;
    private ArrayList<FriendRequestMessage> pendingFriendRequests;

    public PublicUserProfile(String _userID, PublicKey _publicKey, PeerAddress _peerAddress) {
        userID = _userID;
        publicKey = _publicKey;
        peerAddress = _peerAddress;
        pendingFriendRequests = new ArrayList<>();
    }
    
    /**
     * @return the userID
     */
    public String getUserID() {
        return userID;
    }

    /**
     * @return the publicKey
     */
    public PublicKey getPublicKey() {
        return publicKey;
    }

    /**
     * @return the peerAddress
     */
    public PeerAddress getPeerAddress() {
        return peerAddress;
    }

    /**
     * @param peerAddress the peerAddress to set
     */
    public void setPeerAddress(PeerAddress peerAddress) {
        this.peerAddress = peerAddress;
    }

    /**
     * @return the pendingFriendRequests
     */
    public ArrayList<FriendRequestMessage> getPendingFriendRequests() {
        return pendingFriendRequests;
    }
    
}
