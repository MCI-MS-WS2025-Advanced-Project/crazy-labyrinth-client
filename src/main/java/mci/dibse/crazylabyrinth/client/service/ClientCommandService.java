package mci.dibse.crazylabyrinth.client.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import labyrinth.contracts.models.*;
import lombok.extern.slf4j.Slf4j;
import mci.dibse.crazylabyrinth.client.domain.model.record.GameInfoRecord;
import mci.dibse.crazylabyrinth.client.domain.model.record.StartGameRecord;
import mci.dibse.crazylabyrinth.client.infrastructure.util.ObjectMapperConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import static mci.dibse.crazylabyrinth.client.domain.context.ApplicationEvents.BACK_TO_GAME_SERVER_DASHBOARD;


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

    public void sendStartGameCommand(StartGameRecord startGameRecord) {
        StartGameCommandPayload command = new StartGameCommandPayload();
        command.setType(CommandType.START_GAME);

        BoardSize boardSize = new BoardSize();
        boardSize.setCols(startGameRecord.boardSize());
        boardSize.setRows(startGameRecord.boardSize());
        command.setBoardSize(boardSize);

        command.setGameDurationInSeconds(startGameRecord.gameDurationInSeconds());

        command.setTreasureCardCount(startGameRecord.treasureCardCount());

        command.setTotalBonusCount(startGameRecord.totalBonusCount());
        try {
            String json = mapCommandToMessage(CommandType.START_GAME, command);
            System.out.println("json " + json);
            gameServerSocketService.sendCommand(json);
        } catch (JsonProcessingException e) {
            log.error("Failed to map start game command to json", e);
        }
    }

    public void sendDisconnectCommand() {
        DisconnectCommandPayload command = new DisconnectCommandPayload();
        command.setType(CommandType.DISCONNECT);
        try {
            String json = mapCommandToMessage(CommandType.DISCONNECT, command);
            gameServerSocketService.sendCommand(json);
        } catch (JsonProcessingException e) {
            log.error("Failed to map disconnect command to json", e);
        }
        eventPublisher.publishEvent(BACK_TO_GAME_SERVER_DASHBOARD);

    }

    //TODO
    private String mapCommandToMessage(CommandType command, Object payload) throws JsonProcessingException {
        ObjectMapper mapper = ObjectMapperConfig.createCustomObjectMapper();
        return switch (command) {
            case CommandType.CONNECT -> mapper.writeValueAsString((ConnectCommandPayload) payload);
            case CommandType.START_GAME -> mapper.writeValueAsString((StartGameCommandPayload) payload);
            case CommandType.DISCONNECT -> mapper.writeValueAsString((DisconnectCommandPayload) payload);
            default -> throw new IllegalStateException("Unexpected value: " + command);
        };

    }



}
