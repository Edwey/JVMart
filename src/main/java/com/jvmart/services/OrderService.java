package com.jvmart.services;

import com.jvmart.config.MySQLConnection;
import com.jvmart.dao.mongo.ActivityLogDAO;
import com.jvmart.dao.sql.OrderDAO;
import com.jvmart.dao.sql.ProductDAO;
import com.jvmart.dao.sql.UserDAO;
import com.jvmart.models.CartItem;
import com.jvmart.models.Order;
import com.jvmart.models.OrderItem;
import com.jvmart.models.Product;
import com.jvmart.session.SessionManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class OrderService {
    private final OrderDAO orderDAO = new OrderDAO();
    private final ProductDAO productDAO = new ProductDAO();
    private final UserDAO userDAO = new UserDAO();
    private final ActivityLogDAO activityLogDAO = new ActivityLogDAO();

    public ServiceResult<Integer> placeOrder() {
        SessionManager session = SessionManager.getInstance();
        List<CartItem> cart = session.getCart();

        if (cart.isEmpty()) {
            return new ServiceResult.Failure<>("Cart is empty");
        }

        try (Connection conn = MySQLConnection.getInstance()) {
            conn.setAutoCommit(false);
            try {
                List<OrderItem> items = cart.stream()
                        .map(cartItem -> validateAndBuildOrderItem(conn, cartItem))
                        .toList();

                double total = items.stream()
                        .mapToDouble(item -> item.quantity() * item.unitPrice())
                        .sum();

                Order order = new Order(
                        0,
                        session.getCurrentUser().getId(),
                        total,
                        "pending",
                        java.time.LocalDateTime.now(),
                        items
                );

                int orderId = orderDAO.save(conn, order);

                for (OrderItem item : items) {
                    Product lockedProduct = productDAO.findByIdForUpdate(conn, item.productId());
                    if (lockedProduct == null) {
                        throw new SQLException("Product #" + item.productId() + " no longer exists.");
                    }
                    productDAO.updateStock(conn, item.productId(), lockedProduct.getStock() - item.quantity());
                }

                conn.commit();

                Thread.startVirtualThread(() ->
                        activityLogDAO.log(session.getCurrentUser().getId(), "PLACE_ORDER",
                                "Order #" + orderId + " placed."));

                session.clearCart();
                return new ServiceResult.Success<>(orderId);
            } catch (RuntimeException e) {
                conn.rollback();
                Throwable cause = e.getCause();
                if (cause instanceof SQLException sqlException) {
                    return new ServiceResult.Failure<>("Failed to place order: " + sqlException.getMessage());
                }
                return new ServiceResult.Failure<>("Failed to place order: " + e.getMessage());
            } catch (SQLException e) {
                conn.rollback();
                return new ServiceResult.Failure<>("Failed to place order: " + e.getMessage());
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            return new ServiceResult.Failure<>("Failed to place order: " + e.getMessage());
        }
    }

    private OrderItem validateAndBuildOrderItem(Connection conn, CartItem cartItem) {
        try {
            Product lockedProduct = productDAO.findByIdForUpdate(conn, cartItem.getProduct().getId());
            if (lockedProduct == null) {
                throw new SQLException("Product \"" + cartItem.getProduct().getName() + "\" no longer exists.");
            }

            if (cartItem.getQuantity() > lockedProduct.getStock()) {
                throw new SQLException(
                        "Only " + lockedProduct.getStock() + " unit(s) of " + lockedProduct.getName() + " remain in stock."
                );
            }

            return new OrderItem(
                    0,
                    0,
                    lockedProduct.getId(),
                    lockedProduct.getName(),
                    cartItem.getQuantity(),
                    lockedProduct.getPrice()
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public ServiceResult<List<Order>> getOrdersForUser(int userId) {
        try {
            return new ServiceResult.Success<>(orderDAO.findByUserId(userId));
        } catch (SQLException e) {
            return new ServiceResult.Failure<>("Failed to retrieve orders: " + e.getMessage());
        }
    }

    public ServiceResult<List<Order>> getAllOrders() {
        try {
            return new ServiceResult.Success<>(orderDAO.findAll());
        } catch (SQLException e) {
            return new ServiceResult.Failure<>("Failed to retrieve all orders: " + e.getMessage());
        }
    }

    public ServiceResult<List<Order>> getRecentOrders(int days) {
        try {
            return new ServiceResult.Success<>(orderDAO.findRecent(days));
        } catch (SQLException e) {
            return new ServiceResult.Failure<>("Failed to retrieve recent orders: " + e.getMessage());
        }
    }

    public ServiceResult<Void> updateStatus(int orderId, String status) {
        try {
            orderDAO.updateStatus(orderId, status);
            return new ServiceResult.Success<>(null);
        } catch (SQLException e) {
            return new ServiceResult.Failure<>("Failed to update order status: " + e.getMessage());
        }
    }

    public ServiceResult<Map<String, Object>> getDashboardStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalRevenue", orderDAO.getTotalRevenue());
            stats.put("ordersToday", orderDAO.countToday());
            stats.put("lowStockCount", productDAO.countLowStock());
            stats.put("totalCustomers", userDAO.countByRole("customer"));
            stats.put("recentOrders", orderDAO.findRecent(10));
            return new ServiceResult.Success<>(stats);
        } catch (SQLException e) {
            return new ServiceResult.Failure<>("Failed to retrieve dashboard stats: " + e.getMessage());
        }
    }

    public ServiceResult<Double> getTotalRevenue() {
        try {
            return new ServiceResult.Success<>(orderDAO.getTotalRevenue());
        } catch (SQLException e) {
            return new ServiceResult.Failure<>("Failed to retrieve total revenue: " + e.getMessage());
        }
    }

    public ServiceResult<Integer> getPendingCount() {
        try {
            return new ServiceResult.Success<>(orderDAO.countPending());
        } catch (SQLException e) {
            return new ServiceResult.Failure<>("Failed to retrieve pending orders: " + e.getMessage());
        }
    }

    public ServiceResult<Double> getAverageOrderValue() {
        try {
            return new ServiceResult.Success<>(orderDAO.getAverageOrderValue());
        } catch (SQLException e) {
            return new ServiceResult.Failure<>("Failed to retrieve average order value: " + e.getMessage());
        }
    }

    public ServiceResult<Map<Integer, Integer>> getOrderCounts() {
        try {
            return new ServiceResult.Success<>(orderDAO.getOrderCounts());
        } catch (SQLException e) {
            return new ServiceResult.Failure<>("Failed to retrieve order counts: " + e.getMessage());
        }
    }

    public ServiceResult<Map<Integer, String>> getCustomerNames() {
        try {
            Map<Integer, String> names = new HashMap<>();
            for (var user : userDAO.findAll()) {
                names.put(user.getId(), user.getFullName());
            }
            return new ServiceResult.Success<>(names);
        } catch (SQLException e) {
            return new ServiceResult.Failure<>("Failed to retrieve customer names: " + e.getMessage());
        }
    }
}
