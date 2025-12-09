package mci.dibse.crazylabyrinth.client.view.viewmodel.item;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import labyrinth.contracts.models.Treasure;
import lombok.Getter;

import java.util.Map;

public class TreasureItem implements Disposable {

    private final java.util.concurrent.atomic.AtomicBoolean pendingUpdate =
            new java.util.concurrent.atomic.AtomicBoolean(false);

    @Getter
    private final SimpleIntegerProperty id = new SimpleIntegerProperty();
    @Getter
    private final SimpleStringProperty name = new SimpleStringProperty();
    @Getter
    private final SimpleObjectProperty<Map<String, Object>> additionalProperties = new SimpleObjectProperty<>();

    public TreasureItem(Treasure source) {
        this.id.set(source.getId());
        this.name.set(source.getName());
        this.additionalProperties.set(source.getAdditionalProperties());
    }

    public void updateFrom(Treasure source) {
        this.additionalProperties.set(source.getAdditionalProperties());
    }


    @Override
    public void dispose() {
        //TODO something to do here?
    }
}
