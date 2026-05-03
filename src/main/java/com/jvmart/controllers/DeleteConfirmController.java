package com.jvmart.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class DeleteConfirmController {
    @FXML private Label confirmMessage;
    
    private Runnable onConfirmCallback;

    public void setConfirmMessage(String message) {
        confirmMessage.setText(message);
    }

    public void setOnConfirm(Runnable callback) {
        this.onConfirmCallback = callback;
    }

    @FXML
    private void onConfirm() {
        if (onConfirmCallback != null) {
            onConfirmCallback.run();
        }
        close();
    }

    @FXML
    private void onCancel() {
        close();
    }

    private void close() {
        Stage stage = (Stage) confirmMessage.getScene().getWindow();
        stage.close();
    }
}
