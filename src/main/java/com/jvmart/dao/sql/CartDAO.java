package com.jvmart.dao.sql;

import com.jvmart.config.MySQLConnection;
import com.jvmart.models.CartItem;
import com.jvmart.models.Product;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Persists cart lines in MySQL. Uses replace-by-user transactional sync.
 */
public class CartDAO {

    public List<CartItem> loadCartForUser(int userId) throws SQLException {
        String sql = """
                SELECT p.id, p.name, p.description, p.price, p.stock, p.category, p.image_path,
                       c.quantity
                FROM cart c
                JOIN products p ON p.id = c.product_id
                WHERE c.user_id = ?
                """;
        try (Connection conn = MySQLConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                List<CartItem> rows = new ArrayList<>();
                while (rs.next()) {
                    Product p = new Product(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("description"),
                            rs.getDouble("price"),
                            rs.getInt("stock"),
                            rs.getString("category"),
                            rs.getString("image_path"));
                    rows.add(new CartItem(p, rs.getInt("quantity")));
                }
                return rows;
            }
        }
    }

    /** Replaces all rows for the user with the current session snapshot. */
    public void syncCart(int userId, List<CartItem> items) throws SQLException {
        try (Connection conn = MySQLConnection.getInstance()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement del = conn.prepareStatement("DELETE FROM cart WHERE user_id = ?")) {
                    del.setInt(1, userId);
                    del.executeUpdate();
                }
                if (items.isEmpty()) {
                    conn.commit();
                    return;
                }
                String ins = """
                        INSERT INTO cart (user_id, product_id, quantity)
                        VALUES (?, ?, ?)
                        """;
                try (PreparedStatement stmt = conn.prepareStatement(ins)) {
                    for (CartItem ci : items) {
                        stmt.setInt(1, userId);
                        stmt.setInt(2, ci.getProduct().getId());
                        stmt.setInt(3, ci.getQuantity());
                        stmt.executeUpdate();
                    }
                }
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }


    /** Clears persisted cart using an existing transactional connection (e.g. after checkout). */
    public void clearCartForUser(Connection conn, int userId) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM cart WHERE user_id = ?")) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
        }
    }

    public void clearCartForUser(int userId) throws SQLException {
        try (Connection conn = MySQLConnection.getInstance()) {
            clearCartForUser(conn, userId);
        }
    }
}
