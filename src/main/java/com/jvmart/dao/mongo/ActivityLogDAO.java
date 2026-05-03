package com.jvmart.dao.mongo;

import com.jvmart.config.MongoConnection;
import org.bson.Document;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ActivityLogDAO {
    private static final String COLLECTION_NAME = "activity_logs";
    private static final Logger LOGGER = Logger.getLogger(ActivityLogDAO.class.getName());

    public void log(int userId, String action, String detail) {
        try {
            var collection = MongoConnection.getDatabase().getCollection(COLLECTION_NAME);
            var doc = new org.bson.Document()
                    .append("userId", userId)
                    .append("action", action)
                    .append("detail", detail)
                    .append("timestamp", new java.util.Date());
            collection.insertOne(doc);
        } catch (Exception e) {
            // Log to stderr as fallback - don't let logging failures crash the app
            LOGGER.log(Level.WARNING, "Failed to log activity: {0} - {1}", new Object[]{action, e.getMessage()});
        }
    }
}
