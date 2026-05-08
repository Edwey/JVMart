package com.jvmart.controllers;

import com.jvmart.dao.sql.CartDAO;
import com.jvmart.models.CartItem;
import com.jvmart.models.User;
import com.jvmart.services.ActivityLogService;
import com.jvmart.services.UserService;
import com.jvmart.session.SessionManager;
import com.jvmart.utils.AlertHelper;
import com.jvmart.utils.SceneRouter;
import com.jvmart.utils.ThemeManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.sql.SQLException;
import java.util.List;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label usernameError;
    @FXML private Label passwordError;
    @FXML private Label loginError;

    private final UserService userService = new UserService();
    private final ActivityLogService activityLogService = new ActivityLogService();

    @FXML
    public void initialize() {
        hideErrors();
        
        // Add Enter key handler to both fields
        if (usernameField != null) {
            usernameField.setOnKeyPressed(event -> {
                if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                    onLogin();
                }
            });
        }
        
        if (passwordField != null) {
            passwordField.setOnKeyPressed(event -> {
                if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                    onLogin();
                }
            });
        }
    }

    @FXML
    private void onLogin() {
        hideErrors();
        String username = usernameField.getText();
        String password = passwordField.getText();

        boolean hasError = false;
        if (username == null || username.trim().isEmpty()) {
            usernameError.setText("Username is required");
            show(usernameError);
            hasError = true;
        }
        if (password == null || password.trim().isEmpty()) {
            passwordError.setText("Password is required");
            show(passwordError);
            hasError = true;
        }
        if (hasError) return;

        Thread.startVirtualThread(() -> {
            var result = userService.login(username, password);
            switch (result) {
                case com.jvmart.services.ServiceResult.Success<?> success -> {
                    User user = (User) success.value();
                    var rows = switch (user.getRole()) {
                        case "admin" -> java.util.List.<com.jvmart.models.CartItem>of();
                        default -> loadCartQuiet(user.getId());
                    };
                    Platform.runLater(() -> {
                        SessionManager sm = SessionManager.getInstance();
                        sm.login(user);
                        sm.replaceCart(rows);
                        activityLogService.log(user.getId(), "LOGIN", "User logged in successfully");
                        switch (user.getRole()) {
                            case "admin" -> SceneRouter.navigateTo("admin_overview.fxml");
                            default -> SceneRouter.navigateTo("customer_home.fxml");
                        }
                    });
                }
                case com.jvmart.services.ServiceResult.Failure<?> failure ->
                        Platform.runLater(() -> AlertHelper.error("Login failed: " + failure.message()));
            }
        });
    }

    private static List<CartItem> loadCartQuiet(int userId) {
        try {
            return new CartDAO().loadCartForUser(userId);
        } catch (SQLException e) {
            return List.of();
        }
    }

    @FXML
    private void onKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            onLogin();
        }
    }

    @FXML
    private void onRegister() {
        SceneRouter.navigateTo("register.fxml");
    }

    @FXML
    private void onForgotPassword() {
        SceneRouter.navigateTo("forgot_password.fxml");
    }

    @FXML
    private void onHelloButtonClick() {
        AlertHelper.info("Hello", "Welcome to JVMart.");
    }

    @FXML
    private void onToggleTheme() {
        if (usernameField != null && usernameField.getScene() != null) {
            ThemeManager.toggleTheme(usernameField.getScene());
        }
    }

    private void hideErrors() {
        if (usernameError != null) {
            usernameError.setVisible(false);
            usernameError.setManaged(false);
        }
        if (passwordError != null) {
            passwordError.setVisible(false);
            passwordError.setManaged(false);
        }
        if (loginError != null) {
            loginError.setVisible(false);
            loginError.setManaged(false);
        }
    }

    private void show(Label label) {
        label.setVisible(true);
        label.setManaged(true);
    }
}
