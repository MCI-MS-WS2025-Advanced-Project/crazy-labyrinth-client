package mci.dibse.crazylabyrinth.client.view.viewmodel.item;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import labyrinth.contracts.models.Bonus;
import labyrinth.contracts.models.Direction;
import labyrinth.contracts.models.Tile;
import lombok.Getter;
import lombok.Setter;
import mci.dibse.crazylabyrinth.client.infrastructure.util.ImageResourceLoader;

import java.util.*;

public class TileItem implements Disposable {


    private final java.util.concurrent.atomic.AtomicBoolean pendingUpdate =
            new java.util.concurrent.atomic.AtomicBoolean(false);


    @Getter private final String internalId = UUID.randomUUID().toString();
    @Getter private final List<Direction> directions;

    @Getter private final SimpleObjectProperty<Image> tileImage = new SimpleObjectProperty<>();
    @Getter private final SimpleObjectProperty<Number> rotation = new SimpleObjectProperty<>();
    @Getter private final SimpleObjectProperty<TreasureItem> treasure = new SimpleObjectProperty<>();
    @Getter private final SimpleObjectProperty<Bonus> bonus = new SimpleObjectProperty<>();
    @Getter private SimpleBooleanProperty isFixed = new SimpleBooleanProperty();

    @Getter private final ObjectProperty<Map<String, Object>> additionalProperties = new SimpleObjectProperty<>();


   // @Getter private  ImageView imageView;

    public TileItem(Tile source) {
        List<Direction> directionList = new ArrayList<>(Arrays.asList(source.getEntrances()));
        directionList.sort(Comparator.comparingInt(Direction::ordinal));
        this.directions = directionList;

        Runnable apply = () -> {

          //  imageView = ImageResourceLoader.mapDirectionsToTile(directions);

            tileImage.set(ImageResourceLoader.loadImageForDirection(directionList));
            rotation.set(ImageResourceLoader.getRotationAngleForDirection(directionList));

            if (source.getTreasure() != null) {
                this.treasure.set(new TreasureItem(source.getTreasure()));
            } else {
                this.treasure.set(null);
            }

            if (source.getBonus() != null) {
                this.bonus.set(source.getBonus());
            } else {
                this.bonus.set(null);
            }

            this.isFixed.set(source.getIsFixed());
        };
        if (javafx.application.Platform.isFxApplicationThread()) apply.run();
        else javafx.application.Platform.runLater(apply);

    }

    public void updateFrom(Tile source) {
        Runnable apply = () -> {
            this.isFixed.set(source.getIsFixed());
            List<Direction> directionList = new ArrayList<>(Arrays.asList(source.getEntrances()));
            this.rotation.set(ImageResourceLoader.getRotationAngleForDirection(directionList));


            if ( this.treasure.get() != null) {
                this.treasure.get().updateFrom(source.getTreasure());
            }
            if (source.getBonus() != null) {
                this.bonus.set(source.getBonus());
            } else {
                this.bonus.set(null);
            }
        };
        if (javafx.application.Platform.isFxApplicationThread()) apply.run();
        else javafx.application.Platform.runLater(apply);
    }

    public void onLeftRotation() {
        Runnable apply = () -> {
            Number rotation = this.rotation.get();
            this.rotation.set((rotation.intValue() - 90) % 360);
            updateDirectionsonRotation(false);
            updateImageFromRotation();
        };
        if (javafx.application.Platform.isFxApplicationThread()) apply.run();
        else javafx.application.Platform.runLater(apply);
    }
    public void onRightRotation() {
        Runnable apply = () -> {
            Number rotation = this.rotation.get();
            this.rotation.set((rotation.intValue() + 90) % 360);
            updateDirectionsonRotation(true);
            updateImageFromRotation();
        };
        if (javafx.application.Platform.isFxApplicationThread()) apply.run();
        else javafx.application.Platform.runLater(apply);
    }

    private void updateDirectionsonRotation(boolean clockwise) {

        switch (directions.size()) {
            case 2:
                if (directions.contains(Direction.UP) && directions.contains(Direction.DOWN)) {
                    directions.clear();
                    directions.add(Direction.RIGHT);
                    directions.add(Direction.LEFT);
                } else if (directions.contains(Direction.LEFT) && directions.contains(Direction.RIGHT)) {
                    directions.clear();
                    directions.add(Direction.UP);
                    directions.add(Direction.DOWN);
                } else if ((directions.contains(Direction.UP) && directions.contains(Direction.RIGHT))) {
                    if (clockwise) {
                        directions.remove(Direction.UP);
                        directions.add(Direction.DOWN);
                    } else {
                        directions.remove(Direction.RIGHT);
                        directions.add(Direction.LEFT);
                    }
                } else if (directions.contains(Direction.DOWN) && directions.contains(Direction.LEFT)) {
                    if (clockwise) {
                        directions.remove(Direction.DOWN);
                        directions.add(Direction.UP);
                    } else {
                        directions.remove(Direction.LEFT);
                        directions.add(Direction.RIGHT);
                    }
                } else if (directions.contains(Direction.UP) && directions.contains(Direction.LEFT)) {
                    if (clockwise) {
                        directions.remove(Direction.LEFT);
                        directions.add(Direction.RIGHT);
                    } else {
                        directions.remove(Direction.UP);
                        directions.add(Direction.DOWN);
                    }
                } else if (directions.contains(Direction.DOWN) && directions.contains(Direction.RIGHT)) {
                    if (clockwise) {
                        directions.remove(Direction.RIGHT);
                        directions.add(Direction.LEFT);
                    } else {
                        directions.remove(Direction.DOWN);
                        directions.add(Direction.UP);
                    }
                } else {
                    throw new IllegalStateException("Unexpected directions: " + directions);
                }
                break;
            case 3:
                if (directions.contains(Direction.UP) && directions.contains(Direction.DOWN) && directions.contains(Direction.LEFT)) {
                    directions.add(Direction.RIGHT);
                    if (clockwise) {
                        directions.remove(Direction.DOWN);
                    } else {
                        directions.remove(Direction.UP);
                    }
                } else if (directions.contains(Direction.UP) && directions.contains(Direction.DOWN) && directions.contains(Direction.RIGHT)) {
                    directions.add(Direction.LEFT);
                    if (clockwise) {
                        directions.remove(Direction.UP);
                    } else {
                        directions.remove(Direction.DOWN);
                    }
                } else if (directions.contains(Direction.LEFT) && directions.contains(Direction.RIGHT) && directions.contains(Direction.UP)) {
                    directions.add(Direction.DOWN);
                    if (clockwise) {
                        directions.remove(Direction.LEFT);
                    } else {
                        directions.remove(Direction.RIGHT);
                    }
                } else if (directions.contains(Direction.LEFT) && directions.contains(Direction.RIGHT) && directions.contains(Direction.DOWN)) {
                    directions.add(Direction.UP);
                    if (clockwise) {
                        directions.remove(Direction.RIGHT);
                    } else {
                        directions.remove(Direction.LEFT);
                    }
                } else {
                    throw new IllegalStateException("Unexpected directions: " + directions);
                }
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + directions.size());
        }
    }

    private void updateImageFromRotation() {
        tileImage.set(ImageResourceLoader.loadImageForDirection(directions));
    }

       /*
        for (int i = 0; i < directions.size(); i++) {
            Direction current = directions.get(i);
            Direction rotated;
            if (clockwise) {
                rotated = switch (current) {
                    case NORTH -> Direction.EAST;
                    case EAST -> Direction.SOUTH;
                    case SOUTH -> Direction.WEST;
                    case WEST -> Direction.NORTH;
                };
            } else {
                rotated = switch (current) {
                    case NORTH -> Direction.WEST;
                    case WEST -> Direction.SOUTH;
                    case SOUTH -> Direction.EAST;
                    case EAST -> Direction.NORTH;
                };
            }
            /directions.set(i, rotated);
        }
        directions.sort(Comparator.comparingInt(Direction::ordinal));

        */


    @Override
    public void dispose() {
        tileImage.unbind();
        rotation.unbind();
        directions.clear();
        if(!treasure.isNull().get()) treasure.get().dispose();
        treasure.set(null);
        additionalProperties.set(null);
    }

}
