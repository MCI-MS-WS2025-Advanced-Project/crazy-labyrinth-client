package mci.dibse.crazylabyrinth.client.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LobbyService {


    private final GameServerManagementService managementService;

    @Autowired
    public LobbyService(GameServerManagementService managementService) {
        this.managementService = managementService;
    }



    public void joinLobby(String serverId, String playerName) {

    }

    public void leaveLobby(String lobbyId) {

    }

}
