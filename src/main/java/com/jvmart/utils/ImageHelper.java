package com.jvmart.utils;

import javafx.scene.image.Image;

import java.io.File;
import java.util.logging.Logger;

public final class ImageHelper {
    private static final Logger LOGGER = Logger.getLogger(ImageHelper.class.getName());
    private ImageHelper() {}

    public static Image loadProductImage(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return null;
        }

        try {
            if (imagePath.startsWith("http://") || imagePath.startsWith("https://") || imagePath.startsWith("file:/")) {
                return new Image(imagePath, true);
            }

            File file = new File(imagePath);
            if (file.exists()) {
                return new Image(file.toURI().toString(), true);
            }

            var resource = ImageHelper.class.getResource(imagePath.startsWith("/") ? imagePath : "/" + imagePath);
            if (resource != null) {
                return new Image(resource.toExternalForm(), true);
            }
        } catch (IllegalArgumentException e) {
            LOGGER.fine("Failed to load image from path: " + imagePath + " - " + e.getMessage());
            return null;
        }

        return null;
    }
}
