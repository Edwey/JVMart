package com.jvmart.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

public class MongoConnection {
    private static final String URI = "mongodb://localhost:27017";
    private static final String DATABASE_NAME = "jvmart";

    private static volatile MongoClient mongoClient;
    private static volatile MongoDatabase database;
    private static final Object lock = new Object();

    private MongoConnection() {}

    public static MongoDatabase getDatabase() {
        if (mongoClient == null) {
            synchronized (lock) {
                if (mongoClient == null) {
                    mongoClient = MongoClients.create(URI);
                    database = mongoClient.getDatabase(DATABASE_NAME);
                    // Register shutdown hook to close client on JVM exit
                    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                        if (mongoClient != null) {
                            mongoClient.close();
                        }
                    }));
                }
            }
        }
        return database;
    }

    public static void close() {
        synchronized (lock) {
            if (mongoClient != null) {
                mongoClient.close();
                mongoClient = null;
                database = null;
            }
        }
    }
}
