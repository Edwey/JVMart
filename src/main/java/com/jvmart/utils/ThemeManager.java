package com.jvmart.utils;

import javafx.scene.Scene;

public class ThemeManager {
    // Fixed to Dark Theme only
    private static final String DARK_CSS = "/com/jvmart/css/jvmart-dark.css";

    public static void applyTheme(Scene scene) {
        if (scene == null) return;

        // Clear existing stylesheets to prevent conflicts
        scene.getStylesheets().clear();

        java.net.URL cssUrl = ThemeManager.class.getResource(DARK_CSS);
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
            System.out.println("[ThemeManager] Applied Dark Theme");
        } else {
            System.err.println("[ThemeManager] ERROR: Dark theme CSS not found!");
        }
    }

    // Stub method to prevent compilation errors in controllers
    public static void toggleTheme(Scene scene) {
        System.out.println("[ThemeManager] Theme toggle disabled - using Dark Theme only");
    }
}
