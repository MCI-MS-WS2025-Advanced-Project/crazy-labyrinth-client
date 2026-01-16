package mci.dibse.crazylabyrinth.client.view.viewmodel.item;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import javafx.beans.property.*;
import lombok.Getter;
import mci.dibse.crazylabyrinth.client.domain.model.enums.GameStatus;
import mci.dibse.crazylabyrinth.client.domain.model.management.GameServer;
import mci.dibse.crazylabyrinth.client.infrastructure.util.JavaFXUtils;

/**
 *  UI representation of GameServer
 *  @author Julian Janetschek
 */
public class GameServerItem extends BaseItem<GameServer> implements ViewItem {

    @Getter private final UUID id;

    @Getter private final StringProperty name = new SimpleStringProperty();
    @Getter private final ObjectProperty<URI> uri = new SimpleObjectProperty<>();
    @Getter private final IntegerProperty maxPlayers = new SimpleIntegerProperty();
    @Getter private final IntegerProperty currentPlayerCount = new SimpleIntegerProperty();
    @Getter private final ObjectProperty<GameStatus> status = new SimpleObjectProperty<>();
    @Getter private final ObjectProperty<Instant> lastSeen = new SimpleObjectProperty<>();

    @Getter private final ReadOnlyBooleanWrapper joinable = new ReadOnlyBooleanWrapper();

    public GameServerItem(GameServer source) {
        super();
        this.id = source.getId();
        Runnable apply = () -> {
            setJoinableProperty();
            updateFrom(source);
        };
        JavaFXUtils.runOnFxThread(apply);

    }

    private void setJoinableProperty() {
        joinable.bind(
                status.isEqualTo(GameStatus.LOBBY).and(currentPlayerCount.lessThan(maxPlayers)));
    }

    public boolean isJoinable() {
        return joinable.get();
    }

    protected void updateItemFromSource(GameServer source) {
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

    @Override
    protected void disposeItem() {
        joinable.unbind();
    }
}
