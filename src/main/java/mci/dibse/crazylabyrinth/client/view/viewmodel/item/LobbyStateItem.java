package mci.dibse.crazylabyrinth.client.view.viewmodel.item;

import javafx.beans.Observable;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import labyrinth.contracts.models.EventType;
import lombok.Getter;
import lombok.Setter;
import mci.dibse.crazylabyrinth.client.domain.model.GameServer;
import mci.dibse.crazylabyrinth.client.domain.model.LobbyState;
import mci.dibse.crazylabyrinth.client.domain.model.PlayerInfo;
import mci.dibse.crazylabyrinth.client.domain.model.record.GameInfoRecord;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LobbyStateItem {

    private final java.util.concurrent.atomic.AtomicBoolean pendingUpdate =
            new java.util.concurrent.atomic.AtomicBoolean(false);


    private final ObservableMap<String, PlayerItem> byId = FXCollections.observableHashMap();

    @Setter @Getter
    private ObservableList<PlayerItem> playerList = FXCollections.observableArrayList(item -> new Observable[] {
            item.idProperty(),
            item.nameProperty(),
            item.colorProperty(),
            item.isAdminProperty(),
            item.isAdminProperty()
    });

    // TODO observable needed?
    private final ObjectProperty<Map<String, Object>> additionalProperties = new SimpleObjectProperty<>();

    @Getter
    private final StringProperty playerCount = new SimpleStringProperty();
    @Getter
    private final StringProperty maxPlayers = new SimpleStringProperty();
    @Getter
    private final StringProperty serverName = new SimpleStringProperty();
    private MapChangeListener<String, PlayerItem> mapChangeListener;



    public LobbyStateItem(LobbyState source) {
        mapChangeListener = (MapChangeListener<String, PlayerItem>) change -> {
            Runnable apply = () -> {
                if (change.wasAdded()) playerList.add(change.getValueAdded());
                if (change.wasRemoved()) playerList.remove(change.getValueRemoved());
                this.playerCount.set(String.valueOf(playerList.size()));
            };
            if (javafx.application.Platform.isFxApplicationThread()) apply.run();
            else javafx.application.Platform.runLater(apply);
        };

        byId.addListener(mapChangeListener);

        updateFrom(source);

        //TODO neded?
      // this.strongListener = evt -> updateFrom(source);
      // source.addListener(strongListener);
    }


    public void dispose() {
        // Remove listener to prevent memory leaks
        byId.removeListener(mapChangeListener);

        // Clear collections and remove listeners
        byId.clear();
        playerList.clear();

        // Clear properties
        playerCount.set(null);
        maxPlayers.set(null);
        serverName.set(null);
        additionalProperties.set(null);

        // Clear references
        mapChangeListener = null;
        playerList = null;
    }


    public void updateFrom(LobbyState source) {
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

    public void updateFromGameInfo(GameInfoRecord info) {
        this.serverName.set(info.serverName());
        this.maxPlayers.set(info.maxPlayers());
    }

    private void updateItemFromSource(LobbyState source) {
       List<PlayerInfo> playerInfos =  source.getPlayers();
        this.playerCount.set(String.valueOf(playerInfos.size()));
        playerInfos.stream()
                .map(this::playerItemFromPlayerInfo)
                .forEach(item -> {byId.put(item.getId(), item);});

        additionalProperties.set(source.getAdditionalProperties());
    }

    private PlayerItem playerItemFromPlayerInfo(PlayerInfo playerInfo){
        return new PlayerItem(playerInfo);
    }


}