package mci.dibse.crazylabyrinth.client.service;

import lombok.extern.slf4j.Slf4j;
import mci.dibse.crazylabyrinth.client.infrastructure.gameserver.GameServerSocketClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class GameServerSocketService {



    private final GameServerSocketClient client;

    @Autowired
    public GameServerSocketService(GameServerSocketClient client) {
        this.client = client;
    }

     public String sendCommand(String command) {
            client.sendMessage(command);
            return "Command sent";
     }


    // TODO remove = test method
    public String connectToServer(String serverUrl, String json) {

        serverUrl = "ws://127.0.0.1:9001/game";
      //  serverUrl = "ws://192.168.178.24:9001/";
        client.connect(serverUrl);
        client.sendMessage(json);
        return "Connected";
    }

}
