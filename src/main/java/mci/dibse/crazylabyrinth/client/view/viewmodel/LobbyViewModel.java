package mci.dibse.crazylabyrinth.client.view.viewmodel;

import static mci.dibse.crazylabyrinth.client.domain.context.ApplicationEvents.BACK_TO_GAME_SERVER_DASHBOARD;

import jakarta.annotation.PostConstruct;

import java.util.*;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import labyrinth.contracts.models.*;
import lombok.extern.slf4j.Slf4j;
import mci.dibse.crazylabyrinth.client.domain.context.ApplicationEvents;
import mci.dibse.crazylabyrinth.client.domain.model.ClientPlayer;
import mci.dibse.crazylabyrinth.client.domain.model.record.GameInfoRecord;
import mci.dibse.crazylabyrinth.client.domain.model.record.StartGameRecord;
import mci.dibse.crazylabyrinth.client.infrastructure.exceptions.GeneralUiException;
import mci.dibse.crazylabyrinth.client.service.ClientCommandService;
import mci.dibse.crazylabyrinth.client.service.LobbyService;
import mci.dibse.crazylabyrinth.client.service.LobbyStateUpdateListener;
import mci.dibse.crazylabyrinth.client.service.PlayerConnectedAckListener;
import mci.dibse.crazylabyrinth.client.view.viewmodel.item.GameOverItem;
import mci.dibse.crazylabyrinth.client.view.viewmodel.item.LobbyStateItem;
import mci.dibse.crazylabyrinth.client.view.viewmodel.item.PlayerInfoItem;
import mci.dibse.crazylabyrinth.client.view.viewmodel.navigation.NavigationStateViewModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LobbyViewModel extends BaseViewModel
        implements LobbyStateUpdateListener, PlayerConnectedAckListener {


    public final SimpleBooleanProperty isResultsAvailable = new SimpleBooleanProperty(false);


    @Value("${game.default.bonus.enabled:false}")
    private boolean GAME_DEFAULT_BONUS_ENABLED;

    @Value("${game.default.board.size:7}")
    private int GAME_DEFAULT_BOARD_SIZE;

    @Value("${game.default.timer.ifEnabled:3600}")
    private int GAME_DEFAULT_TIMER;

    @Value("${game.default.timer.setting.enabled:false}")
    private boolean GAME_DEFAULT_TIMER_ENABLED;

    private ClientCommandService commandService;
    private final LobbyService lobbyService;

    private GameInfoRecord gameInfo;
    private ClientPlayer clientPlayer;

    private LobbyStateItem lobbyStateItem = null;
    private List<ListChangeListener<PlayerInfoItem>> playerListListener = new ArrayList<>();

    private GameOverItem gameOverItem = null;
    private ChangeListener<Boolean> resultsListener;

    @Autowired
    public LobbyViewModel(
            NavigationStateViewModel navState,
            LobbyService lobbyService,
            ClientCommandService clientCommandService) {
        super(navState);
        this.commandService = clientCommandService;
        this.lobbyService = lobbyService;
    }

    @PostConstruct
    public void initLobbyViewModel() {
        lobbyService.addPlayerListener(this);
        lobbyService.addLobbyListener(this);
    }

    public ObservableList<PlayerInfoItem> getPlayersListProperty() {
        return lobbyStateItem.getPlayerList();
    }

    public StringProperty getGameNameProperty() {
        return lobbyStateItem.getServerName();
    }

    public StringProperty getMaxPlayersProperty() {
        return lobbyStateItem.getMaxPlayers();
    }

    public StringProperty getPlayerCountProperty() {
        return lobbyStateItem.getPlayerCount();
    }

    public boolean isClientAdmin() {
        if (clientPlayer == null) return false;
        PlayerInfoItem clientPlayerInof =
                lobbyStateItem.getPlayerList().stream()
                        .filter(
                                player -> {
                                    return player.getId().equals(clientPlayer.getId());
                                })
                        .findFirst()
                        .orElseThrow();
        return clientPlayerInof.getIsAdmin();
    }

    public boolean isDefaultBonusEnabled() {
        return GAME_DEFAULT_BONUS_ENABLED;
    }

    public int getDefaultBoardSize() {
        return GAME_DEFAULT_BOARD_SIZE;
    }

    public int getDefaultTimer() {
        return GAME_DEFAULT_TIMER;
    }

    public boolean isDefaultTimerEnabled() {
        return GAME_DEFAULT_TIMER_ENABLED;
    }

    public boolean isResultsAvailable() {
        return isResultsAvailable.get();
    }

    public ObservableList<RankingEntry> getRankingEntries() {
        return gameOverItem.getRankingEntries();
    }
    public SortedList<RankingEntry> getSortedRankingEntries() {
        return gameOverItem.getSortedRankings();
    }

    public String getWinnerId() {
        return gameOverItem.getWinnerIdProperty().get();
    }

    @EventListener
    public void saveGameInfoData(GameInfoRecord gameInfo) {
        this.gameInfo = gameInfo;
    }

    @EventListener
    public void handleLobbyNavigationEvent(ApplicationEvents event) {
        if (event == BACK_TO_GAME_SERVER_DASHBOARD) {
            navigationStateViewModel.navigateTo(NavigationStateViewModel.ScreenId.MAIN);
        }
    }

    @EventListener
    public void handleGameOverEvent(GameOverEventPayload gameOverEvent) {
        log.debug("Game Over Event received: {}", gameOverEvent);
        gameOverItem = new GameOverItem(gameOverEvent);
        isResultsAvailable.set(true);
    }


    @Override
    public void onLobbyStateUpdated(LobbyStateEventPayload lobbyState) {
        // checkAndSetIfClientIsAdmin(lobbyState);
        log.debug("Lobby State updated");
        if (lobbyStateItem == null
                || !navigationStateViewModel
                        .getCurrentScreenProperty()
                        .get()
                        .equals(NavigationStateViewModel.ScreenId.LOBBY)) {
            LobbyStateItem item = new LobbyStateItem(lobbyState);
            setUpLobby(item);
            setUpGameInfo();
            return;
        }
        lobbyStateItem.updateFrom(lobbyState);
    }

    @Override
    public void onPlayerConnected(ClientPlayer clientPlayer) {
        this.clientPlayer = clientPlayer;
    }

    @Override
    public void cleanup() {
        ObservableList<PlayerInfoItem> playersListProperty = getPlayersListProperty();
        playerListListener.forEach(playersListProperty::removeListener);
        //  this.clientPlayer = null;
        lobbyStateItem.dispose();
        this.lobbyStateItem = null;

        isResultsAvailable.removeListener(resultsListener);
        isResultsAvailable.set(false);
        if(gameOverItem != null) gameOverItem.dispose();
        gameOverItem = null;

    }

    public void startGame(StartGameRecord startGameRecord) {
        try {
            commandService.sendStartGameCommand(startGameRecord);
        } catch (GeneralUiException e) {
            setError(e.getMessage());
        }
    }

    public void leaveLobby() {
        try {
            commandService.sendDisconnectCommand();
        } catch (GeneralUiException e) {
            setError(e.getMessage());
        }
    }

    public void addListenerToPlayersListProperty(
            ListChangeListener<PlayerInfoItem> countDownTimerListener) {
        this.getPlayersListProperty().addListener(countDownTimerListener);
        playerListListener.add(countDownTimerListener);
    }

    public void addListenerToResultsAvailableProperty(ChangeListener<Boolean> resultsListener) {
        this.resultsListener = resultsListener;
        isResultsAvailable.addListener(resultsListener);
        if (isResultsAvailable.get()) resultsListener.changed(isResultsAvailable, false, true);
    }

    @Override
    protected boolean isErrorRelevant() {
        return NavigationStateViewModel.ScreenId.LOBBY.equals(
                navigationStateViewModel.getCurrentScreenProperty().get());
    }

    private void setUpGameInfo() {
        lobbyStateItem.updateFromGameInfo(gameInfo);
    }

    private void checkAndSetIfClientIsAdmin(LobbyStateEventPayload lobbyState) {
        String clientId = clientPlayer.getId();
        mapPlayerInfoToList(lobbyState)
                .forEach(
                        player -> {
                            if (player.getId().equals(clientId)) {
                                // clientPlayer.setIsAdmin(player.getIsAdmin());
                            }
                        });
    }

    private void setUpLobby(LobbyStateItem lobbyStateItem) {
        this.lobbyStateItem = lobbyStateItem;
        navigationStateViewModel.navigateTo(NavigationStateViewModel.ScreenId.LOBBY);
    }

    private List<PlayerInfo> mapPlayerInfoToList(LobbyStateEventPayload lobbyState) {
        return Arrays.asList(lobbyState.getPlayers());
    }

    @Override
    protected boolean customCleanupCheckpoint(
            NavigationStateViewModel.ScreenId oldValue,
            NavigationStateViewModel.ScreenId newValue) {

        if (NavigationStateViewModel.ScreenId.LOBBY.equals(oldValue)
                && NavigationStateViewModel.ScreenId.MAIN.equals(newValue))
            this.clientPlayer = null;

        return newValue != null
                && NavigationStateViewModel.ScreenId.LOBBY.equals(oldValue)
                && !NavigationStateViewModel.ScreenId.LOBBY.equals(newValue);
    }


    public String getPlayerNameForId(String item) {
        Optional<PlayerInfoItem> playerOptional = lobbyStateItem.getPlayerList().stream()
                .filter(player -> player.getId().equals(item))
                .findFirst();
        if (playerOptional.isPresent()) {
            return playerOptional.get().getName();
        }
        log.info("Player with id {} not found in lobby", item);
        return item;
    }

    public void bindSortedRankingsComparator(ReadOnlyObjectProperty<Comparator<RankingEntry>> comparatorReadOnlyObjectProperty) {
        gameOverItem.bindSortedRankingsComparator(comparatorReadOnlyObjectProperty);
    }
}
