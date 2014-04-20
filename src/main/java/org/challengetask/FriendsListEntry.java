/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.challengetask;

import java.io.Serializable;
import net.tomp2p.peers.PeerAddress;

/**
 *
 * @author sstephan
 */
public class FriendsListEntry implements Serializable {
    private final String userID;
    private PeerAddress peerAddress;
    private boolean online = false;
    private boolean friendshipConfirmed = false;

    public FriendsListEntry(String _userID) {
        userID = _userID;
    }
    
    public FriendsListEntry(String _userID, boolean _friendshipConfirmed) {
        this(_userID);
        friendshipConfirmed = _friendshipConfirmed;
    }
    
    /**
     * @return the userID
     */
    public String getUserID() {
        return userID;
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
     * @return the online
     */
    public boolean isOnline() {
        return online;
    }

    /**
     * @param online the online to set
     */
    public void setOnline(boolean online) {
        this.online = online;
    }

    /**
     * @return the friendshipConfiremed
     */
    public boolean isFriendshipConfiremed() {
        return friendshipConfirmed;
    }

    /**
     * @param friendshipConfiremed the friendshipConfiremed to set
     */
    public void setFriendshipConfiremed(boolean friendshipConfiremed) {
        this.friendshipConfirmed = friendshipConfiremed;
    }
}
