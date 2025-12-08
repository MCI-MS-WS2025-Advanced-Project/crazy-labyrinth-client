package mci.dibse.crazylabyrinth.client.view.viewmodel;

import jakarta.annotation.PostConstruct;
import javafx.beans.Observable;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mci.dibse.crazylabyrinth.client.NavigationController;
import mci.dibse.crazylabyrinth.client.domain.model.ClientPlayer;
import mci.dibse.crazylabyrinth.client.domain.model.LobbyState;
import mci.dibse.crazylabyrinth.client.domain.model.record.GameInfoRecord;
import mci.dibse.crazylabyrinth.client.service.*;
import mci.dibse.crazylabyrinth.client.view.viewmodel.item.GameServerItem;
import mci.dibse.crazylabyrinth.client.view.viewmodel.item.LobbyStateItem;
import mci.dibse.crazylabyrinth.client.view.viewmodel.item.PlayerItem;
import mci.dibse.crazylabyrinth.client.view.viewmodel.navigation.NavigationStateViewModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@Slf4j
@NoArgsConstructor
public class LobbyViewModel implements LobbyStateUpdateListener, PlayerConnectedAckListener {

    @Value("${game.default.bonus.enabled:false}")
    private boolean GAME_DEFAULT_BONUS_ENABLED;
    @Value("${game.default.board.size:7}")
    private int GAME_DEFAULT_BOARD_SIZE;
    @Value("${game.default.timer:60}")
    private int GAME_DEFAULT_TIMER;



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

    public void setUpLobby(LobbyStateItem lobbyStateItem){
        this.lobbyStateItem = lobbyStateItem;
        navState.navigateTo(NavigationStateViewModel.ScreenId.LOBBY);
    }

    public List<PlayerItem> getPlayers() {
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

    @Override
    public void onPlayerConnected(ClientPlayer clientPlayer) {
        this.clientPlayer = clientPlayer;
    }

    public void startGame() {
        commandService.sendStartGameCommand();
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
