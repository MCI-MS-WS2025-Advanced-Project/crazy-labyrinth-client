package mci.dibse.crazylabyrinth.client.service;

import labyrinth.contracts.models.LobbyStateEventPayload;

public interface LobbyStateUpdateListener {

    void onLobbyStateUpdated(LobbyStateEventPayload lobbyState);
}
