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

    /**
     * Register a lobby state listener.
     *
     * @param l listener to add
     */
    public void addLobbyListener(LobbyStateUpdateListener l) {
        lobbyStateListeners.add(l);
    }

    /**
     * Register a player connection listener.
     *
     * @param l listener to add
     */
    public void addPlayerListener(PlayerConnectedAckListener l) {
        playerConnectedListener.add(l);
    }

    /**
     * Unregister a lobby state listener.
     *
     * @param l listener to remove
     */
    public void removeLobbyListener(LobbyStateUpdateListener l) {
        lobbyStateListeners.remove(l);
    }

    /**
     * Unregister a player connection listener.
     *
     * @param l listener to remove
     */
    public void removePlayerListener(PlayerConnectedAckListener l) {
        playerConnectedListener.remove(l);
    }

    /**
     * Notify listeners of a successful connection acknowledgement.
     *
     * @param connectAck connect acknowledgement payload
     */
    public void handleConnectAck(ConnectAckEventPayload connectAck) {
        ClientPlayer clientPlayer = new ClientPlayer();
        clientPlayer.setIdentifierToken(connectAck.getIdentifierToken());
        clientPlayer.setId(connectAck.getPlayerId());
        playerConnectedListener.forEach(l -> l.onPlayerConnected(clientPlayer));
    }

    /**
     * Notify listeners of a lobby state update.
     *
     * @param lobbyStatePayload lobby state payload
     */
    public void handleLobbyState(LobbyStateEventPayload lobbyStatePayload) {
        lobbyStateListeners.forEach(l -> l.onLobbyStateUpdated(lobbyStatePayload));
    }
}
