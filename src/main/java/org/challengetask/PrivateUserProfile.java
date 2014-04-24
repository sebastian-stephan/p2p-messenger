/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.challengetask;

import java.io.Serializable;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author sstephan
 */
public class PrivateUserProfile implements Serializable {
    private final String userID;
    private final String password;
    private List<FriendsListEntry> friendsList;
    
    private KeyPair keyPair;
    
    public PrivateUserProfile(String _userID, String _password) {
        userID = _userID;
        password = _password;
        
        // Generate KeyPair
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(512);
            keyPair = keyGen.genKeyPair();
        } catch (NoSuchAlgorithmException ex) {
            System.out.println("Could not generate KeyPair");
        }
        
        // Create new friendsList with some fake data
        friendsList = new ArrayList<FriendsListEntry>();
        FriendsListEntry fakeFriend1 = new FriendsListEntry("Online Friend");
        FriendsListEntry fakeFriend2 = new FriendsListEntry("Offline Friend 1");
        FriendsListEntry fakeFriend3 = new FriendsListEntry("Offline Friend 2");
        fakeFriend1.setOnline(true);
        friendsList.add(fakeFriend1);
        friendsList.add(fakeFriend2);
        friendsList.add(fakeFriend3);

        
    }
    
    @Override
    public String toString() {
        return "UserID: " + userID + 
                "\nPassword: " + password + 
                "\n Private Key: " + keyPair.getPrivate().toString() +
                "\n Public Key: " + keyPair.getPublic().toString();
    }

    /**
     * @return the userID
     */
    public String getUserID() {
        return userID;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @return the keyPair
     */
    public KeyPair getKeyPair() {
        return keyPair;
    }

    /**
     * @return the friendsList
     */
    public List<FriendsListEntry> getFriendsList() {
        return friendsList;
    }

    
}
