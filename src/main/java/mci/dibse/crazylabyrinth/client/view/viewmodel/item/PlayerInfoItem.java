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
        super();
        updateFrom(source);
    }

    /**
     * Update item state from the source player info.
     *
     * @param source player info payload
     */
    protected void updateItemFromSource(PlayerInfo source) {
        id.set(source.getId());
        name.set(source.getName());
        color.set(source.getColor());
        isAdmin.set(source.getIsAdmin());
        isAiControlled.set(source.getIsAiControlled());
    }

    /**
     * Return the player id.
     *
     * @return player id
     */
    public String getId() {
        return id.get();
    }

    /**
     * Return the id property.
     *
     * @return id property
     */
    public StringProperty idProperty() {
        return id;
    }

    /**
     * Return the player name.
     *
     * @return player name
     */
    public String getName() {
        return name.get();
    }

    /**
     * Return the name property.
     *
     * @return name property
     */
    public StringProperty nameProperty() {
        return name;
    }

    /**
     * Return the player color.
     *
     * @return player color
     */
    public PlayerColor getColor() {
        return color.get();
    }

    /**
     * Return the color property.
     *
     * @return color property
     */
    public ObjectProperty<PlayerColor> colorProperty() {
        return color;
    }

    /**
     * Return whether the player is an admin.
     *
     * @return true if admin
     */
    public Boolean getIsAdmin() {
        return isAdmin.get();
    }

    /**
     * Return the admin property.
     *
     * @return admin property
     */
    public BooleanProperty isAdminProperty() {
        return isAdmin;
    }

    /**
     * Return whether the player is AI controlled.
     *
     * @return true if AI controlled
     */
    public Boolean getIsAiControlled() {
        return isAiControlled.get();
    }

    /**
     * Return the AI controlled property.
     *
     * @return AI controlled property
     */
    public BooleanProperty isAiControlledProperty() {
        return isAiControlled;
    }

    /**
     * Dispose item state; no-op for player info.
     */
    @Override
    protected void disposeItem() {
        // nothing to do here
    }
}
