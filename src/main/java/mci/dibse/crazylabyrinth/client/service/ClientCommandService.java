package mci.dibse.crazylabyrinth.client.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import labyrinth.contracts.models.CommandType;
import labyrinth.contracts.models.ConnectCommandPayload;
import lombok.extern.slf4j.Slf4j;
import mci.dibse.crazylabyrinth.client.domain.model.enums.CommandTypes;
import mci.dibse.crazylabyrinth.client.infrastructure.util.ObjectMapperConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class ClientCommandService {




    private final GameServerSocketService gameServerSocketService;

    @Autowired
    public ClientCommandService(GameServerSocketService gameServerSocketService) {
        this.gameServerSocketService = gameServerSocketService;
    }

    public void sendConnectCommand(String serverUri, String playerName) {
        ConnectCommandPayload command  = new ConnectCommandPayload();
        command.setType(CommandType.CONNECT);
        //TODO impl reconnect
      //  connectCommandPayload.setPlayerId();
        command.setUsername(playerName);

        try {
            String json = mapCommandToMessage(CommandType.CONNECT, command);

            gameServerSocketService.connectToServer(serverUri, json);

        } catch (JsonProcessingException e) {
            log.error("Failed to map connect command to json", e);
        }

    }



    private String mapCommandToMessage(CommandType command, Object payload) throws JsonProcessingException {
        ObjectMapper mapper = ObjectMapperConfig.createCustomObjectMapper();
        return switch (command) {
            case CommandType.CONNECT -> mapper.writeValueAsString((ConnectCommandPayload) payload);
            default -> throw new IllegalStateException("Unexpected value: " + command);
        };

    }



}
