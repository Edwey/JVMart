package com.jvmart.session;

import com.jvmart.models.CartItem;
import com.jvmart.models.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;

public class SessionManager {
    private static volatile SessionManager instance;
    private User currentUser;
    private final ObservableList<CartItem> cart = FXCollections.observableArrayList();
    private List<CartItem> lastCartSnapshot = List.of();

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

    public void logout() {
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
        cart.removeIf(item -> item.getProduct().getId() == productId);
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
