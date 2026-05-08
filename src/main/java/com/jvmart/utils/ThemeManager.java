package com.jvmart.utils;

import javafx.scene.Scene;
import javafx.scene.control.DialogPane;

import java.net.URL;
import java.util.prefs.Preferences;

/**
 * Light/dark themes via stylesheet swap; persists with {@link Preferences}.
 */
public final class ThemeManager {
    private static final Preferences PREFS = Preferences.userNodeForPackage(ThemeManager.class);
    /** Course-style key stored as {@code dark} or {@code light}. */
    private static final String PREFS_THEME_KEY = "jvmart.theme";

    static final String LIGHT_CSS = "/com/jvmart/css/jvmart.css";
    static final String DARK_CSS = "/com/jvmart/css/jvmart-dark.css";

    private ThemeManager() {}

    public static boolean isDark() {
        String v = PREFS.get(PREFS_THEME_KEY, "dark").trim();
        return !"light".equalsIgnoreCase(v);
    }

    public static void toggleTheme(Scene scene) {
        PREFS.put(PREFS_THEME_KEY, isDark() ? "light" : "dark");
        applyTheme(scene);
    }

    public static void applyTheme(Scene scene) {
        if (scene == null) {
            return;
        }
        scene.getStylesheets().clear();
        String path = isDark() ? DARK_CSS : LIGHT_CSS;
        URL url = ThemeManager.class.getResource(path);
        if (url != null) {
            scene.getStylesheets().add(url.toExternalForm());
        } else {
            java.util.logging.Logger.getLogger(ThemeManager.class.getName())
                    .severe("Missing stylesheet resource: " + path);
        }
    }

    public static URL getThemeStylesheetUrl() {
        String path = isDark() ? DARK_CSS : LIGHT_CSS;
        URL u = ThemeManager.class.getResource(path);
        return u != null ? u : ThemeManager.class.getResource(DARK_CSS);
    }

    public static void applyThemeToDialog(DialogPane dialogPane) {
        if (dialogPane == null) {
            return;
        }
        dialogPane.getStylesheets().clear();
        URL url = getThemeStylesheetUrl();
        if (url != null) {
            dialogPane.getStylesheets().add(url.toExternalForm());
        }
    }

    /** For tests — normal UI uses {@link #toggleTheme(Scene)}. */
    static void setPreferenceDark(boolean dark) {
        PREFS.put(PREFS_THEME_KEY, dark ? "dark" : "light");
    }
}
