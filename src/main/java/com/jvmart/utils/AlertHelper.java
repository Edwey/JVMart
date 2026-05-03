package com.jvmart.utils;

import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.Region;

public class AlertHelper {
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

    private static void showAlert(AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        DialogPane dialogPane = alert.getDialogPane();
        
        // Set minimum width for better readability
        dialogPane.setMinWidth(400);
        dialogPane.setPrefWidth(450);
        
        // Apply dark theme styling
        dialogPane.getStylesheets().add(
            AlertHelper.class.getResource("/com/jvmart/css/jvmart-dark.css").toExternalForm()
        );
        
        // Style the dialog pane itself
        dialogPane.setStyle(
            "-fx-background-color: #231F20;"
        );
        
        // Style buttons
        for (ButtonType buttonType : dialogPane.getButtonTypes()) {
            Node button = dialogPane.lookupButton(buttonType);
            if (button != null) {
                button.setStyle(
                    "-fx-background-color: #BB4430;" +
                    "-fx-text-fill: #EFE6DD;" +
                    "-fx-font-weight: bold;" +
                    "-fx-background-radius: 8;" +
                    "-fx-padding: 8 16 8 16;"
                );
            }
        }

        alert.showAndWait();
    }
}
