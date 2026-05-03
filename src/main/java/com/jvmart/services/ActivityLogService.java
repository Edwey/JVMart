package com.jvmart.services;

import com.jvmart.dao.mongo.ActivityLogDAO;
import com.jvmart.models.User;
import com.jvmart.session.SessionManager;

public class ActivityLogService {
    private final ActivityLogDAO activityLogDAO = new ActivityLogDAO();

    public void logCurrentUser(String action, String detail) {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }
        log(user.getId(), action, detail);
    }

    public void log(int userId, String action, String detail) {
        Thread.startVirtualThread(() -> activityLogDAO.log(userId, action, detail));
    }
}
