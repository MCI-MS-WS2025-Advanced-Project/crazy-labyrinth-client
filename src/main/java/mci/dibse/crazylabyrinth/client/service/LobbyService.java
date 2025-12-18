package mci.dibse.crazylabyrinth.client.service;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import labyrinth.contracts.models.ConnectAckEventPayload;
import labyrinth.contracts.models.LobbyStateEventPayload;
import mci.dibse.crazylabyrinth.client.domain.model.ClientPlayer;
import org.springframework.stereotype.Service;

@Service
public class LobbyService {

    private final List<LobbyStateUpdateListener> lobbyStateListeners = new CopyOnWriteArrayList<>();
    private final List<PlayerConnectedAckListener> playerConnectedListener =
            new CopyOnWriteArrayList<>();

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

    public void handleConnectAck(ConnectAckEventPayload connectAck) {
        ClientPlayer clientPlayer = new ClientPlayer();
        clientPlayer.setIdentifierToken(connectAck.getIdentifierToken());
        clientPlayer.setId(connectAck.getPlayerId());
        playerConnectedListener.forEach(l -> l.onPlayerConnected(clientPlayer));
    }

    public void handleLobbyState(LobbyStateEventPayload lobbyStatePayload) {
        lobbyStateListeners.forEach(l -> l.onLobbyStateUpdated(lobbyStatePayload));
    }
}
