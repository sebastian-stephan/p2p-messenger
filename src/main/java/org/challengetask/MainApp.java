package org.challengetask;

import java.io.IOException;
import java.net.URL;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import static javafx.application.Application.launch;
import static javafx.application.Application.launch;
import static javafx.application.Application.launch;
import static javafx.application.Application.launch;
import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Parent;
import javafx.scene.Scene;
import static javafx.scene.input.DataFormat.URL;
import javafx.stage.Stage;
import org.challengetask.gui.FXMLLoginController;
import org.challengetask.gui.FXMLMainController;



public class MainApp extends Application {
    Stage mainStage;
    FXMLLoginController loginController;
    FXMLMainController mainController;

    @Override
    public void start(Stage stage) throws Exception {
        mainStage = stage;
        
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(getClass().getResource("/fxml/LoginScene.fxml"));
        Parent loginRoot = fxmlLoader.load();
        loginController  = fxmlLoader.getController();
        loginController.setApplication(this);
        Scene loginScene = new Scene(loginRoot);
        
        // Show Login at startup
        mainStage.setTitle("Appname");
        mainStage.setScene(loginScene);
        mainStage.show();
        
    }
    
    public boolean login(String userID, String password) {
        // Check if login is correct, set UserProfile and return true
        Random r = new Random();
        boolean success = r.nextBoolean();
        
        if(success) {
            try {
                FXMLLoader fxmlLoader = new FXMLLoader();
                fxmlLoader.setLocation(getClass().getResource("/fxml/MainScene.fxml"));
                Parent mainRoot = fxmlLoader.load();
                mainController  = fxmlLoader.getController();
                mainController.setApplication(this);
                Scene mainScene = new Scene(mainRoot);
                mainStage.setScene(mainScene);
            } catch (IOException ex) {
                Logger.getLogger(MainApp.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return success;
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

}
