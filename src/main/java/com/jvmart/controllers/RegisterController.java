package com.jvmart.controllers;

import com.jvmart.services.UserService;
import com.jvmart.utils.AlertHelper;
import com.jvmart.utils.SceneRouter;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class RegisterController {
    @FXML private TextField fullNameField;
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label fullNameError;
    @FXML private Label usernameError;
    @FXML private Label emailError;
    @FXML private Label passwordError;
    @FXML private Label confirmError;
    @FXML private Label successLabel;

    private final UserService userService = new UserService();

    @FXML
    public void initialize() {
        hideErrors();
        successLabel.setVisible(false);
        successLabel.setManaged(false);
    }

    @FXML
    private void onRegister() {
        hideErrors();
        String fullName = fullNameField.getText();
        String username = usernameField.getText();
        String email = emailField.getText();
        String password = passwordField.getText();
        String confirm = confirmPasswordField.getText();

        boolean hasError = false;
        if (fullName == null || fullName.trim().isEmpty()) {
            fullNameError.setText("Full Name is required");
            show(fullNameError);
            hasError = true;
        }
        if (username == null || username.trim().isEmpty()) {
            usernameError.setText("Username is required");
            show(usernameError);
            hasError = true;
        }
        if (email == null || !email.contains("@") || !email.contains(".")) {
            emailError.setText("Invalid email address");
            show(emailError);
            hasError = true;
        }
        if (password == null || password.length() < 6) {
            passwordError.setText("Password must be at least 6 characters");
            show(passwordError);
            hasError = true;
        }
        if (confirm == null || !confirm.equals(password)) {
            confirmError.setText("Passwords do not match");
            show(confirmError);
            hasError = true;
        }
        if (hasError) return;

        Thread.startVirtualThread(() -> {
            var result = userService.register(fullName, username, email, password);
            Platform.runLater(() -> {
                switch (result) {
                    case com.jvmart.services.ServiceResult.Success<?> success -> {
                        successLabel.setVisible(true);
                        successLabel.setManaged(true);
                        AlertHelper.success("Account created! Please sign in.");
                        SceneRouter.navigateTo("login.fxml");
                    }
                    case com.jvmart.services.ServiceResult.Failure<?> failure -> {
                        usernameError.setText(failure.message());
                        show(usernameError);
                    }
                }
            });
        });
    }

    @FXML
    private void onGoToLogin() {
        SceneRouter.navigateTo("login.fxml");
    }

    @FXML
    private void handleRegister() {
        onRegister();
    }

    @FXML
    private void goToLogin() {
        onGoToLogin();
    }

    private void hideErrors() {
        if (fullNameError != null) {
            fullNameError.setVisible(false);
            fullNameError.setManaged(false);
        }
        if (usernameError != null) {
            usernameError.setVisible(false);
            usernameError.setManaged(false);
        }
        if (emailError != null) {
            emailError.setVisible(false);
            emailError.setManaged(false);
        }
        if (passwordError != null) {
            passwordError.setVisible(false);
            passwordError.setManaged(false);
        }
        if (confirmError != null) {
            confirmError.setVisible(false);
            confirmError.setManaged(false);
        }
    }

    private void show(Label label) {
        label.setVisible(true);
        label.setManaged(true);
    }
}
