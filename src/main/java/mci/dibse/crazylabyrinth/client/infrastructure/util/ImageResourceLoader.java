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


    private static final String TILE_IMAGES_PATH = "/tiles/";
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




    public  static  String mapDirectionsToTile(Direction[] direction) {
        List<Direction> l = new ArrayList<>(Arrays.asList(direction));
        l.sort(Comparator.comparingInt(Direction::ordinal));
        switch (l.size()) {
            case 2 -> {
                if (l.contains(Direction.UP) && l.contains(Direction.DOWN)) {
                    // up_down_px.png
                } else if (l.contains(Direction.LEFT) && l.contains(Direction.RIGHT)) {
                    // left_right_px.png
                }  else if ((l.contains(Direction.UP) && l.contains(Direction.RIGHT)) ||
                        (l.contains(Direction.DOWN) && l.contains(Direction.LEFT)) ||
                        (l.contains(Direction.UP) && l.contains(Direction.LEFT)) ||
                        (l.contains(Direction.DOWN) && l.contains(Direction.RIGHT))) {
                    // Corner pieces - you might want to handle rotation here
                    return getCornerTileWithRotation(l);
                }
            }   case 3 -> {
                if (l.contains(Direction.UP) && l.contains(Direction.DOWN) && l.contains(Direction.LEFT)) {

                } else if (l.contains(Direction.UP) && l.contains(Direction.DOWN) && l.contains(Direction.RIGHT)) {

                } else if (l.contains(Direction.LEFT) && l.contains(Direction.RIGHT) && l.contains(Direction.UP)) {

                } else if (l.contains(Direction.LEFT) && l.contains(Direction.RIGHT) && l.contains(Direction.DOWN)) {

                }
            }
        }


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

}

// Java
try (var in = getClass().getResourceAsStream("/tiles/straight_vertical_512px.png")) {
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
      getClass().getResource("/tiles/straight_vertical_512px.png").toExternalForm()
  ));
  iv.setFitWidth(110);
  iv.setFitHeight(110);
  iv.setPreserveRatio(true);



    // Java
  javafx.scene.image.Image img = new javafx.scene.image.Image(
      getClass().getResource("/tiles/straight_vertical_512px.png").toExternalForm()
  );
  extraTile.setImage(img);
  extraTile.setFitWidth(110);
  extraTile.setFitHeight(110);
  extraTile.setPreserveRatio(true);
 */