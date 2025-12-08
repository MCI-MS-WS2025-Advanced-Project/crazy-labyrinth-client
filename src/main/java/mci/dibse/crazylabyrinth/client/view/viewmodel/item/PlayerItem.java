package mci.dibse.crazylabyrinth.client.view.viewmodel.item;

import javafx.application.Platform;
import javafx.beans.property.*;
import labyrinth.contracts.models.PlayerColor;
import lombok.Getter;
import mci.dibse.crazylabyrinth.client.domain.model.ClientPlayer;
import mci.dibse.crazylabyrinth.client.domain.model.GameServer;
import mci.dibse.crazylabyrinth.client.domain.model.PlayerInfo;

import java.util.Map;
import java.util.UUID;

public class PlayerItem {

    private final java.util.concurrent.atomic.AtomicBoolean pendingUpdate =
            new java.util.concurrent.atomic.AtomicBoolean(false);


    //TODO remove?
//    private final java.beans.PropertyChangeListener strongListener;



    private final StringProperty id = new SimpleStringProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final ObjectProperty<PlayerColor> color = new SimpleObjectProperty<>();
    private final BooleanProperty isAdmin = new SimpleBooleanProperty();
    private final BooleanProperty isAiControlled = new SimpleBooleanProperty();


    public PlayerItem(PlayerInfo source) {
        this.id.set(source.getId());
        updateFrom(source);
        /*
        this.strongListener = (event) -> updateFrom(source);
        source.addListener(strongListener);

         */

    }
/*
    public void dispose(PlayerInfo source) {
        source.removeListener(strongListener);
    }
 */
    public void updateFrom(PlayerInfo source) {
        if (pendingUpdate.compareAndSet(false, true)) {
            if (javafx.application.Platform.isFxApplicationThread()) {
                updateItemFromSource(source);
                pendingUpdate.set(false);
            } else {
                Platform.runLater(() -> updateItemFromSource(source));
                pendingUpdate.set(false);
            }
        }
    }

    private void updateItemFromSource(PlayerInfo source) {
        name.set(source.getName());
        color.set(source.getColor());
        isAdmin.set(source.getIsAdmin());
        isAiControlled.set(source.getIsAiControlled());
    }

    public String getId() {
        return id.get();
    }

    public javafx.beans.property.StringProperty idProperty() {
        return id;
    }

    public String getName() {
        return name.get();
    }

    public javafx.beans.property.StringProperty nameProperty() {
        return name;
    }

    public PlayerColor getColor() {
        return color.get();
    }

    public javafx.beans.property.ObjectProperty<PlayerColor> colorProperty() {
        return color;
    }

    public Boolean getIsAdmin() {
        return isAdmin.get();
    }

    public javafx.beans.property.BooleanProperty isAdminProperty() {
        return isAdmin;
    }

    public Boolean getIsAiControlled() {
        return isAiControlled.get();
    }

    public javafx.beans.property.BooleanProperty isAiControlledProperty() {
        return isAiControlled;
    }

}
