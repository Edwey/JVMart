package com.jvmart.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;

import com.jvmart.controllers.AdminShellController;
import com.jvmart.controllers.MainShellController;
import com.jvmart.session.SessionManager;

public class SceneRouter {
    private static Stage primaryStage;
    public static final Map<String, Object> transferData = new HashMap<>();
    private static final Set<String> ADMIN_CONTENT_PAGES = Set.of(
            "admin_overview.fxml",
            "admin_products.fxml",
            "admin_orders.fxml",
            "admin_customers.fxml",
            "admin_inventory.fxml",
            "admin_reports.fxml"
    );
    private static final Set<String> MAIN_CONTENT_PAGES = Set.of(
            "customer_home.fxml",
            "product_catalog.fxml",
            "my_orders.fxml",
            "cart.fxml",
            "checkout.fxml",
            "product_detail.fxml",
            "order_confirmation.fxml"
    );
    private static final Set<String> NORMAL_WINDOW_PAGES = Set.of(
            "login.fxml",
            "register.fxml"
    );

    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }

    public static void navigateTo(String fxmlName) {
        if (primaryStage == null) {
            throw new IllegalStateException("Primary stage not set. Call SceneRouter.setPrimaryStage() before navigating.");
        }
        try {
            Parent root;
            if (ADMIN_CONTENT_PAGES.contains(fxmlName)) {
                root = loadAdminShell(fxmlName);
            } else if ("profile.fxml".equals(fxmlName) && isAdminSession()) {
                root = loadAdminShell(fxmlName);
            } else if (MAIN_CONTENT_PAGES.contains(fxmlName)) {
                root = loadMainShell(fxmlName);
            } else if ("profile.fxml".equals(fxmlName)) {
                root = loadMainShell(fxmlName);
            } else {
                root = loadFXML("/com/jvmart/fxml/" + fxmlName);
            }

            Scene scene = primaryStage.getScene();
            if (scene == null) {
                scene = new Scene(root);
                primaryStage.setScene(scene);
            } else {
                scene.setRoot(root);
            }

            ThemeManager.applyTheme(scene);
            applyWindowMode(fxmlName);

            // Set title based on filename
            String title = fxmlName.replace(".fxml", "").replace("_", " ");
            title = title.substring(0, 1).toUpperCase() + title.substring(1);
            primaryStage.setTitle("JVMart - " + title);
            
            // Clear transferData after navigation to prevent memory leak
            // Data is read during controller initialization, so we can clear it after a short delay
            javafx.application.Platform.runLater(() -> transferData.clear());

        } catch (IOException e) {
            e.printStackTrace();
            AlertHelper.error("Navigation Error: Could not load " + fxmlName);
        }
    }

    private static Parent loadAdminShell(String contentFxml) throws IOException {
        FXMLLoader loader = new FXMLLoader(SceneRouter.class.getResource("/com/jvmart/fxml/admin_layout.fxml"));
        Parent shell = loader.load();
        AdminShellController controller = loader.getController();
        controller.setContent(contentFxml);
        return shell;
    }

    private static Parent loadMainShell(String contentFxml) throws IOException {
        FXMLLoader loader = new FXMLLoader(SceneRouter.class.getResource("/com/jvmart/fxml/main_layout.fxml"));
        Parent shell = loader.load();
        MainShellController controller = loader.getController();
        controller.setContent(contentFxml);
        return shell;
    }

    public static Parent loadFXML(String resourcePath) throws IOException {
        java.net.URL resource = SceneRouter.class.getResource(resourcePath);
        if (resource == null) {
            throw new IOException("FXML resource not found: " + resourcePath);
        }
        FXMLLoader loader = new FXMLLoader(resource);
        return loader.load();
    }

    private static boolean isAdminSession() {
        var user = SessionManager.getInstance().getCurrentUser();
        return user != null && "admin".equalsIgnoreCase(user.getRole());
    }

    private static void applyWindowMode(String fxmlName) {
        if (primaryStage == null) {
            return;
        }
        if (NORMAL_WINDOW_PAGES.contains(fxmlName)) {
            primaryStage.setMaximized(false);
            primaryStage.setWidth(1200);
            primaryStage.setHeight(780);
            primaryStage.centerOnScreen();
        } else {
            primaryStage.setMaximized(true);
        }
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }
}
