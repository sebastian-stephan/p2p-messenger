/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.challengetask.messages;

import org.challengetask.PrivateUserProfile;

/**
 *
 * @author sstephan
 */
public class OnlineStatusMessage extends Message {
    final private boolean onlineStatus;

    public OnlineStatusMessage (String _senderUserID, boolean _onlineStatus) {
        super(_senderUserID, "");
        onlineStatus = _onlineStatus;
    }

    /**
     * @return the onlineStatus
     */
    public boolean isOnline() {
        return onlineStatus;
    }
}
