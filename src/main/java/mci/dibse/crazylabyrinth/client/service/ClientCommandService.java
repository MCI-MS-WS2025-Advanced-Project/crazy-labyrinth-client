package mci.dibse.crazylabyrinth.client.service;

import static mci.dibse.crazylabyrinth.client.domain.context.ApplicationEvents.BACK_TO_GAME_SERVER_DASHBOARD;
import static mci.dibse.crazylabyrinth.client.infrastructure.util.CommandUtils.mapCommandToMessage;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.HashMap;
import labyrinth.contracts.models.*;
import lombok.extern.slf4j.Slf4j;
import mci.dibse.crazylabyrinth.client.domain.context.PushTileCommandRecord;
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
            gameServerSocketService.sendCommand(json, null);
        } catch (JsonProcessingException e) {
            log.error("Failed to map disconnect command to json", e);
            throw new GeneralUiException("Failed to send command");
        } catch (GeneralUiException _) {
            log.error("Failed to send command");
            throw new GeneralUiException("Failed to send command");
        } finally {
            eventPublisher.publishEvent(BACK_TO_GAME_SERVER_DASHBOARD);
            eventPublisher.publishEvent(CommandType.DISCONNECT);
        }

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
            gameServerSocketService.sendCommand(json, CommandType.START_GAME);
        } catch (JsonProcessingException e) {
            log.error("Failed to map start game command to json", e);
        }
    }

    public void sendPushTileCommand(PushTileCommandRecord data) {
        Object command = null;
        if (CommandType.PUSH_TILE.equals(data.commandType())) {
            PushTileCommandPayload payload = new PushTileCommandPayload();
            payload.setType(data.commandType());
            payload.setDirection(data.pushDirection());
            payload.setRowOrColIndex(data.index());
            command = payload;
        } else if (CommandType.USE_PUSH_FIXED.equals(data.commandType())) {
            UsePushFixedCommandPayload payload = new UsePushFixedCommandPayload();
            payload.setType(CommandType.USE_PUSH_FIXED);
            payload.setDirection(data.pushDirection());
            payload.setRowOrColIndex(data.index());
            command = payload;
        }
        if (command == null) return;
        try {
            String json = mapCommandToMessage(data.commandType(), command);
            gameServerSocketService.sendCommand(json, data.commandType());
        } catch (JsonProcessingException e) {
            log.error("Failed to map push tile command to json", e);
            throw new GeneralUiException("Failed to process push tile command");
        } catch (GeneralUiException _) {
            log.error("Failed to send push tile command");
            throw new GeneralUiException("Failed to send push tile command");
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
            gameServerSocketService.sendCommand(json, null);
        } catch (JsonProcessingException e) {
            log.error("Failed to map disconnect command to json", e);
            throw new GeneralUiException("Failed to send move command");
        } catch (GeneralUiException _) {
            log.error("Failed to send move command");
            throw new GeneralUiException("Failed to send  move command");
        }
    }

    public void sendRotateTileCommand() {
        RotateTileCommandPayload rotateTileCommandPayload = new RotateTileCommandPayload();
        rotateTileCommandPayload.setType(CommandType.ROTATE_TILE);
        try {
            String json = mapCommandToMessage(CommandType.ROTATE_TILE, rotateTileCommandPayload);
            gameServerSocketService.sendCommand(json, null);
        } catch (JsonProcessingException e) {
            log.error("Failed to map rotate tile command to json", e);
            throw new GeneralUiException("Failed to send rotate command");
        } catch (GeneralUiException _) {
            log.error("Failed to send rotate command");
            throw new GeneralUiException("Failed to send rotate command");
        }
    }

    public void sendUsePushTwiceComnmand() {
        UsePushTwiceCommandPayload command = new UsePushTwiceCommandPayload();
        command.setType(CommandType.USE_PUSH_TWICE);
        try {
            String json = mapCommandToMessage(CommandType.USE_PUSH_TWICE, command);
            gameServerSocketService.sendCommand(json, CommandType.USE_PUSH_TWICE);
        } catch (JsonProcessingException e) {
            log.error("Failed to map use push twice command to json", e);
            throw new GeneralUiException("Failed to send use push twice command");
        } catch (GeneralUiException _) {
            log.error("Failed to send use push twice command");
            throw new GeneralUiException("Failed to send use push twice command");
        }
    }

    public void sendUseBeamCommand(int finalRow, int finalCol) {
        UseBeamCommandPayload command = new UseBeamCommandPayload();
        command.setType(CommandType.USE_BEAM);
        Coordinates coordinates = new Coordinates();
        coordinates.setColumn(finalCol);
        coordinates.setRow(finalRow);
        coordinates.setAdditionalProperties(new HashMap<>());
        command.setTargetCoordinates(coordinates);
        try {
            String json = mapCommandToMessage(CommandType.USE_BEAM, command);
            gameServerSocketService.sendCommand(json, CommandType.USE_BEAM);
        } catch (JsonProcessingException e) {
            log.error("Failed to map use beam command to json", e);
            throw new GeneralUiException("Failed to send  use beam command");
        } catch (GeneralUiException _) {
            log.error("Failed to send  use beam command");
            throw new GeneralUiException("Failed to send  use beam command");
        }
    }

    public void sendUseSwapCommand(String playerId) {
        UseSwapCommandPayload command = new UseSwapCommandPayload();
        command.setType(CommandType.USE_SWAP);
        command.setTargetPlayerId(playerId);
        try {
            String json = mapCommandToMessage(CommandType.USE_SWAP, command);
            gameServerSocketService.sendCommand(json, CommandType.USE_SWAP);
        } catch (JsonProcessingException e) {
            log.error("Failed to map use swap command to json", e);
            throw new GeneralUiException("Failed to send use swap command");
        } catch (GeneralUiException _) {
            log.error("Failed to send use swap command");
            throw new GeneralUiException("Failed to send  use swap command");
        }
    }

    // todo
    /*
        case CommandType.TOGGLE_AI -> mapper.writeValueAsString((ToggleAiCommandPayload) payload);
           case CommandType.USE_BEAM -> mapper.writeValueAsString((UseBeamCommandPayload) payload);
           case CommandType.USE_SWAP -> mapper.writeValueAsString((UseSwapCommandPayload) payload);
           case CommandType.USE_PUSH_TWICE -> mapper.writeValueAsString((UsePushTwiceCommandPayload) payload);

    */

}
