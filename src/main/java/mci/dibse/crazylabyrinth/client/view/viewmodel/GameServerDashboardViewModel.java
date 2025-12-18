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

    @Override
    protected boolean customCleanupCheckpoint(
            NavigationStateViewModel.ScreenId oldValue,
            NavigationStateViewModel.ScreenId newValue) {
        return newValue != null
                && NavigationStateViewModel.ScreenId.MAIN.equals(oldValue)
                && !NavigationStateViewModel.ScreenId.MAIN.equals(newValue);
    }

    public BooleanProperty onlyJoinableProperty() {
        return onlyJoinable;
    }

    @Override
    public void onServersUpdated(Collection<GameServer> servers) {
        applyServer(servers);
    }

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

    private void setupFilteringAndSorting() {
        filtered.setPredicate(this::matchesJoinable);
    }

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

    private Observable[] extractObservables(GameServerItem item) {
        return new Observable[] {
            item.nameProperty(),
            item.statusProperty(),
            item.currentPlayerCountProperty(),
            item.lastSeenProperty()
        };
    }

    private void updateListFromChange(
            MapChangeListener.Change<? extends UUID, ? extends GameServerItem> ch) {
        if (ch.wasRemoved()) items.remove(ch.getValueRemoved());
        if (ch.wasAdded()) items.add(ch.getValueAdded());
    }

    private void applyServer(Collection<GameServer> servers) {
        runOnFxThread(
                () -> {
                    upsertServers(servers);
                    removeStaleServers(servers);
                });
    }

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

    private void removeStaleServers(Collection<GameServer> servers) {
        Set<UUID> incomingIds =
                servers.stream()
                        .map(GameServer::getId)
                        .collect(java.util.stream.Collectors.toSet());

        serverMap.keySet().removeIf(id -> !incomingIds.contains(id));
    }

    private boolean matchesJoinable(GameServerItem item) {
        if (!onlyJoinable.get()) return true;
        if (item == null) return false;

        boolean isLobby = GameStatus.LOBBY.equals(item.getStatus().getValue());
        boolean hasCapacity =
                item.getCurrentPlayerCount().getValue() < item.getMaxPlayers().getValue();
        return isLobby && hasCapacity;
    }

    private void clearServerMap() {
        runOnFxThread(serverMap::clear);
    }

    public void joinSelectedServer(GameInfoRecord gameInfo) {
        try {
            clientCommandService.sendConnectCommand(gameInfo);
        } catch (GeneralUiException e) {
            setError(e.getMessage());
        }
    }

    @Override
    protected boolean isErrorRelevant() {
        return NavigationStateViewModel.ScreenId.MAIN.equals(
                navigationStateViewModel.getCurrentScreen().get());
    }
}
