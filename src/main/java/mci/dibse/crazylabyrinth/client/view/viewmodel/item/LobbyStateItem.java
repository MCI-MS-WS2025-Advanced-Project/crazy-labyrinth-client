package mci.dibse.crazylabyrinth.client.view.viewmodel.item;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import labyrinth.contracts.models.EventType;
import mci.dibse.crazylabyrinth.client.domain.model.GameServer;
import mci.dibse.crazylabyrinth.client.domain.model.LobbyState;
import mci.dibse.crazylabyrinth.client.domain.model.PlayerInfo;

import java.util.List;
import java.util.Map;

public class LobbyStateItem {

    private final java.util.concurrent.atomic.AtomicBoolean pendingUpdate =
            new java.util.concurrent.atomic.AtomicBoolean(false);


    private final ListProperty<PlayerInfo> players = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ObjectProperty<Map<String, Object>> additionalProperties = new SimpleObjectProperty<>();


    public LobbyStateItem(LobbyState source) {
        updateFrom(source);

     //   this.strongListener = evt -> updateFrom(source);
      //  source.addListener(strongListener);
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

    private void updateItemFromSource(LobbyState source) {
        players.set(FXCollections.observableList(source.getPlayers()));
        additionalProperties.set(source.getAdditionalProperties());
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