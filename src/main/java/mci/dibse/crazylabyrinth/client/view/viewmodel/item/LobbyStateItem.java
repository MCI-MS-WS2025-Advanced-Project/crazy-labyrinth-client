package mci.dibse.crazylabyrinth.client.view.viewmodel.item;

import javafx.beans.Observable;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import lombok.Getter;
import lombok.Setter;
import mci.dibse.crazylabyrinth.client.domain.model.LobbyState;
import mci.dibse.crazylabyrinth.client.domain.model.PlayerInfo;
import mci.dibse.crazylabyrinth.client.domain.model.record.GameInfoRecord;

import java.util.List;
import java.util.Map;

public class LobbyStateItem implements Disposable{

    private final java.util.concurrent.atomic.AtomicBoolean pendingUpdate =
            new java.util.concurrent.atomic.AtomicBoolean(false);


    private final ObservableMap<String, PlayerInfoItem> byId = FXCollections.observableHashMap();

    @Setter @Getter
    private ObservableList<PlayerInfoItem> playerList = FXCollections.observableArrayList(item -> new Observable[] {
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
    private MapChangeListener<String, PlayerInfoItem> mapChangeListener;



    public LobbyStateItem(LobbyState source) {
        mapChangeListener = (MapChangeListener<String, PlayerInfoItem>) change -> {
            Runnable apply = () -> {
                if (change.wasAdded()) playerList.add(change.getValueAdded());
                if (change.wasRemoved()) playerList.remove(change.getValueRemoved());
                this.playerCount.set(String.valueOf(playerList.size()));

                System.out.println("LobbyStateItem: MapChangeListener: palsyerlist" + playerList.size());
                 System.out.println("LobbyStateItem: MapChangeListener: playerCount" + playerCount.getValue());
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

    @Override
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
        System.out.println("LobbyStateItem: updateFromSource");
        List<PlayerInfo> playerInfos = source.getPlayers();
        // Remove players that are no longer in the source list
        byId.keySet().removeIf(playerId ->
                playerInfos.stream().noneMatch(info -> info.getId().equals(playerId)));

       System.out.println("LobbyStateItem: incoming playerInfos: " + playerInfos.size());

        this.playerCount.set(String.valueOf(playerInfos.size()));
        playerInfos.stream()
                .map(this::playerItemFromPlayerInfo)
                .forEach(item -> {byId.putIfAbsent(item.getId(), item);});

        System.out.println("LobbyStateItem: byId: " + byId.size());
        System.out.println("LobbyStateItem: playerList: " + playerList.size());

        additionalProperties.set(source.getAdditionalProperties());
    }

    private PlayerInfoItem playerItemFromPlayerInfo(PlayerInfo playerInfo){
        return new PlayerInfoItem(playerInfo);
    }


}