package mci.dibse.crazylabyrinth.client.view.viewmodel.item;

import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.image.Image;
import labyrinth.contracts.models.Treasure;
import lombok.Getter;
import mci.dibse.crazylabyrinth.client.infrastructure.util.ImageResourceLoader;
import mci.dibse.crazylabyrinth.client.infrastructure.util.JavaFXUtils;

public class TreasureItem extends BaseItem<Treasure> {

    @Getter private SimpleObjectProperty<Image> treasureImageProperty = new SimpleObjectProperty<>();

    @Getter private int id;
    @Getter private String name;

    public TreasureItem(Treasure source) {
        id = source.getId();
        name = source.getName();
        JavaFXUtils.runOnFxThread(() -> treasureImageProperty.set(ImageResourceLoader.loadImage(id)));
    }

    public Image getTreasureImage() {
        return treasureImageProperty.get();
    }

    @Override
    protected void disposeItem() {
        // nothing to do here
    }

    @Override
    protected void updateItemFromSource(Treasure source) {
        id = source.getId();
        name = source.getName();
    }
}
