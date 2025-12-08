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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ClientCommandService {



    private final GameServerSocketService gameServerSocketService;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public ClientCommandService(GameServerSocketService gameServerSocketService, ApplicationEventPublisher eventPublisher) {
        this.gameServerSocketService = gameServerSocketService;
        this.eventPublisher = eventPublisher;
    }

    public void sendConnectCommand(GameInfoRecord gameInfo) {
        ConnectCommandPayload command  = new ConnectCommandPayload();
        command.setType(CommandType.CONNECT);
        //TODO impl reconnect
      //  connectCommandPayload.setPlayerId();
        command.setUsername(gameInfo.playerName());
        System.out.println("gameInfo " + gameInfo.toString());

        try {
            String json = mapCommandToMessage(CommandType.CONNECT, command);
            gameServerSocketService.connectToServer(gameInfo.uri().toString(), json);

        } catch (JsonProcessingException e) {
            log.error("Failed to map connect command to json", e);
            return;
        }
        eventPublisher.publishEvent(gameInfo);
    }

    public void sendStartGameCommand() {
        ConnectCommandPayload command  = new ConnectCommandPayload();
        command.setType(CommandType.START_GAME);
        try {
            String json = mapCommandToMessage(CommandType.START_GAME, command);
            gameServerSocketService.sendCommand(json);
        } catch (JsonProcessingException e) {
            log.error("Failed to map start game command to json", e);
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
