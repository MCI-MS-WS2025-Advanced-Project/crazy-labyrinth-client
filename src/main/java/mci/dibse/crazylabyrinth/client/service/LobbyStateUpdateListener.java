package mci.dibse.crazylabyrinth.client.service;

import mci.dibse.crazylabyrinth.client.domain.model.LobbyState;

public interface LobbyStateUpdateListener {
    void onLobbyStateUpdated(LobbyState lobbyState);
}
