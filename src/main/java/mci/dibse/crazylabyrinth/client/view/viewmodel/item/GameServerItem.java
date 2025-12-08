package mci.dibse.crazylabyrinth.client.view.viewmodel.item;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import javafx.beans.property.*;
import lombok.Getter;
import mci.dibse.crazylabyrinth.client.domain.model.GameServer;
import mci.dibse.crazylabyrinth.client.domain.model.enums.GameStatus;

/**
 *  UI representation of GameServer
 *  @author Julian Janetschek
 */
public class GameServerItem {

    private final java.util.concurrent.atomic.AtomicBoolean pendingUpdate =
            new java.util.concurrent.atomic.AtomicBoolean(false);
    private final java.beans.PropertyChangeListener strongListener;
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
        this.strongListener = evt -> updateFrom(source);
        source.addListener(strongListener);
    }

    public void dispose(GameServer source) {
        source.removeListener(strongListener);
    }

    public void updateFrom(GameServer source) {
        if (pendingUpdate.compareAndSet(false, true)) {
            if (javafx.application.Platform.isFxApplicationThread()) {
                updateItemFromSource(source);
                pendingUpdate.set(false);
            } else {
                javafx.application.Platform.runLater(() -> updateItemFromSource(source));
                pendingUpdate.set(false);
            }
        }
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
}
