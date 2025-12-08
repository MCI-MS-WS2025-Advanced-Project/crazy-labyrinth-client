package mci.dibse.crazylabyrinth.client.service;

import mci.dibse.crazylabyrinth.client.domain.model.ClientPlayer;
import mci.dibse.crazylabyrinth.client.domain.model.LobbyState;

public interface PlayerConnectedAckListener {
    void onPlayerConnected(ClientPlayer clientPlayer);
}
