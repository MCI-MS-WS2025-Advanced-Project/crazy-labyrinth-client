package mci.dibse.crazylabyrinth.client.view.viewmodel.item;

import javafx.beans.property.*;
import labyrinth.contracts.models.PlayerColor;
import labyrinth.contracts.models.PlayerInfo;

public class PlayerInfoItem extends BaseItem<PlayerInfo> {

    private final StringProperty id = new SimpleStringProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final ObjectProperty<PlayerColor> color = new SimpleObjectProperty<>();
    private final BooleanProperty isAdmin = new SimpleBooleanProperty();
    private final BooleanProperty isAiControlled = new SimpleBooleanProperty();

    public PlayerInfoItem(PlayerInfo source) {
        updateFrom(source);
    }

    protected void updateItemFromSource(PlayerInfo source) {
        id.set(source.getId());
        name.set(source.getName());
        color.set(source.getColor());
        isAdmin.set(source.getIsAdmin());
        isAiControlled.set(source.getIsAiControlled());
    }

    public String getId() {
        return id.get();
    }

    public StringProperty idProperty() {
        return id;
    }

    public String getName() {
        return name.get();
    }

    public StringProperty nameProperty() {
        return name;
    }

    public PlayerColor getColor() {
        return color.get();
    }

    public ObjectProperty<PlayerColor> colorProperty() {
        return color;
    }

    public Boolean getIsAdmin() {
        return isAdmin.get();
    }

    public BooleanProperty isAdminProperty() {
        return isAdmin;
    }

    public Boolean getIsAiControlled() {
        return isAiControlled.get();
    }

    public BooleanProperty isAiControlledProperty() {
        return isAiControlled;
    }

    @Override
    protected void disposeItem() {
        // todo?
    }
}
