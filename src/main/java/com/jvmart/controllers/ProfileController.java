package com.jvmart.controllers;

import com.jvmart.config.MySQLConnection;
import com.jvmart.models.User;
import com.jvmart.services.ActivityLogService;
import com.jvmart.session.SessionManager;
import com.jvmart.utils.AlertHelper;
import com.jvmart.utils.SceneRouter;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ProfileController {
    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private TextField usernameField;
    @FXML private TextField phoneField;
    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label profileError;
    @FXML private Label securityError;
    @FXML private Label profileNameLabel;
    @FXML private Label profileRoleLabel;
    private final ActivityLogService activityLogService = new ActivityLogService();

    @FXML
    public void initialize() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) {
            SceneRouter.navigateTo("login.fxml");
            return;
        }

        // Pre-fill form with current user data
        if (fullNameField != null) {
            fullNameField.setText(currentUser.getFullName());
        }
        if (emailField != null) {
            emailField.setText(currentUser.getEmail());
        }
        if (usernameField != null) {
            usernameField.setText(currentUser.getUsername());
        }
        if (phoneField != null) {
            phoneField.setText("");
        }
        
        // Update profile header
        if (profileNameLabel != null) {
            profileNameLabel.setText(currentUser.getFullName());
        }
        if (profileRoleLabel != null) {
            profileRoleLabel.setText(currentUser.getRole().toUpperCase());
        }
    }

    @FXML
    private void onSave() {
        hideErrors();
        
        String fullName = fullNameField.getText();
        String email = emailField.getText();
        String username = usernameField.getText();

        // Basic validation
        if (fullName == null || fullName.trim().isEmpty()) {
            profileError.setText("Full name is required");
            show(profileError);
            return;
        }

        if (email == null || email.trim().isEmpty()) {
            profileError.setText("Email is required");
            show(profileError);
            return;
        }

        if (username == null || username.trim().isEmpty()) {
            profileError.setText("Username is required");
            show(profileError);
            return;
        }

        // Update user profile in database
        Thread.startVirtualThread(() -> {
            try {
                User currentUser = SessionManager.getInstance().getCurrentUser();
                if (currentUser == null) {
                    Platform.runLater(() -> {
                        AlertHelper.error("Error", "User session expired. Please login again.");
                        SceneRouter.navigateTo("login.fxml");
                    });
                    return;
                }

                // Check if username is already taken by another user
                if (!username.equals(currentUser.getUsername()) && isUsernameTaken(username)) {
                    Platform.runLater(() -> {
                        profileError.setText("Username is already taken");
                        show(profileError);
                    });
                    return;
                }

                // Update user in database
                updateUserProfile(currentUser.getId(), fullName, email, username);

                // Update session
                currentUser.setFullName(fullName);
                currentUser.setEmail(email);
                currentUser.setUsername(username);
                SessionManager.getInstance().login(currentUser);

                Platform.runLater(() -> {
                    if (profileNameLabel != null) {
                        profileNameLabel.setText(fullName);
                    }
                    activityLogService.logCurrentUser("UPDATE_PROFILE", "Updated profile details.");
                    AlertHelper.success("Profile Updated", "Your profile information has been saved successfully.");
                });
            } catch (SQLException e) {
                Platform.runLater(() -> {
                    profileError.setText("Error updating profile: " + e.getMessage());
                    show(profileError);
                });
            }
        });
    }

    @FXML
    private void onChangePassword() {
        hideErrors();
        
        String currentPassword = currentPasswordField.getText();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // Validation
        if (currentPassword == null || currentPassword.trim().isEmpty()) {
            securityError.setText("Current password is required");
            show(securityError);
            return;
        }

        if (newPassword == null || newPassword.length() < 6) {
            securityError.setText("New password must be at least 6 characters");
            show(securityError);
            return;
        }

        if (confirmPassword == null || !confirmPassword.equals(newPassword)) {
            securityError.setText("New passwords do not match");
            show(securityError);
            return;
        }

        // Update password in database
        Thread.startVirtualThread(() -> {
            try {
                User currentUser = SessionManager.getInstance().getCurrentUser();
                if (currentUser == null) {
                    Platform.runLater(() -> {
                        AlertHelper.error("Error", "User session expired. Please login again.");
                        SceneRouter.navigateTo("login.fxml");
                    });
                    return;
                }

                // Verify current password
                if (!BCrypt.checkpw(currentPassword, currentUser.getPassword())) {
                    Platform.runLater(() -> {
                        securityError.setText("Current password is incorrect");
                        show(securityError);
                    });
                    return;
                }

                // Hash new password
                String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());

                // Update in database
                updatePassword(currentUser.getId(), hashedPassword);

                // Update session
                currentUser.setPassword(hashedPassword);
                SessionManager.getInstance().login(currentUser);

                Platform.runLater(() -> {
                    currentPasswordField.clear();
                    newPasswordField.clear();
                    confirmPasswordField.clear();
                    activityLogService.logCurrentUser("CHANGE_PASSWORD", "Updated account password.");
                    AlertHelper.success("Password Updated", "Your password has been changed successfully.");
                });
            } catch (SQLException e) {
                Platform.runLater(() -> {
                    securityError.setText("Error updating password: " + e.getMessage());
                    show(securityError);
                });
            }
        });
    }

    private boolean isUsernameTaken(String username) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        try (Connection conn = MySQLConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    private void updateUserProfile(int userId, String fullName, String email, String username) throws SQLException {
        String sql = "UPDATE users SET full_name = ?, email = ?, username = ? WHERE id = ?";
        try (Connection conn = MySQLConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, fullName);
            stmt.setString(2, email);
            stmt.setString(3, username);
            stmt.setInt(4, userId);
            stmt.executeUpdate();
        }
    }

    private void updatePassword(int userId, String hashedPassword) throws SQLException {
        String sql = "UPDATE users SET password = ? WHERE id = ?";
        try (Connection conn = MySQLConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, hashedPassword);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        }
    }

    @FXML
    private void onBack() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null && "admin".equals(currentUser.getRole())) {
            SceneRouter.navigateTo("admin_overview.fxml");
        } else {
            SceneRouter.navigateTo("customer_home.fxml");
        }
    }

    @FXML
    private void onLogout() {
        activityLogService.logCurrentUser("LOGOUT", "User logged out.");
        SessionManager.getInstance().logout();
        SceneRouter.navigateTo("login.fxml");
    }

    private void hideErrors() {
        if (profileError != null) {
            profileError.setVisible(false);
            profileError.setManaged(false);
        }
        if (securityError != null) {
            securityError.setVisible(false);
            securityError.setManaged(false);
        }
    }

    private void show(Label label) {
        if (label != null) {
            label.setVisible(true);
            label.setManaged(true);
        }
    }
}
