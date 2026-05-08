package com.jvmart.utils;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/**
 * Global refresh utility that provides consistent refresh functionality
 * across all user interfaces (admin, customer, product catalog, etc.)
 */
public class GlobalRefresh {
    
    /**
     * Register refresh functionality for the current scene
     * @param scene The scene to register refresh for
     */
    public static void registerRefreshHandler(Scene scene) {
        if (scene == null) return;
        
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (KeyCode.F5.equals(event.getCode())) {
                Platform.runLater(() -> {
                    // Dispatch refresh event to all controllers
                    scene.getRoot().fireEvent(
                        new RefreshEvent(RefreshEvent.Type.GLOBAL_REFRESH)
                    );
                });
                event.consume();
            }
        });
    }
    
    /**
     * Custom refresh event for global refresh handling
     */
    public static class RefreshEvent extends javafx.event.Event {
        private final Type type;
        
        public RefreshEvent(Type type) {
            super(ANY);
            this.type = type;
        }
        
        public Type getType() { return type; }
        
        public enum Type {
            GLOBAL_REFRESH,
            DASHBOARD_REFRESH,
            REVIEWS_REFRESH,
            INVENTORY_REFRESH,
            CATALOG_REFRESH
        }
    }
    
    /**
     * Interface for refreshable components
     */
    public interface Refreshable {
        void refresh();
    }
    
    /**
     * Find and refresh all refreshable components in the scene
     * @param scene The scene to search for refreshable components
     */
    public static void refreshAllRefreshableComponents(Scene scene) {
        if (scene == null) return;
        
        refreshRecursive(scene.getRoot());
    }
    
    /**
     * Recursively search for refreshable components and trigger refresh
     * @param node The node to search (and its children)
     */
    private static void refreshRecursive(javafx.scene.Node node) {
        if (node == null) return;
        
        // If the node itself is refreshable, refresh it
        if (node instanceof Refreshable) {
            ((Refreshable) node).refresh();
        }
        
        // Recursively check children
        if (node instanceof javafx.scene.Parent parent) {
            for (javafx.scene.Node child : parent.getChildrenUnmodifiable()) {
                refreshRecursive(child);
            }
        }
    }
}
