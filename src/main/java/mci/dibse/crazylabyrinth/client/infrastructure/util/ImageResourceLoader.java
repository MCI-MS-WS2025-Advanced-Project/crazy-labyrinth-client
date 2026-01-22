package mci.dibse.crazylabyrinth.client.infrastructure.util;

import java.io.InputStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import labyrinth.contracts.models.BonusType;
import labyrinth.contracts.models.Direction;
import labyrinth.contracts.models.PlayerColor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ImageResourceLoader {

    private final static String POSTFIX = ".png";


    private static final String TILE_IMAGES_PATH = "/tiles/";
    private static final Map<String, Image> imageTileCache = new HashMap<>();


    private static final String TREASURE_IMAGES_PATH = "/treasures/";
    private static final Map<String, Image> imageTreasureCache = new HashMap<>();

    private static final String PLAYERS_IMAGES_PATH = "/players/";
    private static final Map<String, Image> imagePlayerCache = new HashMap<>();

    private static final String HOME_IMAGES_PATH = "/homes/";
    private static final Map<String, Image> imageHomeCache = new HashMap<>();

    private static final String BONI_IMAGES_PATH = "/boni/";
    private static final Map<String, Image> imageBoniCache = new HashMap<>();



    public static ImageView createImageViewAndRotate(Image image, double rotationAngle) {
        ImageView imageView = new ImageView(image);
        if (rotationAngle != 0) {
            imageView.setRotate(rotationAngle);
        }
        return imageView;
    }



    public static Image loadImage(PlayerColor color) {
        String name = FileNameMapper.loadPlayerName(color);
       return loadImage(name, IMAGE_TYPE.PLAYER);
    }

    public static Image loadImage(int id) {
        String name = FileNameMapper.getTreasureName(id);
        return loadImage(name, IMAGE_TYPE.TREASURE);
    }

    public static Image loadImage(List<Direction> direction) {
        String name = FileNameMapper.loadImageNameForDirection(direction);
        return loadImage(name, IMAGE_TYPE.TILE);
    }

    public static Image loadImage(BonusType bonusType) {
        String name = FileNameMapper.getBoniName(bonusType);
        return loadImage(name, IMAGE_TYPE.BONI);
    }


    /**
     * Load image from resources with caching
     */
    private static Image loadImage(String imageFileName, IMAGE_TYPE type) {
        imageFileName += POSTFIX;
        Map<String, Image> cache = null;
        String resourcePath;
        switch (type) {
            case TREASURE -> {
                cache = imageTreasureCache;
                resourcePath = TREASURE_IMAGES_PATH;
            }
            case TILE -> {
                cache = imageTileCache;
                resourcePath = TILE_IMAGES_PATH;
            }
            case PLAYER -> {
                cache = imagePlayerCache;
                resourcePath = PLAYERS_IMAGES_PATH;
            }
            case BONI -> {
                cache = imageBoniCache;
                resourcePath = BONI_IMAGES_PATH;
            }
            default -> throw new IllegalStateException("Unexpected value: " + type);
        }

        String finalImageFileName = imageFileName;
        Image image = cache.computeIfAbsent(
                imageFileName,
                path -> {
                    path = resourcePath + finalImageFileName;
                    try (InputStream inputStream =
                                 ImageResourceLoader.class.getResourceAsStream(path)) {
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
       // return image;
        return copyImage(image);
    }

    /**
     * Creates a deep copy of the image pixels into a new WritableImage.
     * Note: The original image must be fully loaded for getPixelReader() to work.
     */
    public static Image copyImage(Image original) {
        if (original == null) {
            return null;
        }
        javafx.scene.image.PixelReader pixelReader = original.getPixelReader();
        if (pixelReader == null) {
            // Image is not readable or not yet loaded
            return null;
        }
        int width = (int) original.getWidth();
        int height = (int) original.getHeight();
        return new javafx.scene.image.WritableImage(pixelReader, width, height);
    }


    private enum IMAGE_TYPE {
        TILE, TREASURE, PLAYER, BONI
    }
}
