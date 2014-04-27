package org.challengetask;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
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
import net.tomp2p.futures.BaseFutureAdapter;
import net.tomp2p.futures.FutureGet;
import net.tomp2p.peers.PeerAddress;
import org.challengetask.audio.OpusSoundTest;
import org.challengetask.gui.FXMLLoginController;
import org.challengetask.gui.FXMLMainController;
import org.challengetask.messages.FriendRequestMessage;
import org.challengetask.messages.OnlineStatusMessage;
import org.challengetask.network.ObjectReplyHandler;
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
    private ObservableList<FriendRequestMessage> friendRequestsList;

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

        // Get userprofile
        userProfile = (PrivateUserProfile) getResult;

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

        // Reset all friends list entries 
        for (FriendsListEntry e : userProfile.getFriendsList()) {
            e.setOnline(false);
            e.setPeerAddress(null);
        }

        // Set the observable friends list from the user profile
        friendsList = FXCollections.observableList(userProfile.getFriendsList());

        // Set the observable friend requests list from the user profile
        friendRequestsList = FXCollections.observableList(userProfile.getFriendRequestsList());

        // Set reply handler
        p2p.setObjectDataReply(new ObjectReplyHandler(this));

        // Show main window
        try {
            FXMLLoader fxmlLoader = new FXMLLoader();
            fxmlLoader.setLocation(getClass().getResource("/fxml/MainScene.fxml"));
            Parent mainRoot = fxmlLoader.load();
            mainController = fxmlLoader.getController();
            mainController.setApplication(this);
            mainController.setFriendsList(friendsList);
            mainController.setFriendRequestList(getFriendRequestsList());
            mainController.setTitleText(userID);
            Scene mainScene = new Scene(mainRoot);
            mainStage.setScene(mainScene);
        } catch (IOException ex) {
            Logger.getLogger(MainApp.class.getName()).log(Level.SEVERE, null, ex);
        }

        // Send out online status to all friends
        pingAllFriends(true);

        return new Pair<>(true, "Login successful");
    }

    public ObservableList<FriendsListEntry> getFriendsList() {
        return friendsList;
    }

    public void call(String userID) {
        // TODO
    }

    public boolean existsUser(String userID) {
        return (p2p.get(userID) != null);
    }

    public boolean addFriend(String userID) {
        // Add to list
        friendsList.add(new FriendsListEntry(userID));

        // Send ping
        pingUser(userID, true, true);

        // Save profile
        return savePrivateUserProfile();
    }

    public Pair<Boolean, String> sendFriendRequest(String userID, String messageText) {
        // Check if user already exists in friends list
        if (getFriendsListEntry(userID) != null) {
            return new Pair<>(false, "User already in friendslist");
        }

        // Check if user exists in the network
        if (!existsUser(userID)) {
            return new Pair<>(false, "User was not found");
        }

        // Get public profile of friend we want to add
        PublicUserProfile friendProfile = (PublicUserProfile) p2p.get(userID);

        // Create friend request message
        FriendRequestMessage friendRequestMessage = new FriendRequestMessage(userProfile.getUserID(), messageText);

        // Try to send direct friend request if other user is online
        if (friendProfile.getPeerAddress() != null) {
            if (p2p.send(friendProfile.getPeerAddress(), friendRequestMessage) == false) {
                return new Pair<>(false, "Error sending friend request");
            }
        } else {
            return new Pair<>(false, "Friend doesn't seem to be online");
        }

        // Addd as friend
        if (addFriend(userID) == false) {
            return new Pair<>(false, "Error, adding the friend");
        }

        return new Pair<>(true, "Friend request to " + userID + " was sent");
    }

    public void acceptFriendRequest(FriendRequestMessage message) {
        // Add user
        addFriend(message.getSenderUserID());

        // Remove friend request
        friendRequestsList.remove(message);

        // Save User Profile
        savePrivateUserProfile();
    }

    public void declineFriendRequest(FriendRequestMessage message) {
        // Remove friend request
        friendRequestsList.remove(message);

        // Save User Profile
        savePrivateUserProfile();
    }

    public Pair<Boolean, String> removeFriend(FriendsListEntry friendEntry) {
        if (friendsList.remove(friendEntry) && savePrivateUserProfile()) {
            return new Pair<>(true, "Friend removed");
        } else {
            return new Pair<>(false, "Could not remove friend");
        }
    }

    public void handleIncomingFriendRequest(FriendRequestMessage requestMessage) {
        // Ignore requests from users already in the list
        if (userProfile.isFriendsWith(requestMessage.getSenderUserID())) {
            return;
        }

        // Ignore multiple requests
        if (userProfile.hasFriendRequestFromUser(requestMessage.getSenderUserID())) {
            return;
        }

        // Add friend request
        getFriendRequestsList().add(requestMessage);

        // Save the change
        savePrivateUserProfile();

        // Show visual message
        mainController.showIncomingFriendRequest(requestMessage);
    }

    public String getUserID() {
        return (userProfile != null) ? userProfile.getUserID() : "error";
    }

    @Override
    public void stop() {
        logout();
        shutdown();
    }

    public void logout() {
        // Tell "friends" that i'm going offline
        pingAllFriends(false);

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

    public FriendsListEntry getFriendsListEntry(String userID) {
        for (FriendsListEntry e : friendsList) {
            if (e.getUserID().equals(userID)) {
                return e;
            }
        }
        return null;
    }

    public void pingUser(PeerAddress pa, boolean onlineStatus, boolean replyPongExpected) {
        // Send ping
        OnlineStatusMessage msg = new OnlineStatusMessage(userProfile.getUserID(), onlineStatus, replyPongExpected);
        p2p.sendNonBlocking(pa, msg);
    }

    private void pingUser(String userID, boolean onlineStatus, boolean replyPongExpected) {
        p2p.getNonBLocking(userID, new BaseFutureAdapter<FutureGet>() {
            @Override
            public void operationComplete(FutureGet f) throws Exception {
                FriendsListEntry friendsListEntry = getFriendsListEntry(userID);
                assert(friendsListEntry != null);
                if (f.isSuccess()) {
                    PublicUserProfile publicUserProfile = (PublicUserProfile) f.getData().object();
                    // Set peer address in friendslist
                    PeerAddress peerAddress = publicUserProfile.getPeerAddress();
                    friendsListEntry.setPeerAddress(peerAddress);

                    // Send ping
                    pingUser(publicUserProfile.getPeerAddress(), onlineStatus, replyPongExpected);
                } else {
                    // Can't find other peer, maybe he deleted his account? -> show offline
                    System.out.println("User " + userID + " doesnt seem to exist anymore");
                    friendsListEntry.setOnline(false);
                    friendsListEntry.setPeerAddress(null);
                }
            }
        });

    }

    private void pingAllFriends(boolean onlineStatus) {

        for (FriendsListEntry entry : friendsList) {
            String userID = entry.getUserID();

            // Assuming online friends have the correct peer address
            if (entry.isOnline()) {
                OnlineStatusMessage ping = new OnlineStatusMessage(userProfile.getUserID(), 
                        onlineStatus, onlineStatus);
                p2p.sendNonBlocking(entry.getPeerAddress(), ping);
            } // If friend seems offline, only send out ping if we want to broadcast online=true
            else if (entry.isOnline() == false && onlineStatus == true) {
                // Get users public profile and read it's peer address
                pingUser(userID, onlineStatus, onlineStatus);

            }
        }
    }

    private boolean savePrivateUserProfile() {
        // TODO: encrypt before saving

        return p2p.put(userProfile.getUserID() + userProfile.getPassword(), userProfile);
    }

    /**
     * @return the friendRequestsList
     */
    public ObservableList<FriendRequestMessage> getFriendRequestsList() {
        return friendRequestsList;
    }
}
