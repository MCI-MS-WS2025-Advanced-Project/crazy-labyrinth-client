package mci.dibse.crazylabyrinth.client.view.viewmodel;

import static mci.dibse.crazylabyrinth.client.infrastructure.util.JavaFXUtils.runOnFxThread;

import java.util.*;
import javafx.beans.Observable;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import lombok.Getter;
import mci.dibse.crazylabyrinth.client.domain.model.enums.GameStatus;
import mci.dibse.crazylabyrinth.client.domain.model.management.GameServer;
import mci.dibse.crazylabyrinth.client.domain.model.record.GameInfoRecord;
import mci.dibse.crazylabyrinth.client.infrastructure.exceptions.GeneralUiException;
import mci.dibse.crazylabyrinth.client.service.ClientCommandService;
import mci.dibse.crazylabyrinth.client.service.GameServerManagementService;
import mci.dibse.crazylabyrinth.client.service.GameServerUpdateListener;
import mci.dibse.crazylabyrinth.client.view.viewmodel.item.GameServerItem;
import mci.dibse.crazylabyrinth.client.view.viewmodel.navigation.NavigationStateViewModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * ViewModel for the Game Server Dashboard, responsible for managing the state of available game servers.
 * <p>
 * It provides an observable list of {@link GameServerItem}s with filtering and sorting capabilities,
 * synchronizes data with the {@link GameServerManagementService}, and exposes properties for UI status
 * feedback such as loading indicators and error messages.
 * </p>
 *
 *
 */
@Component
public class GameServerDashboardViewModel extends BaseViewModel
        implements GameServerUpdateListener {

    private final ObservableMap<UUID, GameServerItem> serverMap = FXCollections.observableHashMap();
    private final ObservableList<GameServerItem> items;
    private final FilteredList<GameServerItem> filtered;
    @Getter private final SortedList<GameServerItem> sorted;
    private final BooleanProperty onlyJoinable = new SimpleBooleanProperty(false);

    private final GameServerManagementService service;
    private final ClientCommandService clientCommandService;

    private ChangeListener<Boolean> joinableListener;
    private MapChangeListener<UUID, GameServerItem> serverMapListener;

    @Autowired
    public GameServerDashboardViewModel(
            GameServerManagementService service,
            NavigationStateViewModel navState,
            ClientCommandService clientCommandService,
            NavigationStateViewModel navigationStateViewModel) {

        super(navState);
        this.service = service;

        items = FXCollections.observableArrayList(this::extractObservables);
        filtered = new FilteredList<>(items);
        sorted = new SortedList<>(filtered);

        setupFilteringAndSorting();
        registerListeners();
        this.clientCommandService = clientCommandService;
        this.navigationStateViewModel = navigationStateViewModel;
    }

    // todo test
    /**
     * Dispose items and clear server state.
     */
    @Override
    public void cleanup() {
        Runnable cleanup =
                () -> {
                    serverMap.clear();
                    items.forEach(GameServerItem::dispose);
                    items.clear();
                };
        runOnFxThread(cleanup);
    }

    /**
     * Decide whether to run cleanup when leaving the main screen.
     *
     * @param oldValue previous screen
     * @param newValue new screen
     * @return true when cleanup should run
     */
    @Override
    protected boolean customCleanupCheckpoint(
            NavigationStateViewModel.ScreenId oldValue,
            NavigationStateViewModel.ScreenId newValue) {
        return newValue != null
                && NavigationStateViewModel.ScreenId.MAIN.equals(oldValue)
                && !NavigationStateViewModel.ScreenId.MAIN.equals(newValue);
    }

    /**
     * Expose the "only joinable" filter property.
     *
     * @return boolean property controlling joinable filtering
     */
    public BooleanProperty onlyJoinableProperty() {
        return onlyJoinable;
    }

    /**
     * Apply server updates from the management service.
     *
     * @param servers updated server list
     */
    @Override
    public void onServersUpdated(Collection<GameServer> servers) {
        applyServer(servers);
    }

    /**
     * Refresh the server list and update error state on failures.
     */
    public void refreshServerList() {
        setRefreshing(true);
        try {
            service.refreshGameServerList();
        } catch (GeneralUiException e) {
            clearServerMap();
            String message =
                    (Objects.nonNull(e.getErrorResponse()))
                            ? e.getErrorResponse().getMessage()
                            : e.getMessage();
            setError(message);
        } finally {
            setRefreshing(false);
        }
    }

    /**
     * Configure filtering based on joinable status.
     */
    private void setupFilteringAndSorting() {
        filtered.setPredicate(this::matchesJoinable);
    }

    /**
     * Register listeners for data changes and filter updates.
     */
    private void registerListeners() {
        this.service.addListener(this);
        // Keep items list in sync with map changes
        joinableListener = (obs, o, n) -> filtered.setPredicate(this::matchesJoinable);
        onlyJoinable.addListener(joinableListener);

        serverMapListener =
                (MapChangeListener<UUID, GameServerItem>)
                        ch -> runOnFxThread(() -> updateListFromChange(ch));
        serverMap.addListener(serverMapListener);
    }

    /**
     * Extract observables that should trigger UI updates for a server item.
     *
     * @param item server item
     * @return array of observable properties
     */
    private Observable[] extractObservables(GameServerItem item) {
        return new Observable[] {
            item.nameProperty(),
            item.statusProperty(),
            item.currentPlayerCountProperty(),
            item.lastSeenProperty()
        };
    }

    /**
     * Apply a map change to the observable list.
     *
     * @param ch map change event
     */
    private void updateListFromChange(
            MapChangeListener.Change<? extends UUID, ? extends GameServerItem> ch) {
        if (ch.wasRemoved()) items.remove(ch.getValueRemoved());
        if (ch.wasAdded()) items.add(ch.getValueAdded());
    }

    /**
     * Apply updates to the server list on the FX thread.
     *
     * @param servers incoming server list
     */
    private void applyServer(Collection<GameServer> servers) {
        runOnFxThread(
                () -> {
                    upsertServers(servers);
                    removeStaleServers(servers);
                });
    }

    /**
     * Update existing server items or insert new ones.
     *
     * @param servers incoming server list
     */
    private void upsertServers(Collection<GameServer> servers) {
        for (GameServer server : servers) {
            GameServerItem item = serverMap.get(server.getId());
            if (item != null) {
                item.updateFrom(server);
            } else {
                serverMap.put(server.getId(), new GameServerItem(server));
            }
        }
    }

    /**
     * Remove servers that are no longer present in the update.
     *
     * @param servers incoming server list
     */
    private void removeStaleServers(Collection<GameServer> servers) {
        Set<UUID> incomingIds =
                servers.stream()
                        .map(GameServer::getId)
                        .collect(java.util.stream.Collectors.toSet());

        serverMap.keySet().removeIf(id -> !incomingIds.contains(id));
    }

    /**
     * Determine whether a server matches the joinable filter.
     *
     * @param item server item to test
     * @return true when the server should be shown
     */
    private boolean matchesJoinable(GameServerItem item) {
        if (!onlyJoinable.get()) return true;
        if (item == null) return false;

        boolean isLobby = GameStatus.LOBBY.equals(item.getStatus().getValue());
        boolean hasCapacity =
                item.getCurrentPlayerCount().getValue() < item.getMaxPlayers().getValue();
        return isLobby && hasCapacity;
    }

    /**
     * Clear the server map on the FX thread.
     */
    private void clearServerMap() {
        runOnFxThread(serverMap::clear);
    }

    /**
     * Send a connect command for the selected server.
     *
     * @param gameInfo connection data for the target server
     */
    public void joinSelectedServer(GameInfoRecord gameInfo) {
        try {
            clientCommandService.sendConnectCommand(gameInfo);
        } catch (GeneralUiException e) {
            setError(e.getMessage());
        }
    }

    /**
     * Determine whether errors should be shown on the main screen.
     *
     * @return true when the main screen is active
     */
    @Override
    protected boolean isErrorRelevant() {
        return NavigationStateViewModel.ScreenId.MAIN.equals(
                navigationStateViewModel.getCurrentScreenProperty().get());
    }
}
