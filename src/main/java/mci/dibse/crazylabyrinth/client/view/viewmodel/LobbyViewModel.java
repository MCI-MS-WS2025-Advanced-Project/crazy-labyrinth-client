package mci.dibse.crazylabyrinth.client.view.viewmodel;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;
import labyrinth.contracts.models.LobbyStateEventPayload;
import labyrinth.contracts.models.PlayerInfo;
import lombok.extern.slf4j.Slf4j;
import mci.dibse.crazylabyrinth.client.domain.model.ClientPlayer;
import mci.dibse.crazylabyrinth.client.domain.model.record.GameInfoRecord;
import mci.dibse.crazylabyrinth.client.service.LobbyService;
import mci.dibse.crazylabyrinth.client.service.LobbyStateUpdateListener;
import mci.dibse.crazylabyrinth.client.service.PlayerConnectedAckListener;
import mci.dibse.crazylabyrinth.client.view.controller.impl.BaseViewCleanup;
import mci.dibse.crazylabyrinth.client.view.viewmodel.item.LobbyStateItem;
import mci.dibse.crazylabyrinth.client.view.viewmodel.navigation.NavigationStateViewModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LobbyViewModel extends BaseViewCleanup
        implements LobbyStateUpdateListener, PlayerConnectedAckListener {

    private final NavigationStateViewModel navState;
    private final LobbyService lobbyService;

    private GameInfoRecord gameInfo;
    private ClientPlayer clientPlayer;

    private LobbyStateItem lobbyStateItem = null;

    @Autowired
    public LobbyViewModel(NavigationStateViewModel navState, LobbyService lobbyService) {
        super(navState);
        this.navState = navState;
        this.lobbyService = lobbyService;
    }

    @PostConstruct
    public void initLobbyViewModel() {
        lobbyService.addPlayerListener(this);
        lobbyService.addLobbyListener(this);
    }

    @EventListener
    public void saveGameInfoData(GameInfoRecord gameInfo) {
        this.gameInfo = gameInfo;
    }

    @Override
    public void onLobbyStateUpdated(LobbyStateEventPayload lobbyState) {
        checkAndSetIfClientIsAdmin(lobbyState);
        if (lobbyStateItem == null
                || !navState.getCurrentScreen()
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
        navState.navigateTo(NavigationStateViewModel.ScreenId.LOBBY);
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
}
