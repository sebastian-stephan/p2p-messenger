/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.challengetask.messages;

import java.io.Serializable;
import org.challengetask.PrivateUserProfile;

/**
 *
 * @author Sebastian
 */
public abstract class Message implements Serializable {
    private final String senderUserID;
    protected final String messageText;


    
    public Message (String _senderUserID, String _messageText) {
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
    
}
