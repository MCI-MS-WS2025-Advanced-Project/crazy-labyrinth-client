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

    @Getter private final SimpleObjectProperty<BonusType> bonus = new SimpleObjectProperty<>();
    @Getter private SimpleBooleanProperty isFixed = new SimpleBooleanProperty();

    public TreasureItem getTreasureItem() {
        return treasure.get();
    }

    public BonusType getBonusType() {
        return bonus.get();
    }

    public TileItem(Tile source) {
        List<Direction> directionList = new ArrayList<>(Arrays.asList(source.getEntrances()));
        directionList.sort(Comparator.comparingInt(Direction::ordinal));
        this.directions = directionList;

        Runnable apply =
                () -> {

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
        runOnFxThread(apply);
    }

    @Override
    protected void disposeItem() {
        // todo?
    }

    @Override
    protected void updateItemFromSource(Tile source) {
        this.isFixed.set(source.getIsFixed());
        List<Direction> directionList = new ArrayList<>(Arrays.asList(source.getEntrances()));
        this.rotation.set(ImageResourceLoader.getRotationAngleForDirection(directionList));

        if (this.treasure.get() != null) {
            this.treasure.get().updateFrom(source.getTreasure());
        }
        if (source.getBonus() != null) {
            this.bonus.set(source.getBonus());
        } else {
            this.bonus.set(null);
        }
    }

    // todo not needed anymore? rotation is done on server via command
    public void onLeftRotation() {
        Runnable apply =
                () -> {
                    Number rotation = this.rotation.get();
                    this.rotation.set((rotation.intValue() - 90) % 360);
                    updateDirectionsonRotation(false);
                    updateImageFromRotation();
                };
        runOnFxThread(apply);
    }

    public void onRightRotation() {
        Runnable apply =
                () -> {
                    Number rotation = this.rotation.get();
                    this.rotation.set((rotation.intValue() + 90) % 360);
                    updateDirectionsonRotation(true);
                    updateImageFromRotation();
                };
        runOnFxThread(apply);
    }

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

    private void updateImageFromRotation() {
        tileImage.set(ImageResourceLoader.loadImageForDirection(directions));
    }
}
