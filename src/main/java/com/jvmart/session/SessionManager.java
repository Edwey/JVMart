package com.jvmart.session;

import com.jvmart.dao.sql.CartDAO;
import com.jvmart.models.CartItem;
import com.jvmart.models.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SessionManager {
    private static final Logger LOGGER = Logger.getLogger(SessionManager.class.getName());

    private static volatile SessionManager instance;
    private User currentUser;
    private final ObservableList<CartItem> cart = FXCollections.observableArrayList();
    private List<CartItem> lastCartSnapshot = List.of();

    private final CartDAO cartDAO = new CartDAO();

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            synchronized (SessionManager.class) {
                if (instance == null) {
                    instance = new SessionManager();
                }
            }
        }
        return instance;
    }

    public void login(User user) {
        this.currentUser = user;
    }

    /** Replaces in-memory cart (e.g. after loading from DB). Does not alter last-order snapshot. */
    public void replaceCart(java.util.Collection<CartItem> items) {
        cart.clear();
        cart.addAll(new ArrayList<>(items));
    }

    public void logout() {
        User u = this.currentUser;
        if (u != null) {
            try {
                cartDAO.syncCart(u.getId(), List.copyOf(cart));
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Could not persist cart on logout", e);
            }
        }
        this.currentUser = null;
        clearCart();
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public ObservableList<CartItem> getCart() {
        return cart;
    }

    public void addToCart(CartItem item) {
        for (CartItem ci : cart) {
            if (ci.getProduct().getId() == item.getProduct().getId()) {
                ci.setQuantity(ci.getQuantity() + item.getQuantity());
                return;
            }
        }
        cart.add(item);
    }

    public void removeFromCart(int productId) {
        cart.removeIf(entry -> entry.getProduct().getId() == productId);
    }

    public void updateCartQuantity(int productId, int qty) {
        for (CartItem item : cart) {
            if (item.getProduct().getId() == productId) {
                item.setQuantity(qty);
                return;
            }
        }
    }

    public void clearCart() {
        lastCartSnapshot = List.copyOf(cart);
        cart.clear();
    }

    public double getCartTotal() {
        return cart.stream().mapToDouble(CartItem::getLineTotal).sum();
    }

    public int getCartCount() {
        return cart.stream().mapToInt(CartItem::getQuantity).sum();
    }

    public List<CartItem> getLastCartSnapshot() {
        return List.copyOf(lastCartSnapshot);
    }
}
