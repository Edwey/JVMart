package com.jvmart.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MySQLConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/jvmart";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    private MySQLConnection() {}

    public static Connection getInstance() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
