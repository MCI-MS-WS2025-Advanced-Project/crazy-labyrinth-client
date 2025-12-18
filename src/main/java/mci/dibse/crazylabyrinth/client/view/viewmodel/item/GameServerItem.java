package mci.dibse.crazylabyrinth.client.view.viewmodel.item;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.beans.property.*;
import lombok.Getter;
import mci.dibse.crazylabyrinth.client.domain.model.enums.GameStatus;
import mci.dibse.crazylabyrinth.client.domain.model.management.GameServer;
import mci.dibse.crazylabyrinth.client.infrastructure.util.JavaFXUtils;

/**
 *  UI representation of GameServer
 *  @author Julian Janetschek
 */
public class GameServerItem implements ViewItem {

    private final AtomicBoolean pendingUpdate = new AtomicBoolean(false);

    @Getter private final UUID id;

    @Getter private final StringProperty name = new SimpleStringProperty();
    @Getter private final ObjectProperty<URI> uri = new SimpleObjectProperty<>();
    @Getter private final IntegerProperty maxPlayers = new SimpleIntegerProperty();
    @Getter private final IntegerProperty currentPlayerCount = new SimpleIntegerProperty();
    @Getter private final ObjectProperty<GameStatus> status = new SimpleObjectProperty<>();
    @Getter private final ObjectProperty<Instant> lastSeen = new SimpleObjectProperty<>();

    public GameServerItem(GameServer source) {
        this.id = source.getId();
        updateFrom(source);
    }

    public void updateFrom(GameServer source) {
        if (!pendingUpdate.compareAndSet(false, true)) return;

        Runnable updateTask =
                () -> {
                    try {
                        updateItemFromSource(source);
                    } finally {
                        pendingUpdate.set(false);
                    }
                };
        JavaFXUtils.runOnFxThread(updateTask);
    }

    private void updateItemFromSource(GameServer source) {
        name.set(source.getName());
        uri.set(source.getUri());
        maxPlayers.set(source.getMaxPlayers());
        currentPlayerCount.set(source.getCurrentPlayerCount());
        status.set(source.getStatus());
        lastSeen.set(source.getLastSeen());
    }

    public StringProperty nameProperty() {
        return name;
    }

    public ObjectProperty<URI> uriProperty() {
        return uri;
    }

    public IntegerProperty maxPlayersProperty() {
        return maxPlayers;
    }

    public IntegerProperty currentPlayerCountProperty() {
        return currentPlayerCount;
    }

    public ObjectProperty<GameStatus> statusProperty() {
        return status;
    }

    public ObjectProperty<Instant> lastSeenProperty() {
        return lastSeen;
    }

    // todo something to do here?
    @Override
    public void dispose() {}
}
