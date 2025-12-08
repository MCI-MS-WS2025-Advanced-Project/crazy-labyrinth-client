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



    public LobbyStateItem(LobbyState source) {
        byId.addListener((MapChangeListener<? super String, ? super PlayerItem>) change -> {
            Runnable apply = () -> {
                if (change.wasAdded()) playerList.add(change.getValueAdded());
                if (change.wasRemoved()) playerList.remove(change.getValueRemoved());
                this.playerCount.set(String.valueOf(playerList.size()));
            };
            if (javafx.application.Platform.isFxApplicationThread()) apply.run();
            else javafx.application.Platform.runLater(apply);
        });


        updateFrom(source);

        //TODO neded?
      // this.strongListener = evt -> updateFrom(source);
      // source.addListener(strongListener);
    }

    /*
    public void dispose(GameServer source) {
        source.removeListener(strongListener);
    }
     */

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


/*
private final ObjectProperty<Map<String, Object>> additionalProperties = new SimpleObjectProperty<>();
private final ObservableMap<UUID, GameServerItem> byId = FXCollections.observableHashMap();

 private final ListProperty<PlayerInfo> players = new SimpleListProperty<>(FXCollections.observableArrayList());
private final ObservableList<GameServerItem> items;

### **ObservableList**

- **Direct observable collection**: The list itself is observable
- **Fine-grained notifications**: Fires events for individual list operations (add, remove, set)
- **Direct manipulation**: Direct access to list methods
- **Event types**: Provides detailed `ListChangeListener.Change` events

### **ListProperty**
- **Property wrapper**: Wraps an ObservableList as a property
- **Dual notification**: Fires both property change events AND list change events
- **Property features**: Supports binding, can be bound to other properties
- **Best of both**: Combines property capabilities with list observability

ListProperty<PlayerInfo> players = new SimpleListProperty<>(FXCollections.observableArrayList());
players.add(player); // Direct list manipulation (delegated to wrapped list)
players.addListener((obs, oldList, newList) -> {}); // Property change listener
players.addListener((ListChangeListener<PlayerInfo>) change -> {}); // List change listener


private final ObservableMap<UUID, GameServerItem> byId = FXCollections.observableHashMap();
### **ObservableMap<K, V>**
- **Direct observable collection**: The map itself is observable
- **Fine-grained notifications**: Fires events for individual map operations (put, remove, clear)
- **Direct manipulation**: You can directly call `put()`, `remove()`, `clear()` on the map
- **Event types**: Provides specific change events like `MapChangeListener.Change`

byId.put(uuid, item); // Direct manipulation
byId.addListener((MapChangeListener<UUID, GameServerItem>) change -> {
    // Detailed change information available
});


### **ObjectProperty<Map<K, V>>**
- **Property wrapper**: Wraps a Map object as a property
- **Coarse-grained notifications**: Fires events only when the entire Map reference changes
- **Indirect manipulation**: Must get the map, modify it, then potentially set it back
- **Property binding**: Can be bound to other properties, supports bidirectional binding
ObjectProperty<Map<String, Object>> additionalProperties = new SimpleObjectProperty<>();
additionalProperties.set(newMap); // Replaces entire map
additionalProperties.addListener((obs, oldMap, newMap) -> {
    // Only fires when map reference changes, not map content
});

 */