package com.jvmart.utils;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;

/** Dialogs themed via {@link ThemeManager}. */
public final class AlertHelper {

    private AlertHelper() {}

    public static void info(String message) {
        showAlert(AlertType.INFORMATION, "Information", message);
    }

    public static void info(String title, String message) {
        showAlert(AlertType.INFORMATION, title, message);
    }

    public static void success(String message) {
        showAlert(AlertType.INFORMATION, "Success", message);
    }

    public static void success(String title, String message) {
        showAlert(AlertType.INFORMATION, title, message);
    }

    public static void error(String message) {
        showAlert(AlertType.ERROR, "Error", message);
    }

    public static void error(String title, String message) {
        showAlert(AlertType.ERROR, title, message);
    }

    private static void attachTheme(DialogPane dialogPane) {
        dialogPane.setMinWidth(400);
        dialogPane.setPrefWidth(450);
        ThemeManager.applyThemeToDialog(dialogPane);
    }

    private static void showAlert(AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        attachTheme(alert.getDialogPane());
        alert.showAndWait();
    }

    public static boolean confirm(String title, String message, Runnable onConfirm) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        attachTheme(alert.getDialogPane());

        boolean result = alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
        if (result && onConfirm != null) {
            onConfirm.run();
        }
        return result;
    }
}
