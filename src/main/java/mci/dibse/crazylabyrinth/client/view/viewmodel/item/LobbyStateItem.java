package mci.dibse.crazylabyrinth.client.view.viewmodel.item;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import labyrinth.contracts.models.LobbyStateEventPayload;
import lombok.Getter;
import mci.dibse.crazylabyrinth.client.domain.model.record.GameInfoRecord;
import mci.dibse.crazylabyrinth.client.infrastructure.util.JavaFXUtils;

public class LobbyStateItem implements ViewItem {

    private final java.util.concurrent.atomic.AtomicBoolean pendingUpdate =
            new java.util.concurrent.atomic.AtomicBoolean(false);

    @Getter private final StringProperty playerCount = new SimpleStringProperty();
    @Getter private final StringProperty maxPlayers = new SimpleStringProperty();
    @Getter private final StringProperty serverName = new SimpleStringProperty();

    public LobbyStateItem(LobbyStateEventPayload source) {
        // todo impl
        updateFrom(source);
    }

    @Override
    public void dispose() {
        playerCount.set(null);
        maxPlayers.set(null);
        serverName.set(null);
    }

    public void updateFrom(LobbyStateEventPayload source) {
        if (pendingUpdate.compareAndSet(false, true)) {
            JavaFXUtils.runOnFxThread(
                    () -> {
                        updateItemFromSource(source);
                        pendingUpdate.set(false);
                    });
        }
    }

    public void updateFromGameInfo(GameInfoRecord info) {
        this.serverName.set(info.serverName());
        this.maxPlayers.set(info.maxPlayers());
    }

    private void updateItemFromSource(LobbyStateEventPayload source) {
        // todo impl
    }
}
