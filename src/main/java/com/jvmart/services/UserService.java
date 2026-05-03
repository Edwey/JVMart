package com.jvmart.services;

import com.jvmart.dao.sql.UserDAO;
import com.jvmart.models.User;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.SQLException;
import java.util.List;

public class UserService {
    private final UserDAO userDAO = new UserDAO();

    public ServiceResult<Boolean> register(String fullName, String username, String email, String password) {
        try {
            return switch (userDAO.findByUsername(username)) {
                case null -> {
                    User user = new User(0, fullName, username, email, password, "customer", java.time.LocalDateTime.now());
                    userDAO.save(user);
                    yield new ServiceResult.Success<>(true);
                }
                case User existingUser -> new ServiceResult.Failure<>("Username already exists");
            };
        } catch (SQLException e) {
            return new ServiceResult.Failure<>("Registration failed: " + e.getMessage());
        }
    }

    public ServiceResult<User> login(String username, String password) {
        try {
            User user = userDAO.findByUsername(username);
            if (user != null && BCrypt.checkpw(password, user.getPassword())) {
                return new ServiceResult.Success<>(user);
            }
            return new ServiceResult.Failure<>("Invalid username or password");
        } catch (SQLException e) {
            return new ServiceResult.Failure<>("Login failed: " + e.getMessage());
        }
    }

    public ServiceResult<List<User>> getAllCustomers() {
        try {
            List<User> customers = userDAO.findAll().stream()
                    .filter(u -> "customer".equals(u.getRole()))
                    .toList();
            return new ServiceResult.Success<>(customers);
        } catch (SQLException e) {
            return new ServiceResult.Failure<>("Failed to retrieve customers: " + e.getMessage());
        }
    }
}
