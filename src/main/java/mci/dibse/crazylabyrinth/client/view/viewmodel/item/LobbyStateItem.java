package mci.dibse.crazylabyrinth.client.view.viewmodel.item;

import java.util.Arrays;
import java.util.List;
import javafx.beans.Observable;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import labyrinth.contracts.models.LobbyStateEventPayload;
import labyrinth.contracts.models.PlayerInfo;
import lombok.Getter;
import lombok.Setter;
import mci.dibse.crazylabyrinth.client.domain.model.record.GameInfoRecord;
import mci.dibse.crazylabyrinth.client.infrastructure.util.JavaFXUtils;

public class LobbyStateItem extends BaseItem<LobbyStateEventPayload> {

    @Getter private final StringProperty playerCount = new SimpleStringProperty();
    @Getter private final StringProperty maxPlayers = new SimpleStringProperty();
    @Getter private final StringProperty serverName = new SimpleStringProperty();

    private final ObservableMap<String, PlayerInfoItem> byId = FXCollections.observableHashMap();
    private MapChangeListener<String, PlayerInfoItem> mapChangeListener;

    @Setter @Getter
    private ObservableList<PlayerInfoItem> playerList =
            FXCollections.observableArrayList(
                    item ->
                            new Observable[] {
                                item.idProperty(),
                                item.nameProperty(),
                                item.colorProperty(),
                                item.isAiControlledProperty(),
                                item.isAdminProperty()
                            });

    public LobbyStateItem(LobbyStateEventPayload source) {
        super();
        mapChangeListener = initMapChangeListener();
        Runnable apply = () -> {
            byId.addListener(this.mapChangeListener);
            updateFrom(source);
        };
        JavaFXUtils.runOnFxThread(apply);

    }

    @Override
    protected void disposeItem() {
        byId.removeListener(mapChangeListener);
        byId.clear();
        playerList.clear();
        mapChangeListener = null;
        playerList = null;

        playerCount.set(null);
        maxPlayers.set(null);
        serverName.set(null);
    }

    public void updateFromGameInfo(GameInfoRecord info) {
        this.serverName.set(info.serverName());
        this.maxPlayers.set(info.maxPlayers());
    }

    protected void updateItemFromSource(LobbyStateEventPayload source) {
        List<PlayerInfo> playerInfos = Arrays.asList(source.getPlayers());
        byId.keySet()
                .removeIf(
                        playerId ->
                                playerInfos.stream()
                                        .noneMatch(info -> info.getId().equals(playerId)));
        this.playerCount.set(String.valueOf(playerInfos.size()));
        playerInfos.stream()
                .map(this::playerItemFromPlayerInfo)
                .forEach(item -> byId.putIfAbsent(item.getId(), item));
    }

    private PlayerInfoItem playerItemFromPlayerInfo(PlayerInfo playerInfo) {
        return new PlayerInfoItem(playerInfo);
    }

    private MapChangeListener<String, PlayerInfoItem> initMapChangeListener() {
        return (MapChangeListener<String, PlayerInfoItem>)
                change -> {
                    Runnable apply =
                            () -> {
                                if (change.wasAdded()) playerList.add(change.getValueAdded());
                                if (change.wasRemoved())
                                    playerList.remove(change.getValueRemoved());
                                this.playerCount.set(String.valueOf(playerList.size()));
                            };
                    JavaFXUtils.runOnFxThread(apply);
                };
    }
}
