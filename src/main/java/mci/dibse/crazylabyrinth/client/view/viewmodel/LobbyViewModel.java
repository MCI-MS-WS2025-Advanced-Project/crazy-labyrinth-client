package mci.dibse.crazylabyrinth.client.view.viewmodel;

import jakarta.annotation.PostConstruct;
import javafx.beans.property.StringProperty;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mci.dibse.crazylabyrinth.client.domain.context.ApplicationEvents;
import mci.dibse.crazylabyrinth.client.domain.model.ClientPlayer;
import mci.dibse.crazylabyrinth.client.domain.model.LobbyState;
import mci.dibse.crazylabyrinth.client.domain.model.record.GameInfoRecord;
import mci.dibse.crazylabyrinth.client.domain.model.record.StartGameRecord;
import mci.dibse.crazylabyrinth.client.service.*;
import mci.dibse.crazylabyrinth.client.view.viewmodel.item.LobbyStateItem;
import mci.dibse.crazylabyrinth.client.view.viewmodel.item.PlayerInfoItem;
import mci.dibse.crazylabyrinth.client.view.viewmodel.navigation.NavigationStateViewModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

import static mci.dibse.crazylabyrinth.client.domain.context.ApplicationEvents.BACK_TO_GAME_SERVER_DASHBOARD;

@Component
@Slf4j
@NoArgsConstructor
public class LobbyViewModel implements LobbyStateUpdateListener, PlayerConnectedAckListener {



    //TODO defaults!?
    @Value("${game.default.bonus.enabled:false}")
    private boolean GAME_DEFAULT_BONUS_ENABLED;
    @Value("${game.default.board.size:7}")
    private int GAME_DEFAULT_BOARD_SIZE;
    @Value("${game.default.timer.ifEnabled:3600}")
    private int GAME_DEFAULT_TIMER;
    @Value("${game.default.timer.setting.enabled:false}")
    private boolean GAME_DEFAULT_TIMER_ENABLED;



    @Autowired
    private ClientCommandService commandService;
    @Autowired
    private NavigationStateViewModel navState;
    @Autowired
    private LobbyService lobbyService;

    private LobbyStateItem lobbyStateItem = null;

    private ClientPlayer clientPlayer;

    private GameInfoRecord gameInfo;

    @PostConstruct
    public void init(){
        lobbyService.addPlayerListener(this);
        lobbyService.addLobbyListener(this);
    }

    //todo after game  is ended --> server sned GAME_OVER & LOBBY_STATE  event
    // but when in result screen
    public void setUpLobby(LobbyStateItem lobbyStateItem){
        this.lobbyStateItem = lobbyStateItem;
        navState.navigateTo(NavigationStateViewModel.ScreenId.LOBBY);
    }

    public List<PlayerInfoItem> getPlayers() {
        return lobbyStateItem.getPlayerList();
    }
    public StringProperty getGameNameProperty() {
        return lobbyStateItem.getServerName();
    }
    public StringProperty getMaxPlayersProperty() {
        return lobbyStateItem.getMaxPlayers();
    }
    public StringProperty getPlayerCountProperty(){
        return lobbyStateItem.getPlayerCount();
    }


    public boolean isClientAdmin() {
        return clientPlayer.getIsAdmin();
    }

    @Override
    public void onLobbyStateUpdated(LobbyState lobbyState) {
        if (lobbyStateItem == null || !navState.getCurrentScreen().get().equals(NavigationStateViewModel.ScreenId.LOBBY)) {
            LobbyStateItem item = new LobbyStateItem(lobbyState);
            checkAndSetIfClientIsAdmin(lobbyState);
            setUpLobby(item);
            setUpGameInfo();
            return;
        }
        lobbyStateItem.updateFrom(lobbyState);
    }

    @EventListener
    public void saveGameInfoData(GameInfoRecord gameInfo) {
        this.gameInfo = gameInfo;
    }

    @EventListener
    public void handleLobbyNavigationEvent(ApplicationEvents event) {
        if (event == BACK_TO_GAME_SERVER_DASHBOARD) {
            this.clientPlayer = null;
            lobbyStateItem.dispose();
            this.lobbyStateItem = null;
            navState.navigateTo(NavigationStateViewModel.ScreenId.MAIN);
        }
    }



    @Override
    public void onPlayerConnected(ClientPlayer clientPlayer) {
        this.clientPlayer = clientPlayer;
    }

    public void startGame(StartGameRecord startGameRecord) {
        commandService.sendStartGameCommand(startGameRecord);
    }
    public void leaveLobby() {commandService.sendDisconnectCommand();}

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

    private void setUpGameInfo() {
        lobbyStateItem.updateFromGameInfo(gameInfo);
    }




    private void checkAndSetIfClientIsAdmin(LobbyState lobbyState) {
        String clientId = clientPlayer.getId();
        lobbyState.getPlayers().forEach(player -> {
            if (player.getId().equals(clientId)) {
                clientPlayer.setIsAdmin(player.getIsAdmin());
            }
        });
    }



}
