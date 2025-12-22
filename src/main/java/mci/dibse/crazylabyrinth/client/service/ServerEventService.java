package mci.dibse.crazylabyrinth.client.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import labyrinth.contracts.models.*;
import lombok.extern.slf4j.Slf4j;
import mci.dibse.crazylabyrinth.client.infrastructure.util.ObjectMapperConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ServerEventService {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final LobbyService lobbyService;
    private final GameService gameService;

    @Autowired
    public ServerEventService(
            LobbyService lobbyService,
            GameService gameService,
            ApplicationEventPublisher applicationEventPublisher) {
        this.lobbyService = lobbyService;
        this.gameService = gameService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public void handleEvent(String event) {
        ObjectMapper mapper = ObjectMapperConfig.createEventObjectMapper();
        try {
            Object eventObj = mapper.readValue(event, Object.class);
            handleTypedEvent(eventObj);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse event JSON: {}", e.getMessage());
        }
    }

    private void handleTypedEvent(Object event) {
        switch (event) {
            case ServerInfoPayload serverInfo -> handleServerInfo(serverInfo);
            case ConnectAckEventPayload connectAck -> handleConnectAck(connectAck);
            case LobbyStateEventPayload lobbyState -> handleLobbyState(lobbyState);
            case GameStateEventPayload gameState -> handleGameState(gameState);
            case NextTreasureCardEventPayload nextTreasure -> handleNextTreasure(nextTreasure);
            case ActionErrorEventPayload actionError -> handleError(actionError);
            case AchievementUnlockedEventPayload achievement -> handleAchievement(achievement);
            case GameOverEventPayload gameOver -> handleGameOver(gameOver);
            case PlayerUpdatedEventPayload playerUpdate -> handlePlayerUpdate(playerUpdate);
            default -> log.warn("Unknown event type: {}", event.getClass().getSimpleName());
        }
    }

    private void handlePlayerUpdate(PlayerUpdatedEventPayload playerUpdate) {
        log.debug("player update event received {}", playerUpdate);
        // todo??????
    }

    private void handleServerInfo(ServerInfoPayload serverInfo) {
        log.debug("Received server info: {}", serverInfo);
        // TODO what do with this?
        // snyc client with server time?
    }

    private void handleError(ActionErrorEventPayload actionError) {
        log.debug("Received action error: {}", actionError);
        // todo
        applicationEventPublisher.publishEvent(actionError);
    }

    private void handleConnectAck(ConnectAckEventPayload connectAck) {
        log.debug("Connection acknowledged: {}", connectAck.getIdentifierToken());
        lobbyService.handleConnectAck(connectAck);
    }

    private void handleLobbyState(LobbyStateEventPayload lobbyState) {
        log.debug("Lobby state updated: {}", lobbyState);
        lobbyService.handleLobbyState(lobbyState);
    }

    private void handleGameState(GameStateEventPayload gameState) {
        log.debug("game state updated: {}", gameState);
        gameService.handleGameEvent(gameState);
    }

    private void handleNextTreasure(NextTreasureCardEventPayload nextTreasure) {
        log.debug("next treasure event received {}", nextTreasure);
        gameService.handleGameEvent(nextTreasure);
    }

    private void handleAchievement(AchievementUnlockedEventPayload achievement) {
        log.debug("achievement unlocked event received{}", achievement);
    }

    private void handleGameOver(GameOverEventPayload gameOver) {
        log.debug("game over event received {}", gameOver);
    }
}
