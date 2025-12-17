package mci.dibse.crazylabyrinth.client.service;

import mci.dibse.crazylabyrinth.client.domain.model.ClientPlayer;

public interface PlayerConnectedAckListener {
    void onPlayerConnected(ClientPlayer clientPlayer);
}
