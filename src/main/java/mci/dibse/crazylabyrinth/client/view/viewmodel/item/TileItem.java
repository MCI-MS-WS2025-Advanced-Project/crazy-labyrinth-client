package mci.dibse.crazylabyrinth.client.view.viewmodel.item;

import static mci.dibse.crazylabyrinth.client.infrastructure.util.JavaFXUtils.runOnFxThread;

import java.util.*;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.image.Image;
import labyrinth.contracts.models.BonusType;
import labyrinth.contracts.models.Direction;
import labyrinth.contracts.models.Tile;
import lombok.Getter;
import mci.dibse.crazylabyrinth.client.infrastructure.util.ImageResourceLoader;

public class TileItem extends BaseItem<Tile> {

    @Getter private final String internalId = UUID.randomUUID().toString();
    @Getter private final List<Direction> directions;

    @Getter private final SimpleObjectProperty<Image> tileImage = new SimpleObjectProperty<>();
    @Getter private final SimpleObjectProperty<Number> rotation = new SimpleObjectProperty<>();

    @Getter
    private final SimpleObjectProperty<TreasureItem> treasure = new SimpleObjectProperty<>();

    @Getter
    private final SimpleObjectProperty<BonusType> bonusProperty = new SimpleObjectProperty<>();

    @Getter private SimpleBooleanProperty isFixed = new SimpleBooleanProperty();

    /**
     * Return the treasure item on this tile.
     *
     * @return treasure item or null when none
     */
    public TreasureItem getTreasureItem() {
        return treasure.get();
    }

    /**
     * Return the bonus type on this tile.
     *
     * @return bonus type or null when none
     */
    public BonusType getBonusType() {
        return bonusProperty.get();
    }

    /**
     * Check whether the tile has a bonus.
     *
     * @return true when a bonus is present
     */
    public boolean hasBonus() {
        return bonusProperty.get() != null;
    }

    /**
     * Check whether the tile has a treasure.
     *
     * @return true when a treasure is present
     */
    public boolean hasTreasure() {
        return treasure.get() != null;
    }

    public TileItem(Tile source) {
        super();
        List<Direction> directionList = new ArrayList<>(Arrays.asList(source.getEntrances()));
        directionList.sort(Comparator.comparingInt(Direction::ordinal));
        this.directions = directionList;

        Runnable apply =
                () -> {

                    //  imageView = ImageResourceLoader.mapDirectionsToTile(directions);

                    tileImage.set(ImageResourceLoader.loadImage(directionList));
                    rotation.set(getRotationAngleForDirection(directionList));

                    if (source.getTreasure() != null) {
                        this.treasure.set(new TreasureItem(source.getTreasure()));
                    } else {
                        this.treasure.set(null);
                    }

                    if (source.getBonus() != null) {
                        this.bonusProperty.set(source.getBonus());
                    } else {
                        this.bonusProperty.set(null);
                    }

                    this.isFixed.set(source.getIsFixed());
                };
        runOnFxThread(apply);
    }

    @Override
    protected void disposeItem() {
        // nothing to do here
    }

    /**
     * Update tile state from the source tile.
     *
     * @param source tile payload
     */
    @Override
    protected void updateItemFromSource(Tile source) {
        this.isFixed.set(source.getIsFixed());
        List<Direction> directionList = new ArrayList<>(Arrays.asList(source.getEntrances()));
        this.rotation.set(getRotationAngleForDirection(directionList));

        if (this.treasure.get() != null) {
            this.treasure.get().updateFrom(source.getTreasure());
        }
        if (source.getBonus() != null) {
            this.bonusProperty.set(source.getBonus());
        } else {
            this.bonusProperty.set(null);
        }
    }

    /**
     * Update the direction list after a rotation.
     *
     * @param clockwise true for clockwise rotation
     */
    private void updateDirectionsonRotation(boolean clockwise) {

        switch (directions.size()) {
            case 2:
                if (directions.contains(Direction.UP) && directions.contains(Direction.DOWN)) {
                    directions.clear();
                    directions.add(Direction.RIGHT);
                    directions.add(Direction.LEFT);
                } else if (directions.contains(Direction.LEFT)
                        && directions.contains(Direction.RIGHT)) {
                    directions.clear();
                    directions.add(Direction.UP);
                    directions.add(Direction.DOWN);
                } else if ((directions.contains(Direction.UP)
                        && directions.contains(Direction.RIGHT))) {
                    if (clockwise) {
                        directions.remove(Direction.UP);
                        directions.add(Direction.DOWN);
                    } else {
                        directions.remove(Direction.RIGHT);
                        directions.add(Direction.LEFT);
                    }
                } else if (directions.contains(Direction.DOWN)
                        && directions.contains(Direction.LEFT)) {
                    if (clockwise) {
                        directions.remove(Direction.DOWN);
                        directions.add(Direction.UP);
                    } else {
                        directions.remove(Direction.LEFT);
                        directions.add(Direction.RIGHT);
                    }
                } else if (directions.contains(Direction.UP)
                        && directions.contains(Direction.LEFT)) {
                    if (clockwise) {
                        directions.remove(Direction.LEFT);
                        directions.add(Direction.RIGHT);
                    } else {
                        directions.remove(Direction.UP);
                        directions.add(Direction.DOWN);
                    }
                } else if (directions.contains(Direction.DOWN)
                        && directions.contains(Direction.RIGHT)) {
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
                if (directions.contains(Direction.UP)
                        && directions.contains(Direction.DOWN)
                        && directions.contains(Direction.LEFT)) {
                    directions.add(Direction.RIGHT);
                    if (clockwise) {
                        directions.remove(Direction.DOWN);
                    } else {
                        directions.remove(Direction.UP);
                    }
                } else if (directions.contains(Direction.UP)
                        && directions.contains(Direction.DOWN)
                        && directions.contains(Direction.RIGHT)) {
                    directions.add(Direction.LEFT);
                    if (clockwise) {
                        directions.remove(Direction.UP);
                    } else {
                        directions.remove(Direction.DOWN);
                    }
                } else if (directions.contains(Direction.LEFT)
                        && directions.contains(Direction.RIGHT)
                        && directions.contains(Direction.UP)) {
                    directions.add(Direction.DOWN);
                    if (clockwise) {
                        directions.remove(Direction.LEFT);
                    } else {
                        directions.remove(Direction.RIGHT);
                    }
                } else if (directions.contains(Direction.LEFT)
                        && directions.contains(Direction.RIGHT)
                        && directions.contains(Direction.DOWN)) {
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

    /**
     * Refresh the tile image based on the current directions.
     */
    private void updateImageFromRotation() {
        tileImage.set(ImageResourceLoader.loadImage(directions));
    }

    /**
     * Compute the rotation angle for a set of entrances.
     *
     * @param direction list of entrance directions
     * @return rotation angle in degrees
     */
    private static Number getRotationAngleForDirection(List<Direction> direction) {
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
}
