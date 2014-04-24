package org.challengetask;

import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import static javafx.application.Application.launch;
import static javafx.application.Application.launch;
import static javafx.application.Application.launch;
import static javafx.application.Application.launch;
import static javafx.application.Application.launch;
import static javafx.application.Application.launch;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Pair;
import net.tomp2p.storage.Data;
import org.challengetask.audio.OpusSoundTest;
import org.challengetask.gui.FXMLLoginController;
import org.challengetask.gui.FXMLMainController;
import org.challengetask.network.P2POverlay;
import org.controlsfx.dialog.Dialogs;

public class MainApp extends Application {

    private Stage mainStage;
    private Scene loginScene;
    private FXMLLoginController loginController;
    private FXMLMainController mainController;
    
    private P2POverlay p2p;
    private OpusSoundTest o;

    private PrivateUserProfile userProfile;
    private ObservableList<FriendsListEntry> friendsList;

    @Override
    public void start(Stage stage) throws Exception {
        mainStage = stage;

        // Setup network stuff
        p2p = new P2POverlay();

        // Load login screen
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(getClass().getResource("/fxml/LoginScene.fxml"));
        Parent loginRoot = fxmlLoader.load();
        loginController = fxmlLoader.getController();
        loginController.setApplication(this);
        loginScene = new Scene(loginRoot);

        // Show login screen
        mainStage.setTitle("Skype");
        mainStage.setScene(loginScene);
        mainStage.show();

        // Try to bootstrap
        Pair<Boolean, String> result = p2p.bootstrap();
        if (result.getKey() == false) {
            Dialogs.create().owner(mainStage)
                    .title("Bootstrap error")
                    .message(result.getValue())
                    .showError();
            mainStage.close();
        }
    }

    public Pair<Boolean, String> createAccount(String userID, String password) {
        // Check if the user is already in the friendslist
        
        
        // Check if account exists
        if (p2p.get(userID) != null) {
            return new Pair<>(false, "Could not create user account. UserID already taken.");
        }

        // Create private UserProfile
        userProfile = new PrivateUserProfile(userID, password);

        // TODO: Encrypt it with password
        if (savePrivateUserProfile() == false) {
            return new Pair<>(false, "Error. Could not save private UserProfile");
        }

        // Create public UserProfile
        PublicUserProfile publicUserProfile;
        publicUserProfile = new PublicUserProfile(userID, userProfile.getKeyPair().getPublic(),
                null);

        if (p2p.put(userID, publicUserProfile)) {
            return new Pair<>(true, "User account for user \"" + userID + "\" successfully created");
        } else {
            return new Pair<>(false, "Network DHT error. Could not save public UserProfile");
        }
    }

    public Pair<Boolean, String> login(String userID, String password) {
        Object getResult = p2p.get(userID + password);

        if (getResult == null) {
            return new Pair<>(false, "Login data not valid, Wrong UserID/password?");
        }

        userProfile = (PrivateUserProfile) getResult;

        // Show main window
        try {
            FXMLLoader fxmlLoader = new FXMLLoader();
            fxmlLoader.setLocation(getClass().getResource("/fxml/MainScene.fxml"));
            Parent mainRoot = fxmlLoader.load();
            mainController = fxmlLoader.getController();
            mainController.setApplication(this);
            Scene mainScene = new Scene(mainRoot);
            mainStage.setScene(mainScene);
        } catch (IOException ex) {
            Logger.getLogger(MainApp.class.getName()).log(Level.SEVERE, null, ex);
        }

        // Update public profile with current IP Address
        Object objectPublicUserProfile = p2p.get(userID);
        if (objectPublicUserProfile == null) {
            return new Pair<>(false, "Could not retrieve public userprofile");
        }
        PublicUserProfile publicUserProfile = (PublicUserProfile) objectPublicUserProfile;
        publicUserProfile.setPeerAddress(p2p.getPeerAddress());
        if (p2p.put(userID, publicUserProfile) == false) {
            return new Pair<>(false, "Could not update peer address in public user profile");
        }

        // Set the FriendsList UI to show the friends in the profile
        friendsList = FXCollections.observableList(userProfile.getFriendsList());
        mainController.setFriendsList(friendsList);

        return new Pair<>(true, "Login successful");
    }

    public List<FriendsListEntry> getFriendsList() {
        return userProfile.getFriendsList();
    }

    public MainApp() {
    }

    public void call(String userID) {
        // TODO
    }

    public boolean existsUser(String userID) {
        return (p2p.get(userID) != null);
    }

    public Pair<Boolean, String> addFriend(String userID, String messageText) {
        if (isUserInFriendsList(userID)) {
            return new Pair<>(false, "User already in friendslist");
        }
        
        if (!existsUser(userID)) {
            return new Pair<>(false, "User was not found");
        }

        // TODO: FriendRequest: check if user is online try to send friend request 
        // directlyif that fails, append the friend request message to the user's
        // public profile
        PublicUserProfile friendProfile = (PublicUserProfile)p2p.get(userID);
        // Try to send direct if possible
        if (friendProfile.getPeerAddress() != null) {
            if (p2p.send(friendProfile.getPeerAddress(), messageText) == false) {
                return new Pair<>(false, "Error sending friend request");
            }
        } else {
            return new Pair<>(false, "Friend doesn't seem to be online");
        }
        
        // Add to friendsList
        friendsList.add(new FriendsListEntry(userID));

        // Save profile in the DHT
        if (savePrivateUserProfile() == false) {
            return new Pair<>(false, "Error, saving the private User Profile");
        }

        return new Pair<>(true, "Friend request to " + userID + " was sent");
    }

    public Pair<Boolean, String> removeFriend(FriendsListEntry friendEntry) {
        if (friendsList.remove(friendEntry) && savePrivateUserProfile()) {
            return new Pair<>(true, "Friend removed");
        } else {
            return new Pair<>(false, "Could not remove friend");
        }
    }

    /**
     * The main() method is ignored in correctly deployed JavaFX application.
     * main() serves only as fallback in case the application can not be
     * launched through deployment artifacts, e.g., in IDEs with limited FX
     * support. NetBeans ignores main().
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void stop() {
        shutdown();
    }

    public void logout() {
        // TODO: Tell "friends" that i'm going offline

        // Set PeerAddress in public Profile to null
        Object objectPublicUserProfile = p2p.get(userProfile.getUserID());
        if (objectPublicUserProfile == null) {
            System.out.println("Could not retrieve public userprofile");
            return;
        }
        PublicUserProfile publicUserProfile = (PublicUserProfile) objectPublicUserProfile;
        publicUserProfile.setPeerAddress(null);
        if (p2p.put(userProfile.getUserID(), publicUserProfile) == false) {
            System.out.println("Could not update peer address in public user profile");
            return;
        }
        
        userProfile = null;
        friendsList = null;
        
        // Change back to login screen
        mainStage.setScene(loginScene);
        
        
    }
    
    /*
     Gracefully disconnect from network
     */
    public void shutdown() {

        // Shutdown Tom P2P stuff
        p2p.shutdown();
    }

    private boolean savePrivateUserProfile() {
        // TODO: encrypt before saving

        return p2p.put(userProfile.getUserID() + userProfile.getPassword(), userProfile);
    }

    private boolean isUserInFriendsList(String userID) {
        for (FriendsListEntry e : friendsList) {
            if (e.getUserID().equals(userID)) {
                return true;
            }
        }
        return false;
    }
}
