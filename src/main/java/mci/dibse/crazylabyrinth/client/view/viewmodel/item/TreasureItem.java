package mci.dibse.crazylabyrinth.client.view.viewmodel.item;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import labyrinth.contracts.models.Treasure;
import lombok.Getter;

public class TreasureItem extends BaseItem<Treasure> {

    @Getter private final SimpleIntegerProperty id = new SimpleIntegerProperty();
    @Getter private final SimpleStringProperty name = new SimpleStringProperty();

    public TreasureItem(Treasure source) {
        this.id.set(source.getId());
        this.name.set(source.getName());
    }

    @Override
    protected void disposeItem() {
        // todo?
    }

    @Override
    protected void updateItemFromSource(Treasure source) {
        // TODO?
    }
}
