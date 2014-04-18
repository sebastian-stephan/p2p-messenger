/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.challengetask;

import java.io.Serializable;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

/**
 *
 * @author sstephan
 */
public class UserProfile implements Serializable {
    private final String userID;
    private final String password;
    
    private KeyPair keyPair;
    
    public UserProfile(String _userID, String _password) {
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

    
}
