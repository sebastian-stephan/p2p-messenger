package org.challengetask;

import java.io.IOException;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import static javafx.application.Application.launch;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.challengetask.audio.OpusSoundTest;
import org.challengetask.gui.FXMLLoginController;
import org.challengetask.gui.FXMLMainController;
import org.challengetask.network.P2POverlay;

public class MainApp extends Application {

    private Stage mainStage;
    private FXMLLoginController loginController;
    private FXMLMainController mainController;
    private P2POverlay p2p;
    private OpusSoundTest o;
    
    private String userProfile;

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
        Scene loginScene = new Scene(loginRoot);

        // Show login screen
        mainStage.setTitle("Appname");
        mainStage.setScene(loginScene);
        mainStage.show();

        // Try to bootstrap and show result on the bottom of the login screen
        loginController.setMessage(p2p.bootstrap());
        //o = new OpusSoundTest();
        //o.start();

    }

    public boolean createAccount(String userID, String password) {
        // Check if account exists
        if (p2p.get(userID) != null) {
            return false;
        }

        return p2p.put(userID, "This is the profile of " + userID);
    }

    public boolean login(String userID, String password) {
        Object getResult = p2p.get(userID);
        if (getResult == null) {
            return false;
        }

        userProfile = (String)getResult;

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

        return true;
    }

    public MainApp() {
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
        logout();
    }

    public void logout() {
        //o.stop();
        p2p.shutdown();
    }

}
