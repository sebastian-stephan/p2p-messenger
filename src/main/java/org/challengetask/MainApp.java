package org.challengetask;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Pair;
import net.tomp2p.futures.BaseFutureAdapter;
import net.tomp2p.futures.FutureGet;
import net.tomp2p.peers.PeerAddress;
import org.challengetask.audio.OpusSoundTest;
import org.challengetask.gui.FXMLChatController;
import org.challengetask.gui.FXMLLoginController;
import org.challengetask.gui.FXMLMainController;
import org.challengetask.gui.FriendsListComparator;
import org.challengetask.messages.ChatMessage;
import org.challengetask.messages.FriendRequestMessage;
import org.challengetask.messages.OnlineStatusMessage;
import org.challengetask.network.ObjectReplyHandler;
import org.challengetask.network.P2POverlay;
import org.controlsfx.control.Notifications;
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
    private Map<FriendsListEntry, FXMLChatController> openChats = new HashMap<>();

    @Override
    public void start(Stage stage) throws Exception {
        mainStage = stage;
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                stop();
            }
        });

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
        String bootstrapIP = getParameters().getNamed().get("bootstrap");
        bootstrapIP = (bootstrapIP == null) ? "127.0.0.1" : bootstrapIP;
        Pair<Boolean, String> result = p2p.bootstrap(bootstrapIP);
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
        // Get userprofile if password and username are correct
        Object getResult = p2p.get(userID + password);
        if (getResult == null) {
            return new Pair<>(false, "Login data not valid, Wrong UserID/password?");
        }
        userProfile = (PrivateUserProfile) getResult;

        // Load FXML
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(getClass().getResource("/fxml/MainScene.fxml"));
        Parent mainRoot;
        try {
            mainRoot = fxmlLoader.load();
        } catch (IOException ex) {
            Logger.getLogger(MainApp.class.getName()).log(Level.SEVERE, null, ex);
            return new Pair<>(false, "Error loading the main window");
        }

        // Get controller of main window
        mainController = fxmlLoader.getController();
        mainController.setApplication(this);

        // Get public user profile
        Object objectPublicUserProfile = p2p.get(userID);
        if (objectPublicUserProfile == null) {
            return new Pair<>(false, "Could not retrieve public userprofile");
        }
        PublicUserProfile publicUserProfile = (PublicUserProfile) objectPublicUserProfile;

        // **** FRIENDS LIST ****
        // Reset all friends list entries to offline and unkown peer address
        for (FriendsListEntry e : userProfile.getFriendsList()) {
            e.setOnline(false);
            e.setPeerAddress(null);
        }
        // Set the observable friends list from the user profile
        friendsList = FXCollections.synchronizedObservableList(FXCollections.observableList(userProfile.getFriendsList()));
        friendsList.sort(new FriendsListComparator());
        mainController.setFriendsListSource(friendsList);

        // **** FRIEND REQUESTS ****
        // Set the observable friendrequests list from the user profile
        friendRequestsList = FXCollections.observableList(userProfile.getFriendRequestsList());
        mainController.setFriendRequestListSource(friendRequestsList);

        // Digest all pending friend requests in public user profile
        for (FriendRequestMessage msg : publicUserProfile.getPendingFriendRequests()) {
            handleIncomingFriendRequest(msg);
        }
        publicUserProfile.getPendingFriendRequests().clear();

        // Set current IP address in public user profile
        publicUserProfile.setPeerAddress(p2p.getPeerAddress());

        // Save public user profile
        if (p2p.put(userID, publicUserProfile) == false) {
            return new Pair<>(false, "Could not update public user profile");
        }

        // Set Title
        mainController.setTitleText(userID);

        // Show scene
        Scene mainScene = new Scene(mainRoot);
        mainStage.setScene(mainScene);

        // Set reply handler
        p2p.setObjectDataReply(new ObjectReplyHandler(this));

        // Send out online status to all friends
        pingAllFriends(true);

        return new Pair<>(true, "Login successful");
    }

    public boolean existsUser(String userID) {
        return (p2p.get(userID) != null);
    }

    public boolean addFriend(String userID) {
        // Add to list
        friendsList.add(new FriendsListEntry(userID));
        friendsList.sort(new FriendsListComparator());
        mainController.updateFriendsListView();

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
        FriendRequestMessage friendRequestMessage = new FriendRequestMessage(p2p.getPeerAddress(), userProfile.getUserID(), messageText);

        // Try to send direct friend request if other user is online
        if (friendProfile.getPeerAddress() != null) {
            if (p2p.send(friendProfile.getPeerAddress(), friendRequestMessage) == false) {
                return new Pair<>(false, "Error sending direct friend request");
            }
        } else {
            // Friend is not online, append to public profile
            friendProfile.getPendingFriendRequests().add(friendRequestMessage);
            if (p2p.put(userID, friendProfile) == false) {
                return new Pair<>(false, "Error sending friend request");
            }
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
        friendRequestsList.add(requestMessage);

        // Save the change
        savePrivateUserProfile();

        // Show visual message
        mainController.showIncomingFriendRequest(requestMessage);
    }

    public void handleIncomingOnlineStatus(OnlineStatusMessage msg) {
        synchronized (this) {
            FriendsListEntry e = getFriendsListEntry(msg.getSenderUserID());

            // If friend is in friendslist
            if (e != null) {
                // Set online
                e.setOnline(msg.isOnline());
                e.setPeerAddress(msg.getSenderPeerAddress());

                updateFriendsListView();
                // Show notification
                if (msg.isOnline()) {
                    Notifications.create().text("User " + msg.getSenderUserID() + " just came online")
                            .showInformation();
                }

                // Send pong back if wanted
                if (msg.isReplyPongExpected()) {
                    pingUser(msg.getSenderPeerAddress(), true, false);
                }
            }
        }
    }

    public void handleIncomingChatMessage(ChatMessage msg) {
        synchronized (openChats) {
            FriendsListEntry e = getFriendsListEntry(msg.getSenderUserID());

            // If friend is in friendslist
            if (e != null) {
                // Is there already a chat window open?
                FXMLChatController openChat = openChats.get(e);
                if (openChat == null) {
                    openChatWindow(e);
                    openChat = openChats.get(e);
                } 
                
                // Send chat message to controller
                openChat.showIncomingChatMessage(msg.getSenderUserID(), msg.getMessageText());
            }
        }
    }

    

    public void openChatWindow(FriendsListEntry friend) {
        // Check if there is already a chat window open
        if (openChats.containsKey(friend)) {
            openChats.get(friend).getStage().requestFocus();
        } else {
            Parent chatRoot = null;
            FXMLLoader fxmlLoader = new FXMLLoader();
            fxmlLoader.setLocation(getClass().getResource("/fxml/ChatScene.fxml"));
            try {
                chatRoot = fxmlLoader.load();
            } catch (IOException ex) {
                Logger.getLogger(MainApp.class.getName()).log(Level.SEVERE, null, ex);
            }
            Stage stage = new Stage();
            stage.setTitle("Chat");
            stage.setScene(new Scene(chatRoot, 450, 300));
            stage.show();

            FXMLChatController controller = fxmlLoader.getController();
            controller.initController(this, stage, friend);

            openChats.put(friend, controller);
        }
    }

    public void sendChatMessage(String text, FriendsListEntry friendsListEntry) {
        ChatMessage chatMessage = new ChatMessage(p2p.getPeerAddress(), userProfile.getUserID(), text);
        p2p.sendNonBlocking(friendsListEntry.getPeerAddress(), chatMessage);
    }

    public void removeChatWindow(FriendsListEntry friend) {
        openChats.remove(friend);
    }

    public String getUserID() {
        return (userProfile != null) ? userProfile.getUserID() : "error";
    }

    public ObservableList<FriendsListEntry> getFriendsList() {
        return friendsList;
    }

    public FriendsListEntry getFriendsListEntry(String userID) {
        for (FriendsListEntry e : friendsList) {
            if (e.getUserID().equals(userID)) {
                return e;
            }
        }
        return null;
    }

    public void updateFriendsListView() {
        mainController.updateFriendsListView();
    }

    @Override
    public void stop() {
        if (userProfile != null) {
            logout();
        }
        shutdown();
    }

    public void logout() {
        // Close all chat windows
        for (FXMLChatController controller : openChats.values()) {
            controller.closeChat();
        }
        openChats.clear();

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
        
        p2p.setObjectDataReply(null);

        userProfile = null;
        friendsList = null;

        // Change back to login screen
        mainStage.setScene(loginScene);

    }

    public void shutdown() {

        // Shutdown Tom P2P stuff
        p2p.shutdown();
    }

    public void pingUser(PeerAddress pa, boolean onlineStatus, boolean replyPongExpected) {
        // Send ping
        OnlineStatusMessage msg = new OnlineStatusMessage(p2p.getPeerAddress(), userProfile.getUserID(), onlineStatus, replyPongExpected);
        p2p.sendNonBlocking(pa, msg);
    }

    private void pingUser(String userID, boolean onlineStatus, boolean replyPongExpected) {
        p2p.getNonBLocking(userID, new BaseFutureAdapter<FutureGet>() {
            @Override
            public void operationComplete(FutureGet f) throws Exception {
                FriendsListEntry friendsListEntry = getFriendsListEntry(userID);
                assert (friendsListEntry != null);
                if (f.isSuccess()) {
                    PublicUserProfile publicUserProfile = (PublicUserProfile) f.getData().object();
                    // Set peer address in friendslist
                    PeerAddress peerAddress = publicUserProfile.getPeerAddress();
                    friendsListEntry.setPeerAddress(peerAddress);

                    // Send ping
                    if (peerAddress != null) {
                        pingUser(publicUserProfile.getPeerAddress(), onlineStatus, replyPongExpected);
                    }
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
                OnlineStatusMessage ping = new OnlineStatusMessage(p2p.getPeerAddress(), userProfile.getUserID(),
                        onlineStatus, onlineStatus);
                p2p.sendNonBlocking(entry.getPeerAddress(), ping);
            } // If friend seems offline, only send out ping if we come online
            else if (!entry.isOnline() && onlineStatus == true) {
                // Get users public profile and read it's peer address
                pingUser(userID, onlineStatus, onlineStatus);

            }
        }
    }

    private boolean savePrivateUserProfile() {
        // TODO: encrypt before saving

        return p2p.put(userProfile.getUserID() + userProfile.getPassword(), userProfile);
    }

}
