package mci.dibse.crazylabyrinth.client.infrastructure.util;


import labyrinth.contracts.models.Direction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.*;


@Slf4j
public class ImageResourceLoader {


    private final String UP_DOWN = "up_down_512px.png";
    private final String UP_RIGHT = "up_left_512px.png";
    private final String UP_DOWN_LEFT = "up_down_left_512px";

    private static final String TILE_IMAGES_PATH = "/tiles/";
    private static final Map<String, Image> imageCache = new HashMap<>();




    public static Image mapDirectionsToTile(Direction[] direction) {
        List<Direction> l = new ArrayList<>(Arrays.asList(direction));
        l.sort(Comparator.comparingInt(Direction::ordinal));
        switch (l.size()) {
            case 2 -> {
                if (l.contains(Direction.UP) && l.contains(Direction.DOWN)) {
                    // straight_vertical.png
                     return loadImage();
                } else if (l.contains(Direction.LEFT) && l.contains(Direction.RIGHT)) {
                    // rotate --> straight_vertical.png + 90 C°
                }  else if ((l.contains(Direction.UP) && l.contains(Direction.RIGHT)) {
                    // up_left.png
                } else if (l.contains(Direction.DOWN) && l.contains(Direction.LEFT)) {
                    // rotate --> up_left.png + 180 C°
                } else if (l.contains(Direction.UP) && l.contains(Direction.LEFT)) {
                    // rotate -->  up_left.png - 90 C°
                } else if (l.contains(Direction.DOWN) && l.contains(Direction.RIGHT))) {
                    // rotate --> up_left.png + 90 C°
                }

            }   case 3 -> {
                if (l.contains(Direction.UP) && l.contains(Direction.DOWN) && l.contains(Direction.LEFT)) {
                    // up_down_left.png
                } else if (l.contains(Direction.UP) && l.contains(Direction.DOWN) && l.contains(Direction.RIGHT)) {
                    // rotate up_down_left.png + 180 C°
                } else if (l.contains(Direction.LEFT) && l.contains(Direction.RIGHT) && l.contains(Direction.UP)) {
                    // rotate up_down_left.png + 90 C°
                } else if (l.contains(Direction.LEFT) && l.contains(Direction.RIGHT) && l.contains(Direction.DOWN)) {
                    // rotate up_down_left.png - 90 C°
                }
            }
        }

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


        private Image loadAndRotate()
/*
        public static ImageView createTileImageView(String baseTilePath, double rotationAngle) {
            Image image = new Image(baseTilePath);
            ImageView imageView = new ImageView(image);
            if (rotationAngle != 0) {
                imageView.setRotate(rotationAngle);
            }
            return imageView;
        }


        public static ImageView createImageViewFromResources(String resourcePath) {
            URL resourceUrl = getClass().getResource(resourcePath);
            if (resourceUrl == null) {
                throw new IllegalArgumentException("Resource not found: " + resourcePath);
            }

            Image image = new Image(resourceUrl.toExternalForm());
            return new ImageView(image);
        }

 */

}

// Java
try (var in = getClass().getResourceAsStream("/tiles/up_down_512px.png")) {
        javafx.scene.image.Image img = new javafx.scene.image.Image(in);
        extraTile.setImage(img);
    }
/*
// Java
  extraTile.imageProperty().bind(viewModelImageProperty); // ObjectProperty<Image>
  // Java
  extraTile.imageProperty().bind(javafx.beans.binding.Bindings.createObjectBinding(
      () -> new javafx.scene.image.Image(
              MyController.class.getResource(pathProp.get()).toExternalForm()
      ),
      pathProp // Observable
  ));
    // Java
  extraTile.rotateProperty().bind(angleProperty); // DoubleProperty
    // Java
  extraTile.fitWidthProperty().bind(container.widthProperty().divide(7));
  extraTile.fitHeightProperty().bind(container.heightProperty().divide(7));
  extraTile.setPreserveRatio(true);
    // Java
  javafx.scene.layout.Region container = somePane; // e.g., GridPane, StackPane
  extraTile.fitWidthProperty().bind(container.widthProperty().divide(7));
  extraTile.fitHeightProperty().bind(container.heightProperty().divide(7));
  extraTile.setPreserveRatio(true);


  // Java
  extraTile.setRotate(90); // degrees

  // Java
  ImageView iv = new ImageView(new javafx.scene.image.Image(
      getClass().getResource("/tiles/up_down_512px.png").toExternalForm()
  ));
  iv.setFitWidth(110);
  iv.setFitHeight(110);
  iv.setPreserveRatio(true);



    // Java
  javafx.scene.image.Image img = new javafx.scene.image.Image(
      getClass().getResource("/tiles/up_down_512px.png").toExternalForm()
  );
  extraTile.setImage(img);
  extraTile.setFitWidth(110);
  extraTile.setFitHeight(110);
  extraTile.setPreserveRatio(true);
 */