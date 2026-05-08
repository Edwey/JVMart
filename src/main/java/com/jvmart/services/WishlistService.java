package com.jvmart.services;

import com.jvmart.dao.sql.ProductDAO;
import com.jvmart.dao.sql.WishlistDAO;
import com.jvmart.models.CartItem;
import com.jvmart.models.Product;
import com.jvmart.models.WishlistItem;
import com.jvmart.session.SessionManager;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Persisted wishlist in MySQL ({@link WishlistDAO}).
 */
public final class WishlistService {

    private static final Logger LOGGER = Logger.getLogger(WishlistService.class.getName());
    private static volatile WishlistService instance;

    private final WishlistDAO wishlistDAO = new WishlistDAO();
    private final ProductDAO productDAO = new ProductDAO();

    private WishlistService() {}

    public static WishlistService getInstance() {
        if (instance == null) {
            synchronized (WishlistService.class) {
                if (instance == null) {
                    instance = new WishlistService();
                }
            }
        }
        return instance;
    }

    private int requireUserId() throws SQLException {
        var u = SessionManager.getInstance().getCurrentUser();
        if (u == null) {
            throw new SQLException("Not logged in.");
        }
        return u.getId();
    }

    public List<WishlistItem> getWishlistItems() {
        try {
            int userId = requireUserId();
            List<Integer> ids = wishlistDAO.listProductIds(userId);
            List<WishlistItem> items = new ArrayList<>();
            for (int pid : ids) {
                Product p = productDAO.findById(pid);
                if (p != null) {
                    items.add(WishlistItem.fromProduct(p));
                }
            }
            return items;
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Wishlist load failed", e);
            return List.of();
        }
    }

    /** @return {@code true} if the product was newly added */
    public boolean addToWishlist(Product product) throws SQLException {
        int userId = requireUserId();
        if (wishlistDAO.exists(userId, product.getId())) {
            return false;
        }
        wishlistDAO.add(userId, product.getId());
        return true;
    }

    public boolean removeFromWishlist(int productId) throws SQLException {
        return wishlistDAO.remove(requireUserId(), productId);
    }

    public void clearWishlist() throws SQLException {
        wishlistDAO.clearForUser(requireUserId());
    }

    /** Adds first available product snapshot to cart and removes wishlist row. */
    public boolean moveToCart(int productId) throws SQLException {
        int userId = requireUserId();
        Product product = productDAO.findById(productId);
        if (product == null) {
            return false;
        }
        SessionManager.getInstance().addToCart(new CartItem(product, 1));
        wishlistDAO.remove(userId, productId);
        return true;
    }

    public boolean isInWishlist(int productId) {
        try {
            return wishlistDAO.exists(requireUserId(), productId);
        } catch (SQLException e) {
            return false;
        }
    }

    public int addCartItemsToWishlist(List<CartItem> cartItems) throws SQLException {
        int userId = requireUserId();
        int added = 0;
        for (CartItem ci : cartItems) {
            if (!wishlistDAO.exists(userId, ci.getProduct().getId())) {
                wishlistDAO.add(userId, ci.getProduct().getId());
                added++;
            }
        }
        return added;
    }
}
