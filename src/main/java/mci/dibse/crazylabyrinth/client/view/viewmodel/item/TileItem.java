package mci.dibse.crazylabyrinth.client.view.viewmodel.item;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.image.Image;
import labyrinth.contracts.models.Bonus;
import labyrinth.contracts.models.Direction;
import labyrinth.contracts.models.Tile;
import lombok.Getter;
import mci.dibse.crazylabyrinth.client.infrastructure.util.ImageResourceLoader;

import java.util.*;

public class TileItem implements Disposable {


    private final java.util.concurrent.atomic.AtomicBoolean pendingUpdate =
            new java.util.concurrent.atomic.AtomicBoolean(false);


    @Getter private final String internalId = UUID.randomUUID().toString();
    @Getter private final List<Direction> directions;

    @Getter private final SimpleObjectProperty<Image> tileImage = new SimpleObjectProperty<>();
    @Getter private final SimpleObjectProperty<TreasureItem> treasure = new SimpleObjectProperty<>();
    @Getter private final SimpleObjectProperty<Bonus> bonus = new SimpleObjectProperty<>();
    @Getter private SimpleBooleanProperty isFixed = new SimpleBooleanProperty();

    @Getter private final ObjectProperty<Map<String, Object>> additionalProperties = new SimpleObjectProperty<>();


    public TileItem(Tile source) {
        List<Direction> directionList = new ArrayList<>(Arrays.asList(source.getEntrances()));
        directionList.sort(Comparator.comparingInt(Direction::ordinal));
        this.directions = directionList;

        Runnable apply = () -> {

            ImageResourceLoader loader = new ImageResourceLoader();
            loader.loadImage()

            tileImage.set();


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


    @Override
    public void dispose() {
        directions.clear();
        treasure.get().dispose();
        treasure.set(null);
        bonus.set(null);
        additionalProperties.set(null);
    }
}
