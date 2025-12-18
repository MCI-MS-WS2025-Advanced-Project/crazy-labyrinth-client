package mci.dibse.crazylabyrinth.client.view.viewmodel;

import static mci.dibse.crazylabyrinth.client.domain.context.ApplicationEvents.BACK_TO_GAME_SERVER_DASHBOARD;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javafx.beans.property.StringProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import labyrinth.contracts.models.LobbyStateEventPayload;
import labyrinth.contracts.models.PlayerInfo;
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
        return clientPlayer != null && clientPlayer.getIsAdmin();
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

    @Override
    public void onLobbyStateUpdated(LobbyStateEventPayload lobbyState) {
        checkAndSetIfClientIsAdmin(lobbyState);
        if (lobbyStateItem == null
                || !navigationStateViewModel
                        .getCurrentScreen()
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
        this.clientPlayer = null;
        lobbyStateItem.dispose();
        this.lobbyStateItem = null;
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

    @Override
    protected boolean isErrorRelevant() {
        return NavigationStateViewModel.ScreenId.LOBBY.equals(
                navigationStateViewModel.getCurrentScreen().get());
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
                                clientPlayer.setIsAdmin(player.getIsAdmin());
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
        return newValue != null
                && NavigationStateViewModel.ScreenId.LOBBY.equals(oldValue)
                && !NavigationStateViewModel.ScreenId.LOBBY.equals(newValue);
    }

    public void addListenerToPlayersListProperty(
            ListChangeListener<PlayerInfoItem> countDownTimerListener) {
        this.getPlayersListProperty().addListener(countDownTimerListener);
        playerListListener.add(countDownTimerListener);
    }
}
