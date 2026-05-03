package com.jvmart.dao.sql;

import com.jvmart.config.MySQLConnection;
import com.jvmart.models.Order;
import com.jvmart.models.OrderItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrderDAO {
    public int save(Order order) throws SQLException {
        try (Connection conn = MySQLConnection.getInstance()) {
            conn.setAutoCommit(false);
            try {
                int orderId = save(conn, order);
                conn.commit();
                return orderId;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public int save(Connection conn, Order order) throws SQLException {
        String orderSql = """
                INSERT INTO orders (user_id, total, status)
                VALUES (?, ?, ?)
                """;
        String itemSql = """
                INSERT INTO order_items (order_id, product_id, quantity, unit_price)
                VALUES (?, ?, ?, ?)
                """;

        int orderId = 0;
        try (PreparedStatement orderStmt = conn.prepareStatement(orderSql, Statement.RETURN_GENERATED_KEYS)) {
            orderStmt.setInt(1, order.getUserId());
            orderStmt.setDouble(2, order.getTotal());
            orderStmt.setString(3, order.getStatus());
            orderStmt.executeUpdate();

            try (ResultSet orderRs = orderStmt.getGeneratedKeys()) {
                if (orderRs.next()) {
                    orderId = orderRs.getInt(1);

                    try (PreparedStatement itemStmt = conn.prepareStatement(itemSql)) {
                        for (OrderItem item : order.getItems()) {
                            itemStmt.setInt(1, orderId);
                            itemStmt.setInt(2, item.productId());
                            itemStmt.setInt(3, item.quantity());
                            itemStmt.setDouble(4, item.unitPrice());
                            itemStmt.addBatch();
                        }
                        itemStmt.executeBatch();
                    }
                }
            }
        }
        return orderId;
    }

    public List<Order> findByUserId(int userId) throws SQLException {
        List<Order> orders = new ArrayList<>();
        String sql = """
                SELECT o.id, o.user_id, o.total, o.status, o.created_at
                FROM orders o
                WHERE o.user_id = ?
                ORDER BY o.created_at DESC
                """;
        try (Connection conn = MySQLConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    orders.add(mapResultSetToOrder(rs));
                }
            }
        }
        return orders;
    }

    public List<Order> findAll() throws SQLException {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT id, user_id, total, status, created_at FROM orders ORDER BY created_at DESC";
        try (Connection conn = MySQLConnection.getInstance();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                orders.add(mapResultSetToOrder(rs));
            }
        }
        return orders;
    }

    public List<Order> findRecent(int days) throws SQLException {
        List<Order> orders = new ArrayList<>();
        String sql = """
                SELECT o.id, o.user_id, o.total, o.status, o.created_at
                FROM orders o
                WHERE o.created_at >= DATE_SUB(CURDATE(), INTERVAL ? DAY)
                ORDER BY o.created_at DESC
                """;
        try (Connection conn = MySQLConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, days);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    orders.add(mapResultSetToOrder(rs));
                }
            }
        }
        return orders;
    }

    public void updateStatus(int orderId, String status) throws SQLException {
        String sql = "UPDATE orders SET status = ? WHERE id = ?";
        try (Connection conn = MySQLConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, orderId);
            stmt.executeUpdate();
        }
    }

    public double getTotalRevenue() throws SQLException {
        String sql = "SELECT COALESCE(SUM(total), 0) FROM orders";
        try (Connection conn = MySQLConnection.getInstance();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getDouble(1);
            }
        }
        return 0.0;
    }

    public int countToday() throws SQLException {
        String sql = "SELECT COUNT(*) FROM orders WHERE DATE(created_at) = CURRENT_DATE";
        try (Connection conn = MySQLConnection.getInstance();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    public int countPending() throws SQLException {
        String sql = "SELECT COUNT(*) FROM orders WHERE status = 'pending'";
        try (Connection conn = MySQLConnection.getInstance();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    public double getAverageOrderValue() throws SQLException {
        String sql = "SELECT COALESCE(AVG(total), 0) FROM orders";
        try (Connection conn = MySQLConnection.getInstance();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getDouble(1);
            }
        }
        return 0.0;
    }

    public java.util.Map<Integer, Integer> getOrderCounts() throws SQLException {
        java.util.Map<Integer, Integer> counts = new java.util.HashMap<>();
        String sql = "SELECT user_id, COUNT(*) AS cnt FROM orders GROUP BY user_id";
        try (Connection conn = MySQLConnection.getInstance();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                counts.put(rs.getInt("user_id"), rs.getInt("cnt"));
            }
        }
        return counts;
    }

    private Order mapResultSetToOrder(ResultSet rs) throws SQLException {
        return new Order(
            rs.getInt("id"),
            rs.getInt("user_id"),
            rs.getDouble("total"),
            rs.getString("status"),
            rs.getTimestamp("created_at").toLocalDateTime(),
            new ArrayList<>()
        );
    }
}
