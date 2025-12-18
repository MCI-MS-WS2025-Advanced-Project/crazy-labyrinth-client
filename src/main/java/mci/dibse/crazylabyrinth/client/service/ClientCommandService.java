package mci.dibse.crazylabyrinth.client.service;

import static mci.dibse.crazylabyrinth.client.domain.context.ApplicationEvents.BACK_TO_GAME_SERVER_DASHBOARD;

import com.fasterxml.jackson.core.JsonProcessingException;
import labyrinth.contracts.models.*;
import lombok.extern.slf4j.Slf4j;
import mci.dibse.crazylabyrinth.client.domain.model.record.GameInfoRecord;
import mci.dibse.crazylabyrinth.client.infrastructure.exceptions.ConnectionException;
import mci.dibse.crazylabyrinth.client.infrastructure.util.CommandUtils;
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
            String json = CommandUtils.mapCommandToMessage(CommandType.CONNECT, command);
            gameServerSocketService.connectToServerAndSendMessage(gameInfo.uri().toString(), json);

        } catch (JsonProcessingException e) {
            log.error("Failed to map connect command to json", e);
            throw new ConnectionException("Something went wrong! Please contact support!");
        } catch (ConnectionException e) {
            log.error("Failed to connect to server");
        }

        eventPublisher.publishEvent(gameInfo);
    }

    public void sendDisconnectCommand() {
        DisconnectCommandPayload command = new DisconnectCommandPayload();
        command.setType(CommandType.DISCONNECT);
        try {
            String json = CommandUtils.mapCommandToMessage(CommandType.DISCONNECT, command);
            gameServerSocketService.sendCommand(json);
        } catch (JsonProcessingException e) {
            log.error("Failed to map disconnect command to json", e);
        }
        eventPublisher.publishEvent(CommandType.DISCONNECT);
        eventPublisher.publishEvent(BACK_TO_GAME_SERVER_DASHBOARD);
    }
}
