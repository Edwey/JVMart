package com.jvmart.utils;

import com.jvmart.controllers.AdminShellController;
import com.jvmart.controllers.MainShellController;
import com.jvmart.session.SessionManager;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Routes full-window scenes and holds navigation-scoped arguments for the current navigation.
 * Each {@link #navigateTo(String)} or {@link #navigateTo(String, Map)} replaces the navigation
 * state map entirely. Use {@link #mergeNavigationArguments(Map)} only when injecting content via
 * {@link #loadContent(StackPane, String)} without a full navigation cycle.
 */
public final class SceneRouter {
    private static final Logger LOGGER = Logger.getLogger(SceneRouter.class.getName());

    private static Stage primaryStage;
    /** Arguments for the in-flight navigation; cleared and repopulated on each navigateTo. */
    private static final Map<String, Object> navigationState = Collections.synchronizedMap(new HashMap<>());

    private static final Set<String> ADMIN_CONTENT_PAGES = Set.of(
            "admin_overview.fxml",
            "admin_products.fxml",
            "admin_orders.fxml",
            "admin_customers.fxml",
            "admin_inventory.fxml",
            "admin_reports.fxml",
            "admin_reviews.fxml",
            "admin_reviews_analytics.fxml"
    );
    private static final Set<String> MAIN_CONTENT_PAGES = Set.of(
            "customer_home.fxml",
            "product_catalog.fxml",
            "my_orders.fxml",
            "cart.fxml",
            "checkout.fxml",
            "product_detail.fxml",
            "product_reviews.fxml",
            "wishlist.fxml",
            "order_confirmation.fxml"
    );
    private static final Set<String> NORMAL_WINDOW_PAGES = Set.of(
            "login.fxml",
            "register.fxml",
            "forgot_password.fxml"
    );

    private SceneRouter() {}

    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }

    /**
     * Merges key-value pairs into the current navigation state without clearing existing entries.
     * Intended for embedding content without calling {@link #navigateTo(String)} (e.g. swap-in loaders).
     */
    public static void mergeNavigationArguments(Map<String, ?> extra) {
        if (extra == null || extra.isEmpty()) {
            return;
        }
        extra.forEach(navigationState::put);
    }

    /** Read-only peek for routing arguments belonging to this navigation (e.g. category filter). */
    @SuppressWarnings("unchecked")
    public static <T> T getNavigationArgument(String key) {
        return (T) navigationState.get(key);
    }

    /** Remove and return a one-shot routing argument after use (optional). */
    @SuppressWarnings("unchecked")
    public static <T> T consumeNavigationArgument(String key) {
        return (T) navigationState.remove(key);
    }

    public static void loadContent(StackPane container, String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneRouter.class.getResource("/com/jvmart/fxml/" + fxmlFile));
            Parent content = loader.load();
            container.getChildren().clear();
            container.getChildren().add(content);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to load embedded content: " + fxmlFile, e);
            AlertHelper.error("Could not load " + fxmlFile);
        }
    }

    public static void navigateTo(String fxmlName) {
        navigateTo(fxmlName, Map.of());
    }

    /** Navigates to a screen replacing navigation state entirely with {@code navigationProps}. */
    public static void navigateTo(String fxmlName, Map<String, ?> navigationProps) {
        if (primaryStage == null) {
            throw new IllegalStateException("Primary stage not set. Call SceneRouter.setPrimaryStage() before navigating.");
        }
        navigationState.clear();
        if (navigationProps != null && !navigationProps.isEmpty()) {
            navigationProps.forEach(navigationState::put);
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

            String title = fxmlName.replace(".fxml", "").replace("_", " ");
            title = title.substring(0, 1).toUpperCase() + title.substring(1);
            primaryStage.setTitle("JVMart - " + title);

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Navigation failed: " + fxmlName, e);
            // #region agent log
            com.jvmart.utils.DebugLog.log(
                    "sceneRouterFxmlLoad",
                    "SceneRouter.navigateTo",
                    "Navigation failed to load FXML",
                    java.util.Map.of(
                            "fxmlName", fxmlName,
                            "exceptionType", e.getClass().getSimpleName(),
                            "exceptionMessage", e.getMessage() == null ? "" : e.getMessage()
                    ),
                    "pre-fix"
            );
            // #endregion
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
