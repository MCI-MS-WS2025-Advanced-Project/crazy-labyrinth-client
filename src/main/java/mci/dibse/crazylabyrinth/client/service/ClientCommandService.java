package mci.dibse.crazylabyrinth.client.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import labyrinth.contracts.models.CommandType;
import labyrinth.contracts.models.ConnectCommandPayload;
import lombok.extern.slf4j.Slf4j;
import mci.dibse.crazylabyrinth.client.domain.context.ClientInfo;
import mci.dibse.crazylabyrinth.client.domain.model.record.GameInfoRecord;
import mci.dibse.crazylabyrinth.client.infrastructure.util.ObjectMapperConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ClientCommandService {


    private ClientInfo clientInfo;

    private final GameServerSocketService gameServerSocketService;

    @Autowired
    public ClientCommandService(GameServerSocketService gameServerSocketService, ClientInfo clientInfo) {
        this.gameServerSocketService = gameServerSocketService;
        this.clientInfo = clientInfo;
    }

    public void sendConnectCommand(GameInfoRecord gameInfo) {
        ConnectCommandPayload command  = new ConnectCommandPayload();
        command.setType(CommandType.CONNECT);
        //TODO impl reconnect
      //  connectCommandPayload.setPlayerId();
        command.setUsername(gameInfo.playerName());
        //TODO needed?
        clientInfo.setPlayerName(gameInfo.playerName());
        clientInfo.setGameServerName(gameInfo.serverName());

        try {
            String json = mapCommandToMessage(CommandType.CONNECT, command);
            gameServerSocketService.connectToServer(gameInfo.uri().toString(), json);

        } catch (JsonProcessingException e) {
            log.error("Failed to map connect command to json", e);
        }

    }


    //TODO
    private String mapCommandToMessage(CommandType command, Object payload) throws JsonProcessingException {
        ObjectMapper mapper = ObjectMapperConfig.createCustomObjectMapper();
        return switch (command) {
            case CommandType.CONNECT -> mapper.writeValueAsString((ConnectCommandPayload) payload);
            default -> throw new IllegalStateException("Unexpected value: " + command);
        };

    }



}
