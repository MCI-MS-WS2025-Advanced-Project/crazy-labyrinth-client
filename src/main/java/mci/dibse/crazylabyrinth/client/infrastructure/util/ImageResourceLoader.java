package mci.dibse.crazylabyrinth.client.infrastructure.util;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;


@Slf4j
public class ImageResourceLoader {


    private static final String TILE_IMAGES_PATH = "/images/tiles/";
    private static final Map<String, Image> imageCache = new HashMap<>();

    /**
     * Load image from resources with caching
     */
    public static Image loadImage(String resourcePath) {
        return imageCache.computeIfAbsent(resourcePath, path -> {
            try (InputStream inputStream = ImageResourceLoader.class.getResourceAsStream(path)) {
                if (inputStream == null) {
                    log.error("Resource not found: {}", path);
                    return null;
                }
                log.debug("Loading image: {}", path);
                return new Image(inputStream);
            } catch (Exception e) {
                log.error("Failed to load image: {}", path, e);
                return null;
            }
        });
    }


    public static ImageView createTileImageView(String tileName) {
        String fullPath = TILE_IMAGES_PATH + tileName;
        Image image = loadImage(fullPath);

        if (image == null) {
            log.warn("Using fallback for missing tile: {}", tileName);
            // Return a placeholder or default tile
            image = loadImage(TILE_IMAGES_PATH + "default_tile.png");
        }

        return new ImageView(image);
    }

    public static ImageView createRotatedTileImageView(String tileName, double rotationAngle) {
        ImageView imageView = createTileImageView(tileName);
        if (rotationAngle != 0) {
            imageView.setRotate(rotationAngle);
        }
        return imageView;
    }

    public static void clearCache() {
        imageCache.clear();
        log.debug("Image cache cleared");
    }





}
