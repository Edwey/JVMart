package com.jvmart.controllers;

import com.jvmart.dao.sql.UserDAO;
import com.jvmart.models.User;
import com.jvmart.utils.AlertHelper;
import com.jvmart.utils.SceneRouter;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.SQLException;

public class ForgotPasswordController {
    @FXML private TextField emailField;
    @FXML private TextField tokenField;
    @FXML private Label emailError;
    @FXML private Label tokenError;
    @FXML private Label resetError;

    private final UserDAO userDAO = new UserDAO();

    @FXML
    public void initialize() {
        hideErrors();
    }

    private void hideErrors() {
        if (emailError != null) {
            emailError.setVisible(false);
            emailError.setManaged(false);
        }
        if (tokenError != null) {
            tokenError.setVisible(false);
            tokenError.setManaged(false);
        }
        if (resetError != null) {
            resetError.setVisible(false);
            resetError.setManaged(false);
        }
    }

    @FXML
    private void onReset() {
        hideErrors();
        String email = emailField.getText();
        String token = tokenField.getText();

        boolean hasError = false;
        if (email == null || email.trim().isEmpty()) {
            if (emailError != null) {
                emailError.setText("Email is required");
                show(emailError);
            }
            hasError = true;
        }
        if (token == null || token.trim().isEmpty()) {
            if (tokenError != null) {
                tokenError.setText("Recovery code is required");
                show(tokenError);
            }
            hasError = true;
        }
        if (hasError) return;

        // Offline password reset - find user by email and reset to token
        Thread.startVirtualThread(() -> {
            try {
                // Find user by email
                User user = findUserByEmail(email.trim());
                if (user == null) {
                    Platform.runLater(() -> {
                        if (resetError != null) {
                            resetError.setText("No account found with this email");
                            show(resetError);
                        }
                    });
                    return;
                }

                // Generate secure random temporary password
                String newPassword = generateTempPassword();

                // For simplicity: token must match email prefix (offline demo mode)
                // In production: token would be sent via email/SMS
                String expectedToken = user.getEmail().split("@")[0];
                if (!token.equals(expectedToken)) {
                    Platform.runLater(() -> {
                        if (tokenError != null) {
                            tokenError.setText("Invalid recovery code — use exactly the username part before @ in your email.");
                            show(tokenError);
                        }
                    });
                    return;
                }
                String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
                
                // Update password in database
                updatePassword(user.getId(), hashedPassword);

                String finalPassword = newPassword;
                Platform.runLater(() -> {
                    AlertHelper.success("Password Reset", "Your temporary password is: " + finalPassword + "\nPlease login and change it immediately.");
                    SceneRouter.navigateTo("login.fxml");
                });
            } catch (SQLException e) {
                Platform.runLater(() -> {
                    if (resetError != null) {
                        resetError.setText("Error: " + e.getMessage());
                        show(resetError);
                    }
                });
            }
        });
    }

    private String generateTempPassword() {
        // Generate 8-character alphanumeric password
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
        java.security.SecureRandom random = new java.security.SecureRandom();
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private User findUserByEmail(String email) throws SQLException {
        // Find user by email - need to add this method to UserDAO
        String sql = "SELECT id, full_name, username, email, password, role, created_at FROM users WHERE email = ?";
        try (java.sql.Connection conn = com.jvmart.config.MySQLConnection.getInstance();
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new User(
                        rs.getInt("id"),
                        rs.getString("full_name"),
                        rs.getString("username"),
                        rs.getString("email"),
                        rs.getString("password"),
                        rs.getString("role"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                    );
                }
            }
        }
        return null;
    }

    private void updatePassword(int userId, String hashedPassword) throws SQLException {
        String sql = "UPDATE users SET password = ? WHERE id = ?";
        try (java.sql.Connection conn = com.jvmart.config.MySQLConnection.getInstance();
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, hashedPassword);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        }
    }

    @FXML
    private void onBack() {
        SceneRouter.navigateTo("login.fxml");
    }

    private void show(Label label) {
        if (label != null) {
            label.setVisible(true);
            label.setManaged(true);
        }
    }
}
