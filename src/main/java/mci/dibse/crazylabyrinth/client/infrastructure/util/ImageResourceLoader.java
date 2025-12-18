package mci.dibse.crazylabyrinth.client.infrastructure.util;

import java.io.InputStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import labyrinth.contracts.models.Direction;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ImageResourceLoader {

    private static final String UP_DOWN = "up_down_512px.png";
    private static final String UP_RIGHT = "up_right_512px.png";
    private static final String UP_DOWN_LEFT = "up_down_left_512px.png";

    private static final String TILE_IMAGES_PATH = "/tiles/";
    private static final Map<String, Image> imageCache = new HashMap<>();

    public static Image loadImageForDirection(List<Direction> direction) {
        direction.sort(Comparator.comparingInt(Direction::ordinal));
        return switch (direction.size()) {
            case 2 -> {
                if (direction.contains(Direction.UP) && direction.contains(Direction.DOWN)) {
                    yield loadImage(UP_DOWN);
                } else if (direction.contains(Direction.LEFT)
                        && direction.contains(Direction.RIGHT)) {
                    yield loadImage(UP_DOWN);
                } else if ((direction.contains(Direction.UP)
                        && direction.contains(Direction.RIGHT))) {
                    yield loadImage(UP_RIGHT);
                } else if (direction.contains(Direction.DOWN)
                        && direction.contains(Direction.LEFT)) {
                    yield loadImage(UP_RIGHT);
                } else if (direction.contains(Direction.UP) && direction.contains(Direction.LEFT)) {
                    yield loadImage(UP_RIGHT);
                } else if (direction.contains(Direction.DOWN)
                        && direction.contains(Direction.RIGHT)) {
                    yield loadImage(UP_RIGHT);
                } else {
                    throw new IllegalStateException("Unexpected directions: " + direction);
                }
            }
            case 3 -> {
                if (direction.contains(Direction.UP)
                        && direction.contains(Direction.DOWN)
                        && direction.contains(Direction.LEFT)) {
                    yield loadImage(UP_DOWN_LEFT);
                } else if (direction.contains(Direction.UP)
                        && direction.contains(Direction.DOWN)
                        && direction.contains(Direction.RIGHT)) {
                    yield loadImage(UP_DOWN_LEFT);
                } else if (direction.contains(Direction.LEFT)
                        && direction.contains(Direction.RIGHT)
                        && direction.contains(Direction.UP)) {
                    yield loadImage(UP_DOWN_LEFT);
                } else if (direction.contains(Direction.LEFT)
                        && direction.contains(Direction.RIGHT)
                        && direction.contains(Direction.DOWN)) {
                    yield loadImage(UP_DOWN_LEFT);
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

    /*
        public static ImageView mapDirectionsToTile(List<Direction> direction) {
            direction.sort(Comparator.comparingInt(Direction::ordinal));
            return  switch (direction.size()) {
               case 2 -> {
                   if (direction.contains(Direction.UP) && direction.contains(Direction.DOWN)) {
                       // straight_vertical.png
                       Image image = loadImage(UP_DOWN);
                       yield createImageViewAndRotate(image, 0);
                   } else if (direction.contains(Direction.LEFT) && direction.contains(Direction.RIGHT)) {
                       // rotate --> straight_vertical.png + 90 C°
                       Image image = loadImage(UP_DOWN);
                       yield createImageViewAndRotate(image, 90);
                   } else if ((direction.contains(Direction.UP) && direction.contains(Direction.RIGHT))) {
                       // up_right.png
                       Image image = loadImage(UP_RIGHT);
                       yield createImageViewAndRotate(image, 0);
                   } else if (direction.contains(Direction.DOWN) && direction.contains(Direction.LEFT)) {
                       // rotate -->up_right.png + 180 C°
                       Image image = loadImage(UP_RIGHT);
                       yield createImageViewAndRotate(image, 180);
                   } else if (direction.contains(Direction.UP) && direction.contains(Direction.LEFT)) {
                       // rotate -->  up_right.png - 90 C°
                       Image image = loadImage(UP_RIGHT);
                       yield createImageViewAndRotate(image, -90);
                   } else if (direction.contains(Direction.DOWN) && direction.contains(Direction.RIGHT)) {
                       // rotate --> up_right.png + 90 C°
                       Image image = loadImage(UP_RIGHT);
                       yield createImageViewAndRotate(image, 90);
                   } else {
                       throw new IllegalStateException("Unexpected directions: " + direction);
                   }
               }
               case 3 -> {
                   if (direction.contains(Direction.UP) && direction.contains(Direction.DOWN) && direction.contains(Direction.LEFT)) {
                       // up_down_left.png
                       Image image = loadImage(UP_DOWN_LEFT);
                       yield createImageViewAndRotate(image, 0);
                   } else if (direction.contains(Direction.UP) && direction.contains(Direction.DOWN) && direction.contains(Direction.RIGHT)) {
                       // rotate up_down_left.png + 180 C°
                       Image image = loadImage(UP_DOWN_LEFT);
                       yield createImageViewAndRotate(image, 180);
                   } else if (direction.contains(Direction.LEFT) && direction.contains(Direction.RIGHT) && direction.contains(Direction.UP)) {
                       // rotate up_down_left.png + 90 C°
                       Image image = loadImage(UP_DOWN_LEFT);
                       yield createImageViewAndRotate(image, 90);
                   } else if (direction.contains(Direction.LEFT) && direction.contains(Direction.RIGHT) && direction.contains(Direction.DOWN)) {
                       // rotate up_down_left.png - 90 C°
                       Image image = loadImage(UP_DOWN_LEFT);
                       yield createImageViewAndRotate(image, -90);
                   } else {
                       throw new IllegalStateException("Unexpected directions: " + direction);
                   }
               }
               default -> throw new IllegalStateException("Unexpected value: " + direction.size());
           };
        }
    */
    /**
     * Load image from resources with caching
     */
    private static Image loadImage(String imageFileName) {
        return imageCache.computeIfAbsent(
                imageFileName,
                path -> {
                    path = TILE_IMAGES_PATH + imageFileName;
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
    }

    public static ImageView createImageViewAndRotate(Image image, double rotationAngle) {
        ImageView imageView = new ImageView(image);
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
