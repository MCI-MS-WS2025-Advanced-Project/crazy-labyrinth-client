package mci.dibse.crazylabyrinth.client.view.viewmodel.item;

import labyrinth.contracts.models.Treasure;
import lombok.Getter;

public class TreasureItem extends BaseItem<Treasure> {



    @Getter private int id;
    @Getter private String name;

    public TreasureItem(Treasure source) {
        id = source.getId();
        name = source.getName();
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
