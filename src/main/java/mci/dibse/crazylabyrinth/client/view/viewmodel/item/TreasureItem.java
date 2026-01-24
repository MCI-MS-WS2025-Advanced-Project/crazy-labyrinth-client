package mci.dibse.crazylabyrinth.client.view.viewmodel.item;

import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.image.Image;
import labyrinth.contracts.models.Treasure;
import lombok.Getter;
import mci.dibse.crazylabyrinth.client.infrastructure.util.ImageResourceLoader;
import mci.dibse.crazylabyrinth.client.infrastructure.util.JavaFXUtils;

public class TreasureItem extends BaseItem<Treasure> {

    @Getter
    private SimpleObjectProperty<Image> treasureImageProperty = new SimpleObjectProperty<>();

    @Getter private int id;
    @Getter private String name;

    public TreasureItem(Treasure source) {
        id = source.getId();
        name = source.getName();
        JavaFXUtils.runOnFxThread(
                () -> treasureImageProperty.set(ImageResourceLoader.loadImage(id)));
    }

    /**
     * Return the treasure image.
     *
     * @return treasure image
     */
    public Image getTreasureImage() {
        return treasureImageProperty.get();
    }

    /**
     * Dispose item state; no-op for treasures.
     */
    @Override
    protected void disposeItem() {
        // nothing to do here
    }

    /**
     * Update item state from the treasure payload.
     *
     * @param source treasure payload
     */
    @Override
    protected void updateItemFromSource(Treasure source) {
        id = source.getId();
        name = source.getName();
    }
}
