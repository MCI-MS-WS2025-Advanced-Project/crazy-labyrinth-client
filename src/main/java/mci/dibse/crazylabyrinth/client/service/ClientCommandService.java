package mci.dibse.crazylabyrinth.client.service;

import static mci.dibse.crazylabyrinth.client.domain.context.ApplicationEvents.BACK_TO_GAME_SERVER_DASHBOARD;
import static mci.dibse.crazylabyrinth.client.infrastructure.util.CommandUtils.mapCommandToMessage;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.HashMap;
import labyrinth.contracts.models.*;
import lombok.extern.slf4j.Slf4j;
import mci.dibse.crazylabyrinth.client.domain.model.record.GameInfoRecord;
import mci.dibse.crazylabyrinth.client.domain.model.record.StartGameRecord;
import mci.dibse.crazylabyrinth.client.infrastructure.exceptions.ConnectionException;
import mci.dibse.crazylabyrinth.client.infrastructure.exceptions.GeneralUiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ClientCommandService {

    private final GameServerSocketService gameServerSocketService;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public ClientCommandService(
            GameServerSocketService gameServerSocketService,
            ApplicationEventPublisher eventPublisher) {
        this.gameServerSocketService = gameServerSocketService;
        this.eventPublisher = eventPublisher;
    }

    public void sendConnectCommand(GameInfoRecord gameInfo) {
        ConnectCommandPayload command = new ConnectCommandPayload();
        command.setType(CommandType.CONNECT);
        command.setUsername(gameInfo.playerName());
        log.debug("Sending connect command to client {}", command);

        try {
            String json = mapCommandToMessage(CommandType.CONNECT, command);
            gameServerSocketService.connectToServerAndSendMessage(gameInfo.uri().toString(), json);

        } catch (JsonProcessingException e) {
            log.error("Failed to map connect command to json", e);
            throw new GeneralUiException("Something went wrong! Please contact support!");
        } catch (ConnectionException _) {
            log.error("Failed to connect to server");
            throw new GeneralUiException("Something went wrong while connecting to server");
        }

        eventPublisher.publishEvent(gameInfo);
    }

    public void sendDisconnectCommand() {
        DisconnectCommandPayload command = new DisconnectCommandPayload();
        command.setType(CommandType.DISCONNECT);
        try {
            String json = mapCommandToMessage(CommandType.DISCONNECT, command);
            gameServerSocketService.sendCommand(json);
        } catch (JsonProcessingException e) {
            log.error("Failed to map disconnect command to json", e);
            throw new GeneralUiException("Failed to send command");
        } catch (GeneralUiException _) {
            log.error("Failed to send command");
            throw new GeneralUiException("Failed to send command");
        }
        eventPublisher.publishEvent(CommandType.DISCONNECT);
        eventPublisher.publishEvent(BACK_TO_GAME_SERVER_DASHBOARD);
    }

    public void sendStartGameCommand(StartGameRecord startGameRecord) throws GeneralUiException {
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
            log.debug("startGameCommand {}", json);
            gameServerSocketService.sendCommand(json);
        } catch (JsonProcessingException e) {
            log.error("Failed to map start game command to json", e);
        }
    }

    public void sendPushTileCommand(
            int index, Direction pushDirection, Direction[] extraTileEntrances) {
        PushTileCommandPayload command = new PushTileCommandPayload();
        command.setType(CommandType.PUSH_TILE);
        command.setDirection(pushDirection);
        //  command.setTileEntrances(extraTileEntrances);
        command.setRowOrColIndex(index);
        try {
            String json = mapCommandToMessage(CommandType.PUSH_TILE, command);
            gameServerSocketService.sendCommand(json);
        } catch (JsonProcessingException e) {
            log.error("Failed to map disconnect command to json", e);
        }
    }

    public void sendMovePawnCommand(int finalRow, int finalCol) {
        MovePawnCommandPayload command = new MovePawnCommandPayload();
        command.setType(CommandType.MOVE_PAWN);
        Coordinates coordinates = new Coordinates();
        coordinates.setColumn(finalCol);
        coordinates.setRow(finalRow);
        coordinates.setAdditionalProperties(new HashMap<>());
        command.setTargetCoordinates(coordinates);
        command.setAdditionalProperties(new HashMap<>());
        try {
            String json = mapCommandToMessage(CommandType.MOVE_PAWN, command);
            gameServerSocketService.sendCommand(json);
        } catch (JsonProcessingException e) {
            log.error("Failed to map disconnect command to json", e);
        }
    }

    public void sendRotateTileCommand() {
        RotateTileCommandPayload rotateTileCommandPayload = new RotateTileCommandPayload();
        rotateTileCommandPayload.setType(CommandType.ROTATE_TILE);
        try {
            String json = mapCommandToMessage(CommandType.ROTATE_TILE, rotateTileCommandPayload);
            gameServerSocketService.sendCommand(json);
        } catch (JsonProcessingException e) {
            log.error("Failed to map rotate tile command to json", e);
        }
    }

    // todo
    /*
               case CommandType.ROTATE_TILE -> mapper.writeValueAsString((RotateTileCommandPayload) payload);
           case CommandType.TOGGLE_AI -> mapper.writeValueAsString((ToggleAiCommandPayload) payload);
           case CommandType.USE_BEAM -> mapper.writeValueAsString((UseBeamCommandPayload) payload);
           case CommandType.USE_SWAP -> mapper.writeValueAsString((UseSwapCommandPayload) payload);
           case CommandType.USE_PUSH_FIXED -> mapper.writeValueAsString((UsePushFixedCommandPayload) payload);
           case CommandType.USE_PUSH_TWICE -> mapper.writeValueAsString((UsePushTwiceCommandPayload) payload);

    */

}
