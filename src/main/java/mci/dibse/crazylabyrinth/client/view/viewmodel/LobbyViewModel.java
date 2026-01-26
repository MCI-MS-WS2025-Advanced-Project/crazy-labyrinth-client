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
import mci.dibse.crazylabyrinth.client.infrastructure.util.ResultsPlayerInfoLookupHelper;
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

    @Value("${game.default.bonus.count:0}")
    private int GAME_DEFAULT_BONUS_COUNT;

    @Value("${game.default.treasure.count.per.player:6}")
    private int GAME_DEFAULT_TREASURE_COUNT_PER_PLAYER;

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

    private ResultsPlayerInfoLookupHelper playerInfoLookupMap;

    @Autowired
    public LobbyViewModel(
            NavigationStateViewModel navState,
            LobbyService lobbyService,
            ClientCommandService clientCommandService,
            ResultsPlayerInfoLookupHelper playerInfoLookupMap) {
        super(navState);
        this.commandService = clientCommandService;
        this.lobbyService = lobbyService;
        this.playerInfoLookupMap = playerInfoLookupMap;
    }

    /**
     * Register lobby listeners after construction.
     */
    @PostConstruct
    public void initLobbyViewModel() {
        lobbyService.addPlayerListener(this);
        lobbyService.addLobbyListener(this);
    }

    /**
     * Return the observable list of lobby players.
     *
     * @return players list
     */
    public ObservableList<PlayerInfoItem> getPlayersListProperty() {
        return lobbyStateItem.getPlayerList();
    }

    /**
     * Return the game name property.
     *
     * @return game name property
     */
    public StringProperty getGameNameProperty() {
        return lobbyStateItem.getServerName();
    }

    /**
     * Return the max players property.
     *
     * @return max players property
     */
    public StringProperty getMaxPlayersProperty() {
        return lobbyStateItem.getMaxPlayers();
    }

    /**
     * Return the current player count property.
     *
     * @return player count property
     */
    public StringProperty getPlayerCountProperty() {
        return lobbyStateItem.getPlayerCount();
    }

    /**
     * Check whether the client player is an admin.
     *
     * @return true when the client player is an admin
     */
    public boolean isClientAdmin() {
        if (clientPlayer == null || lobbyStateItem == null) return false;
        Optional<PlayerInfoItem> clientPlayerInfo =
                lobbyStateItem.getPlayerList().stream()
                        .filter(
                                player -> {
                                    return player.getId().equals(clientPlayer.getId());
                                })
                        .findFirst();
        if (clientPlayerInfo.isPresent()) return clientPlayerInfo.get().getIsAdmin();
        return false;
    }

    /**
     * Return whether bonuses are enabled by default.
     *
     * @return true when bonuses are enabled
     */
    public boolean isDefaultBonusEnabled() {
        return GAME_DEFAULT_BONUS_ENABLED;
    }

    public int getGameDefaultBonusCount() {
        return GAME_DEFAULT_BONUS_COUNT;
    }

    public int getGameDefaultTreasureCountPerPlayer() {
        return GAME_DEFAULT_TREASURE_COUNT_PER_PLAYER;
    }

    /**
     * Return the default board size.
     *
     * @return default board size
     */
    public int getDefaultBoardSize() {
        return GAME_DEFAULT_BOARD_SIZE;
    }

    /**
     * Return the default timer duration in seconds.
     *
     * @return default timer
     */
    public int getDefaultTimer() {
        return GAME_DEFAULT_TIMER;
    }

    /**
     * Return whether the timer is enabled by default.
     *
     * @return true when the timer is enabled
     */
    public boolean isDefaultTimerEnabled() {
        return GAME_DEFAULT_TIMER_ENABLED;
    }

    /**
     * Return whether results are currently available.
     *
     * @return true when results are available
     */
    public boolean isResultsAvailable() {
        return isResultsAvailable.get();
    }

    /**
     * Return the ranking entries from the game over item.
     *
     * @return ranking entries list
     */
    public ObservableList<RankingEntry> getRankingEntries() {
        return gameOverItem.getRankingEntries();
    }

    /**
     * Return the sorted ranking entries.
     *
     * @return sorted ranking list
     */
    public SortedList<RankingEntry> getSortedRankingEntries() {
        return gameOverItem.getSortedRankings();
    }

    /**
     * Return the winner id from the game over item.
     *
     * @return winner id
     */
    public String getWinnerId() {
        return gameOverItem.getWinnerIdProperty().get();
    }

    /**
     * Store the current game info record.
     *
     * @param gameInfo game info payload
     */
    @EventListener
    public void saveGameInfoData(GameInfoRecord gameInfo) {
        this.gameInfo = gameInfo;
    }

    /**
     * Navigate back to the main screen when leaving the lobby.
     *
     * @param event application event
     */
    @EventListener
    public void handleLobbyNavigationEvent(ApplicationEvents event) {
        if (event == BACK_TO_GAME_SERVER_DASHBOARD) {
            navigationStateViewModel.navigateTo(NavigationStateViewModel.ScreenId.MAIN);
        }
    }

    /**
     * Handle game over events and expose results.
     *
     * @param gameOverEvent game over payload
     */
    @EventListener
    public void handleGameOverEvent(GameOverEventPayload gameOverEvent) {
        log.debug("Game Over Event received: {}", gameOverEvent);
        gameOverItem = new GameOverItem(gameOverEvent);
        isResultsAvailable.set(true);
    }

    /**
     * Apply lobby state updates and initialize view state if needed.
     *
     * @param lobbyState lobby state payload
     */
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

    /**
     * Store the connected client player information.
     *
     * @param clientPlayer connected player payload
     */
    @Override
    public void onPlayerConnected(ClientPlayer clientPlayer) {
        this.clientPlayer = clientPlayer;
    }

    /**
     * Dispose listeners and reset lobby state.
     */
    @Override
    public void cleanup() {
        ObservableList<PlayerInfoItem> playersListProperty = getPlayersListProperty();
        playerListListener.forEach(playersListProperty::removeListener);
        //  this.clientPlayer = null;
        lobbyStateItem.dispose();
        this.lobbyStateItem = null;

        isResultsAvailable.removeListener(resultsListener);
        isResultsAvailable.set(false);
        if (gameOverItem != null) gameOverItem.dispose();
        gameOverItem = null;
    }

    /**
     * Send a start game command with the selected settings.
     *
     * @param startGameRecord game start settings
     */
    public void startGame(StartGameRecord startGameRecord) {
        try {
            commandService.sendStartGameCommand(startGameRecord);
        } catch (GeneralUiException e) {
            setError(e.getMessage());
        }
    }

    /**
     * Leave the lobby by disconnecting from the server.
     */
    public void leaveLobby() {
        try {
            commandService.sendDisconnectCommand();
        } catch (GeneralUiException e) {
            setError(e.getMessage());
        }
    }

    /**
     * Add a listener to the players list.
     *
     * @param countDownTimerListener listener to add
     */
    public void addListenerToPlayersListProperty(
            ListChangeListener<PlayerInfoItem> countDownTimerListener) {
        this.getPlayersListProperty().addListener(countDownTimerListener);
        playerListListener.add(countDownTimerListener);
    }

    /**
     * Add a listener for results availability updates.
     *
     * @param resultsListener listener to add
     */
    public void addListenerToResultsAvailableProperty(ChangeListener<Boolean> resultsListener) {
        this.resultsListener = resultsListener;
        isResultsAvailable.addListener(resultsListener);
        if (isResultsAvailable.get()) resultsListener.changed(isResultsAvailable, false, true);
    }

    /**
     * Determine whether errors should be shown in the lobby.
     *
     * @return true when the lobby screen is active
     */
    @Override
    protected boolean isErrorRelevant() {
        return NavigationStateViewModel.ScreenId.LOBBY.equals(
                navigationStateViewModel.getCurrentScreenProperty().get());
    }

    /**
     * Apply game info details to the lobby state.
     */
    private void setUpGameInfo() {
        lobbyStateItem.updateFromGameInfo(gameInfo);
    }

    /**
     * Check whether the client is an admin based on lobby state.
     *
     * @param lobbyState lobby state payload
     */
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

    /**
     * Store lobby state and navigate to the lobby screen.
     *
     * @param lobbyStateItem lobby state item
     */
    private void setUpLobby(LobbyStateItem lobbyStateItem) {
        this.lobbyStateItem = lobbyStateItem;
        navigationStateViewModel.navigateTo(NavigationStateViewModel.ScreenId.LOBBY);
    }

    /**
     * Convert lobby payload players to a list.
     *
     * @param lobbyState lobby state payload
     * @return list of player info
     */
    private List<PlayerInfo> mapPlayerInfoToList(LobbyStateEventPayload lobbyState) {
        return Arrays.asList(lobbyState.getPlayers());
    }

    /**
     * Decide whether to run cleanup when leaving the lobby.
     *
     * @param oldValue previous screen
     * @param newValue new screen
     * @return true when cleanup should run
     */
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

    /**
     * Return the player name for a given player id.
     *
     * @param item player id
     * @return player name or the id when not found
     */
    public String getPlayerNameForId(String item) {
        Optional<PlayerInfoItem> playerOptional =
                lobbyStateItem.getPlayerList().stream()
                        .filter(player -> player.getId().equals(item))
                        .findFirst();
        if (playerOptional.isPresent()) {
            return playerOptional.get().getName();
        }
        Optional<String> name = lookUpQueueForPlayerInfo(item);
        if (name.isPresent()) return name.get();

        log.info("Player with id {} not found in lobby", item);
        return item;
    }

    private Optional<String> lookUpQueueForPlayerInfo(String id) {
        return playerInfoLookupMap.lookUpCachedPlayerInfo(id);
    }

    /**
     * Bind the ranking comparator for the results list.
     *
     * @param comparatorReadOnlyObjectProperty comparator to bind
     */
    public void bindSortedRankingsComparator(
            ReadOnlyObjectProperty<Comparator<RankingEntry>> comparatorReadOnlyObjectProperty) {
        gameOverItem.bindSortedRankingsComparator(comparatorReadOnlyObjectProperty);
    }
}
