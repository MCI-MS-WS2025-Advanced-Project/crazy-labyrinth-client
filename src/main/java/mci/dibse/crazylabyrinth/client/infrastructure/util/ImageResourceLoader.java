package mci.dibse.crazylabyrinth.client.infrastructure.util;

import java.io.InputStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import labyrinth.contracts.models.Direction;
import labyrinth.contracts.models.PlayerColor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ImageResourceLoader {

    private final static String POSTFIX = ".png";

    private static final String UP_DOWN = "I";
    private static final String UP_RIGHT = "L";
    private static final String UP_DOWN_LEFT = "T";

    private static final String TILE_IMAGES_PATH = "/tiles/";
    private static final Map<String, Image> imageTileCache = new HashMap<>();


    private static final String TREASURE_IMAGES_PATH = "/treasures/";
    private static final Map<String, Image> imageTreasureCache = new HashMap<>();

    private static final String PLAYERS_IMAGES_PATH = "/players/";
    private static final Map<String, Image> imagePlayerCache = new HashMap<>();

    public static Image loadImageForDirection(List<Direction> direction) {
        direction.sort(Comparator.comparingInt(Direction::ordinal));
        return switch (direction.size()) {
            case 2 -> {
                if (direction.contains(Direction.UP) && direction.contains(Direction.DOWN)) {
                    yield loadImage(UP_DOWN, IMAGE_TYPE.TILE);
                } else if (direction.contains(Direction.LEFT)
                        && direction.contains(Direction.RIGHT)) {
                    yield loadImage(UP_DOWN, IMAGE_TYPE.TILE);
                } else if ((direction.contains(Direction.UP)
                        && direction.contains(Direction.RIGHT))) {
                    yield loadImage(UP_RIGHT, IMAGE_TYPE.TILE);
                } else if (direction.contains(Direction.DOWN)
                        && direction.contains(Direction.LEFT)) {
                    yield loadImage(UP_RIGHT, IMAGE_TYPE.TILE);
                } else if (direction.contains(Direction.UP) && direction.contains(Direction.LEFT)) {
                    yield loadImage(UP_RIGHT, IMAGE_TYPE.TILE);
                } else if (direction.contains(Direction.DOWN)
                        && direction.contains(Direction.RIGHT)) {
                    yield loadImage(UP_RIGHT, IMAGE_TYPE.TILE);
                } else {
                    throw new IllegalStateException("Unexpected directions: " + direction);
                }
            }
            case 3 -> {
                if (direction.contains(Direction.UP)
                        && direction.contains(Direction.DOWN)
                        && direction.contains(Direction.LEFT)) {
                    yield loadImage(UP_DOWN_LEFT, IMAGE_TYPE.TILE);
                } else if (direction.contains(Direction.UP)
                        && direction.contains(Direction.DOWN)
                        && direction.contains(Direction.RIGHT)) {
                    yield loadImage(UP_DOWN_LEFT, IMAGE_TYPE.TILE);
                } else if (direction.contains(Direction.LEFT)
                        && direction.contains(Direction.RIGHT)
                        && direction.contains(Direction.UP)) {
                    yield loadImage(UP_DOWN_LEFT, IMAGE_TYPE.TILE);
                } else if (direction.contains(Direction.LEFT)
                        && direction.contains(Direction.RIGHT)
                        && direction.contains(Direction.DOWN)) {
                    yield loadImage(UP_DOWN_LEFT, IMAGE_TYPE.TILE);
                } else {
                    throw new IllegalStateException("Unexpected directions: " + direction);
                }
            }
            default -> throw new IllegalStateException("Unexpected value: " + direction.size());
        };
    }

    public static Number getRotationAngleForDirection(List<Direction> direction) {
        direction.sort(Comparator.comparingInt(Direction::ordinal));
        return switch (direction.size()) {
            case 2 -> {
                if (direction.contains(Direction.UP) && direction.contains(Direction.DOWN)) {
                    yield 0;
                } else if (direction.contains(Direction.LEFT)
                        && direction.contains(Direction.RIGHT)) {
                    yield 90;
                } else if ((direction.contains(Direction.UP)
                        && direction.contains(Direction.RIGHT))) {
                    yield 0;
                } else if (direction.contains(Direction.DOWN)
                        && direction.contains(Direction.LEFT)) {
                    yield 180;
                } else if (direction.contains(Direction.UP) && direction.contains(Direction.LEFT)) {
                    yield -90;
                } else if (direction.contains(Direction.DOWN)
                        && direction.contains(Direction.RIGHT)) {
                    yield 90;
                } else {
                    throw new IllegalStateException("Unexpected directions: " + direction);
                }
            }
            case 3 -> {
                if (direction.contains(Direction.UP)
                        && direction.contains(Direction.DOWN)
                        && direction.contains(Direction.LEFT)) {
                    yield 0;
                } else if (direction.contains(Direction.UP)
                        && direction.contains(Direction.DOWN)
                        && direction.contains(Direction.RIGHT)) {
                    yield 180;
                } else if (direction.contains(Direction.LEFT)
                        && direction.contains(Direction.RIGHT)
                        && direction.contains(Direction.UP)) {
                    yield 90;
                } else if (direction.contains(Direction.LEFT)
                        && direction.contains(Direction.RIGHT)
                        && direction.contains(Direction.DOWN)) {
                    yield -90;
                } else {
                    throw new IllegalStateException("Unexpected directions: " + direction);
                }
            }
            default -> throw new IllegalStateException("Unexpected value: " + direction.size());
        };
    }


    public static ImageView createImageViewAndRotate(Image image, double rotationAngle) {
        ImageView imageView = new ImageView(image);
        if (rotationAngle != 0) {
            imageView.setRotate(rotationAngle);
        }
        return imageView;
    }

    public static void clearCache() {
        imageTileCache.clear();
        log.debug("Image cache cleared");
    }

    public static Image loadPlayerImage(PlayerColor color) {
        return switch (color) {
            case RED -> loadImage("player_red", IMAGE_TYPE.PLAYER);
            case BLUE -> loadImage("player_blue", IMAGE_TYPE.PLAYER);
            case GREEN -> loadImage("player_green", IMAGE_TYPE.PLAYER);
            case YELLOW -> loadImage("player_yellow", IMAGE_TYPE.PLAYER);
        };
    }

    public static Image loadTreasureImage(int id) {
        String name = TreasureMapper.getTreasureName(id);
        return loadImage(name, IMAGE_TYPE.TREASURE);
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
        TILE, TREASURE, PLAYER
    }
}
