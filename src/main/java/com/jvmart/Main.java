package com.jvmart;

import com.jvmart.utils.SceneRouter;
import com.jvmart.utils.ThemeManager;
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import java.util.logging.Logger;

public class Main extends Application {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    
    @Override
    public void start(Stage stage) throws Exception {
        SceneRouter.setPrimaryStage(stage);
        stage.setMinWidth(1100);
        stage.setMinHeight(700);
        stage.setMaximized(false);
        stage.setWidth(1200);
        stage.setHeight(780);
        stage.centerOnScreen();
        stage.setTitle("JVMart");
        
        // Set application icon
        try {
            Image appIcon = new Image(getClass().getResourceAsStream("/com/jvmart/icon/icon.png"));
            if (appIcon != null) {
                stage.getIcons().add(appIcon);
            }
        } catch (Exception e) {
            LOGGER.warning("Could not load application icon: " + e.getMessage());
        }
        
        // Initial navigation to Login
        SceneRouter.navigateTo("login.fxml");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
