package org.challengetask;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import static java.util.concurrent.TimeUnit.SECONDS;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Pair;
import javax.sound.sampled.LineUnavailableException;
import net.tomp2p.futures.BaseFutureAdapter;
import net.tomp2p.futures.FutureGet;
import net.tomp2p.peers.PeerAddress;
import org.challengetask.audio.CallHandler;
import org.challengetask.gui.FXMLCallController;
import org.challengetask.gui.FXMLChatController;
import org.challengetask.gui.FXMLLoginController;
import org.challengetask.gui.FXMLMainController;
import org.challengetask.gui.FriendsListComparator;
import org.challengetask.messages.AudioFrame;
import org.challengetask.messages.CallAcceptMessage;
import org.challengetask.messages.CallRequestMessage;
import org.challengetask.messages.ChatMessage;
import org.challengetask.messages.FriendRequestMessage;
import org.challengetask.messages.OnlineStatusMessage;
import org.challengetask.network.ObjectReplyHandler;
import org.challengetask.network.P2POverlay;
import org.challengetask.usermanagement.FriendsListEntry;
import org.challengetask.usermanagement.PrivateUserProfile;
import org.challengetask.usermanagement.PublicUserProfile;
import org.controlsfx.control.Notifications;
import org.controlsfx.dialog.Dialogs;

public class MainApp extends Application {

    private Stage mainStage;
    private Scene loginScene;
    private FXMLLoginController loginController;
    private FXMLMainController mainController;
    private FXMLCallController activeCallController;

    private P2POverlay p2p;

    private PrivateUserProfile userProfile;
    private ObservableList<FriendsListEntry> friendsList;
    private ObservableList<FriendRequestMessage> friendRequestsList;
    private Map<FriendsListEntry, FXMLChatController> openChats = new HashMap<>();

    private ScheduledExecutorService scheduler;

    @Override
    public void start(Stage stage) {
        mainStage = stage;

        // Get parameters
        String bootstrapIP = getParameters().getNamed().get("bootstrap");
        bootstrapIP = (bootstrapIP == null) ? "127.0.0.1" : bootstrapIP;

        String maxBufferSize = getParameters().getNamed().get("buffersize");
        if (maxBufferSize != null) {
            CallHandler.MAX_PLAY_BUFFER_SIZE = Integer.parseInt(maxBufferSize);
        }

        String frameLength = getParameters().getNamed().get("framelength");
        if (frameLength != null) {
            CallHandler.FRAME_LENGTH = Integer.parseInt(frameLength);
        }

        // What should happen when user closes window
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                stop();
            }
        });

        // Setup network stuff
        p2p = new P2POverlay();

        // Load fonts
        Font.loadFont(getClass().getResource("/fonts/blackrose.ttf").toExternalForm(), 10);

        // Load login screen
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(getClass().getResource("/fxml/LoginScene.fxml"));
        Parent loginRoot = null;
        try {
            loginRoot = fxmlLoader.load();
        } catch (IOException ex) {
            Logger.getLogger(MainApp.class.getName()).log(Level.SEVERE, null, ex);
        }
        loginController = fxmlLoader.getController();
        loginController.setApplication(this);
        loginScene = new Scene(loginRoot);

        // Show login screen
        mainStage.setTitle("tomcall");
        mainStage.setScene(loginScene);
        mainStage.show();

        // Try to bootstrap
        Pair<Boolean, String> result = p2p.bootstrap(bootstrapIP);
        if (result.getKey() == false) {
            Dialogs.create().owner(mainStage)
                    .title("Bootstrap error")
                    .message(result.getValue())
                    .showError();
            mainStage.close();
        }

        System.out.println("Bootstrapped to: " + bootstrapIP
                + "My IP: " + p2p.getPeerAddress().getInetAddress().getHostAddress());

    }

    /**
     * User Account Management *
     */
    public Pair<Boolean, String> createAccount(String userID, String password) {
        // Check if the user is already in the friendslist

        // Check if account exists
        if (p2p.getBlocking(userID) != null) {
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
        Object getResult = p2p.getBlocking(userID + password);
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
        Object objectPublicUserProfile = p2p.getBlocking(userID);
        if (objectPublicUserProfile == null) {
            return new Pair<>(false, "Could not retrieve public userprofile");
        }
        PublicUserProfile publicUserProfile = (PublicUserProfile) objectPublicUserProfile;

        // **** FRIENDS LIST ****
        // Reset all friends list entries to offline and unkown peer address
        for (FriendsListEntry e : userProfile.getFriendsList()) {
            e.setOnline(false);
            e.setPeerAddress(null);
            e.setWaitingForHeartbeat(false);
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

        // Schedule new thread to check periodically if friends are still online
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            pingAllOnlineFriends();
        }, 10, 10, SECONDS);

        return new Pair<>(true, "Login successful");
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
        Object objectPublicUserProfile = p2p.getBlocking(userProfile.getUserID());
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

        scheduler.shutdownNow();
        p2p.setObjectDataReply(null);

        userProfile = null;
        friendsList = null;

        // Change back to login screen
        mainStage.setScene(loginScene);

    }

    public String getUserID() {
        return (userProfile != null) ? userProfile.getUserID() : "error";
    }

    private boolean savePrivateUserProfile() {
        // TODO: encrypt before saving

        return p2p.put(userProfile.getUserID() + userProfile.getPassword(), userProfile);
    }

    /**
     * Friends Management *
     */
    public boolean existsUser(String userID) {
        return (p2p.getBlocking(userID) != null);
    }

    public boolean addFriend(String userID) {
        // Add to list
        friendsList.add(new FriendsListEntry(userID));
        friendsList.sort(new FriendsListComparator());
        mainController.sortFriendsListView();

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
        PublicUserProfile friendProfile = (PublicUserProfile) p2p.getBlocking(userID);

        // Create friend request message
        FriendRequestMessage friendRequestMessage = new FriendRequestMessage(p2p.getPeerAddress(), userProfile.getUserID(), messageText);

        // Try to send direct friend request first, (in case user is online)
        boolean sendDirect = false;
        if (friendProfile.getPeerAddress() != null) {
            sendDirect = p2p.sendBlocking(friendProfile.getPeerAddress(), friendRequestMessage);
        }

        // If that failed, or other has no peer address, append to pub profile of other friend
        if (sendDirect == false) {
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

                // Show notification if user came online
                if (msg.isOnline() && !e.isOnline()) {
                    Notifications.create().text("User " + msg.getSenderUserID() + " just came online")
                            .showInformation();
                }

                // Set online/offline
                e.setOnline(msg.isOnline());
                e.setPeerAddress(msg.getSenderPeerAddress());
                e.setWaitingForHeartbeat(false);

                sortFriendsListView();

                // Send pong back if wanted
                if (msg.isReplyPongExpected()) {
                    pingAddress(msg.getSenderPeerAddress(), true, false);
                }
            }
        }
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

    public void sortFriendsListView() {
        mainController.sortFriendsListView();
    }

    /**
     * Sends a onlineStatusMessage to the selected PeerAddress, indicating if we
     * are online or offline.
     *
     * @param pa: PeerAddress of target client.
     * @param onlineStatus: True if we are online, False if offline
     * @param replyPongExpected True if we want a ping back as confirmation.
     */
    public void pingAddress(PeerAddress pa, boolean onlineStatus, boolean replyPongExpected) {
        // Send ping
        OnlineStatusMessage msg = new OnlineStatusMessage(p2p.getPeerAddress(), userProfile.getUserID(), onlineStatus, replyPongExpected);
        p2p.sendNonBlocking(pa, msg, false);
    }

    /**
     * Sends a onlineStatusMessage to the selected user, whilst first checking
     * for the correct PeerAddress of that user.
     *
     * @param userID: UserID of target.
     * @param onlineStatus: True if we are online, False if offline
     * @param replyPongExpected: True if we want a ping back as confirmation.
     */
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
                        pingAddress(publicUserProfile.getPeerAddress(), onlineStatus, replyPongExpected);
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

    /**
     * Pings all friends in the friendslist with an online or offline status
     * message. If used with onlineStatus=True, a reply is expected from the
     * other users to see who is online. (Used at login) If used with
     * onlineStatus=False, we do not ask for a reply. (Used at logout)
     *
     * @param onlineStatus: True if we go online, False if we go offline.
     */
    private void pingAllFriends(boolean onlineStatus) {
        for (FriendsListEntry entry : friendsList) {
            String userID = entry.getUserID();

            // For friends that are online, send direct to their PeerAddress
            if (entry.isOnline()) {
                OnlineStatusMessage ping = new OnlineStatusMessage(p2p.getPeerAddress(), userProfile.getUserID(),
                        onlineStatus, onlineStatus);
                p2p.sendNonBlocking(entry.getPeerAddress(), ping, false);
            } // For friends that are offline and in the case that we want to tell
            // them we're coming online, use pingUser method to first check for
            // their peerAddress (if any).
            else if (!entry.isOnline() && onlineStatus == true) {
                pingUser(userID, onlineStatus, onlineStatus);

            }
        }
    }

    /**
     * Heartbeat like ping to all online friends to check periodically if they
     * are still online. This will send a OnlineStatusMessage to each online
     * friend and tell them to respond with a reply message. If the friend
     * didn't reply since the last call, she will be set offline.
     */
    private void pingAllOnlineFriends() {
        for (FriendsListEntry entry : friendsList) {
            if (entry.isOnline()) {
                // If friend din't reply since last call, set him offline
                if (entry.isWaitingForHeartbeat()) {
                    entry.setOnline(false);
                    sortFriendsListView();
                }

                // Flag friend until he replies
                entry.setWaitingForHeartbeat(true);

                OnlineStatusMessage ping = new OnlineStatusMessage(p2p.getPeerAddress(), userProfile.getUserID(),
                        true, true);
                p2p.sendNonBlocking(entry.getPeerAddress(), ping, false);
            }
        }

    }

    /**
     * Open a new chat with the given friend, if it is not already open. If it
     * is open, will set the focus to the chat window of that user.
     *
     * @param friend: FriendsListEntry of the other friend.
     */
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

            FXMLChatController controller = fxmlLoader.getController();
            controller.initController(this, stage, friend);

            openChats.put(friend, controller);

            stage.show();
            stage.requestFocus();
        }
    }

    public void removeChatWindow(FriendsListEntry friend) {
        openChats.remove(friend);
    }

    public void sendChatMessage(String text, FriendsListEntry friendsListEntry) {
        ChatMessage chatMessage = new ChatMessage(p2p.getPeerAddress(), userProfile.getUserID(), text);
        p2p.sendNonBlocking(friendsListEntry.getPeerAddress(), chatMessage, false);
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

    /**
     * Call functionality *
     */
    public void openCallWindow(FriendsListEntry friend, boolean _incoming) {
        Parent callRoot = null;
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(getClass().getResource("/fxml/CallScene.fxml"));
        try {
            callRoot = fxmlLoader.load();
        } catch (IOException ex) {
            Logger.getLogger(MainApp.class.getName()).log(Level.SEVERE, null, ex);
        }
        Stage stage = new Stage();
        stage.setTitle("Call");
        stage.setScene(new Scene(callRoot));
        stage.setResizable(false);

        FXMLCallController controller = fxmlLoader.getController();
        controller.initController(this, p2p, stage, friend, _incoming);
        activeCallController = controller;

        stage.show();
        stage.requestFocus();
    }

    public void closeActiveCall() {
        activeCallController = null;
    }

    public void sendCallRequest(FriendsListEntry friendsListEntry) {
        // Show call window
        openCallWindow(friendsListEntry, false);

        // Send call request
        CallRequestMessage callRequestMessage = new CallRequestMessage(p2p.getPeerAddress(), userProfile.getUserID());
        p2p.sendNonBlocking(friendsListEntry.getPeerAddress(), callRequestMessage, false);
    }

    public void handleIncomingCallRequestMessage(CallRequestMessage msg) {
        synchronized (this) {
            FriendsListEntry friend = getFriendsListEntry(msg.getSenderUserID());

            // Only handle call requests from users in friendslist
            if (friend != null) {
                // Check if user is busy with other call
                if (activeCallController != null) {
                    // Send decline, we're busy with another call
                    CallAcceptMessage callAcceptMessage = new CallAcceptMessage(p2p.getPeerAddress(), getUserID());
                    callAcceptMessage.setAccept(false);
                    p2p.sendNonBlocking(msg.getSenderPeerAddress(), callAcceptMessage, false);
                } else {
                    // Show incoming call
                    Notifications.create().title("Incoming call")
                            .text(msg.getSenderUserID() + " is calling")
                            .show();
                    openCallWindow(friend, true);
                }
            }
        }
    }

    public void handleIncomingCallAcceptMessage(CallAcceptMessage msg) {
        synchronized (this) {
            if (activeCallController != null
                    && activeCallController.getFriendsListEntry().getUserID().equals(msg.getSenderUserID())) {
                activeCallController.handleIncomingCallAcceptMessage(msg);
            }
        }
    }

    public void handleIncomingAudioFrame(AudioFrame frame) {
        if (activeCallController != null
                && activeCallController.getFriendsListEntry().getUserID().equals(frame.getSenderUserID())) {
            activeCallController.handleIncomingAudioFrame(frame);
        }
    }

    /**
     * Application shutdown *
     */
    @Override
    public void stop() {
        if (userProfile != null) {
            logout();
        }
        shutdown();
    }

    public void shutdown() {
        // Stop active calls
        if (activeCallController != null) {
            activeCallController.stopTransmitting();
            activeCallController.closeCall();
        }

        // Shutdown Tom P2P stuff
        p2p.shutdown();
    }

}
