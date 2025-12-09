package mci.dibse.crazylabyrinth.client.view.viewmodel;

import java.net.URI;
import java.util.*;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import lombok.Getter;
import mci.dibse.crazylabyrinth.client.domain.context.ApplicationEvents;
import mci.dibse.crazylabyrinth.client.domain.model.GameServer;
import mci.dibse.crazylabyrinth.client.domain.model.enums.GameStatus;
import mci.dibse.crazylabyrinth.client.domain.model.record.GameInfoRecord;
import mci.dibse.crazylabyrinth.client.infrastructure.exceptions.GeneralUiException;
import mci.dibse.crazylabyrinth.client.service.ClientCommandService;
import mci.dibse.crazylabyrinth.client.service.GameServerManagementService;
import mci.dibse.crazylabyrinth.client.service.GameServerSocketService;
import mci.dibse.crazylabyrinth.client.service.GameServerUpdateListener;
import mci.dibse.crazylabyrinth.client.view.viewmodel.item.GameServerItem;
import mci.dibse.crazylabyrinth.client.view.viewmodel.navigation.NavigationStateViewModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import static mci.dibse.crazylabyrinth.client.domain.context.ApplicationEvents.BACK_TO_GAME_SERVER_DASHBOARD;

/**
 *  @author Julian Janetschek
 */
@Component
public class GameServerDashboardViewModel implements GameServerUpdateListener {


    @Getter private final StringProperty errorMessage = new SimpleStringProperty();
    @Getter private final BooleanProperty hasError = new SimpleBooleanProperty(false);
    @Getter private final BooleanProperty isRefreshing = new SimpleBooleanProperty(false);

    private final ObservableMap<UUID, GameServerItem> byId = FXCollections.observableHashMap();
    private final ObservableList<GameServerItem> items;
    private final FilteredList<GameServerItem> filtered;
    @Getter private final SortedList<GameServerItem> sorted;

    private final BooleanProperty onlyJoinable = new SimpleBooleanProperty(false);

    private final GameServerManagementService managementServiceservice;
    private final ClientCommandService commandService;

    @Autowired
    public GameServerDashboardViewModel(GameServerManagementService service, ClientCommandService commandService) {
        this.managementServiceservice = service;
        this.commandService = commandService;
        this.managementServiceservice.addListener(this);
        items =
                FXCollections.observableArrayList(
                        item ->
                                new Observable[] {
                                    item.nameProperty(),
                                    item.statusProperty(),
                                    item.currentPlayerCountProperty(),
                                    item.lastSeenProperty()
                                });
        filtered = new FilteredList<>(items);
        sorted = new SortedList<>(filtered);

        onlyJoinable.addListener((obs, o, n) -> filtered.setPredicate(this::matchesJoinable));
        filtered.setPredicate(this::matchesJoinable);

        // keep items list in sync with map
        byId.addListener(
                (MapChangeListener<UUID, GameServerItem>)
                        ch -> {
                            if (Platform.isFxApplicationThread()) {
                                if (ch.wasRemoved()) items.remove(ch.getValueRemoved());
                                if (ch.wasAdded()) items.add(ch.getValueAdded());
                            } else {
                                Platform.runLater(
                                        () -> {
                                            if (ch.wasRemoved()) items.remove(ch.getValueRemoved());
                                            if (ch.wasAdded()) items.add(ch.getValueAdded());
                                        });
                            }
                        });
        //refreshServerList();
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
            managementServiceservice.refreshGameServerList();
            setRefreshing(false);
        } catch (GeneralUiException e) {
            clearByIdMap();
            setError(e.getMessage());
        }
    }

    public void joinSelectedServer(GameInfoRecord gameInfo) {
        commandService.sendConnectCommand(gameInfo);
    }

/* too early
    @EventListener
    public void handleLobbyNavigationEvent(ApplicationEvents event) {
        if (event == BACK_TO_GAME_SERVER_DASHBOARD) {
                refreshServerList();
        }
    }
 */


    private void applyServer(Collection<GameServer> servers) {
        Runnable task =
                () -> {
                    for (GameServer server : servers) {
                        UUID id = server.getId();
                        GameServerItem item = byId.get(id);
                        if (item != null) {
                            item.updateFrom(server);
                        } else {
                            item = new GameServerItem(server);
                            byId.put(id, item);
                        }
                    }
                    Set<UUID> incoming =
                            servers.stream()
                                    .map(GameServer::getId)
                                    .collect(java.util.stream.Collectors.toSet());
                    byId.keySet().removeIf(id -> !incoming.contains(id));
                };
        if (javafx.application.Platform.isFxApplicationThread()) task.run();
        else javafx.application.Platform.runLater(task);
    }

    private boolean matchesJoinable(GameServerItem item) {
        if (!onlyJoinable.get()) return true;
        if (item == null) return false;
        boolean statusOk = item.getStatus().getValue().equals(GameStatus.LOBBY);
        boolean capacityOk =
                item.getCurrentPlayerCount().getValue() < item.getMaxPlayers().getValue();
        return statusOk && capacityOk;
    }

    private void clearByIdMap() {
        Runnable apply = () -> byId.clear();
        if (Platform.isFxApplicationThread()) apply.run();
        else Platform.runLater(apply);
    }

    private void clearError() {
        Runnable apply =
                () -> {
                    errorMessage.set(null);
                    hasError.set(false);
                };
        if (Platform.isFxApplicationThread()) apply.run();
        else Platform.runLater(apply);
    }

    private void setError(String message) {
        Runnable apply =
                () -> {
                    errorMessage.set(message);
                    hasError.set(true);
                    isRefreshing.set(false);
                };
        if (Platform.isFxApplicationThread()) apply.run();
        else Platform.runLater(apply);
    }

    private void setRefreshing(boolean refreshing) {
        Runnable apply =
                () -> {
                    isRefreshing.set(refreshing);
                    if (refreshing) {
                        hasError.set(false);
                        errorMessage.set(null);
                    }
                };
        if (Platform.isFxApplicationThread()) apply.run();
        else Platform.runLater(apply);
    }
}
