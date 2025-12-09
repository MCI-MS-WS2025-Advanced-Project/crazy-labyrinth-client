package mci.dibse.crazylabyrinth.client.service;

import labyrinth.contracts.models.ConnectAckEventPayload;
import labyrinth.contracts.models.LobbyStateEventPayload;
import mci.dibse.crazylabyrinth.client.domain.model.ClientPlayer;
import mci.dibse.crazylabyrinth.client.domain.model.LobbyState;
import org.springframework.stereotype.Service;

import java.util.List;

import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class LobbyService {


    private final List<LobbyStateUpdateListener> lobbyStateListeners = new CopyOnWriteArrayList<>();
    private final List<PlayerConnectedAckListener> playerConnectedListener = new CopyOnWriteArrayList<>();


    public void addLobbyListener(LobbyStateUpdateListener l) {
        lobbyStateListeners.add(l);
    }
    public void addPlayerListener(PlayerConnectedAckListener l) {
        playerConnectedListener.add(l);
    }
    public void removeLobbyListener(LobbyStateUpdateListener l) {
        lobbyStateListeners.remove(l);
    }
    public void removePlayerListener(PlayerConnectedAckListener l) {
        playerConnectedListener.remove(l);
    }


/*
    private final GameServerManagementService managementService;
    @Autowired
    public LobbyService(GameServerManagementService managementService) {
        this.managementService = managementService;
    }
*/
// save identifier token for reconnects
//clientInfo.setIdentifierToken(connectAck.getIdentifierToken());

    //TODO wie spieler eindeutig identifizieren?
    // {"type":"CONNECT_ACK","identifierToken":"8a4a74a4-72d5-4742-9961-72abcd1d9b50"}
    // {"type":"LOBBY_STATE","players":[{"id":"7c561a10-1220-4bf1-a8c4-4fbdc77b55f9","name":"Julian","color":"RED","isAdmin":true,"isAiControlled":false}]}

    public void handleConnectAck(ConnectAckEventPayload connectAck) {
        ClientPlayer clientPlayer = new ClientPlayer();
        clientPlayer.setIdentifierToken(connectAck.getIdentifierToken());
        clientPlayer.setId(connectAck.getPlayerId());
        playerConnectedListener.forEach(l -> l.onPlayerConnected(clientPlayer));
    }


    public void handleLobbyState(LobbyStateEventPayload lobbyStatePayload) {
        LobbyState lobbyState = new LobbyState(lobbyStatePayload);
        lobbyStateListeners.forEach(l -> l.onLobbyStateUpdated(lobbyState));
    }


}
