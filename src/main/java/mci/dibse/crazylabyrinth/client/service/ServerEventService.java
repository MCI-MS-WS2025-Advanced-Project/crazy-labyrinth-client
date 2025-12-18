package mci.dibse.crazylabyrinth.client.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import labyrinth.contracts.models.*;
import lombok.extern.slf4j.Slf4j;
import mci.dibse.crazylabyrinth.client.infrastructure.util.ObjectMapperConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ServerEventService {

    private final LobbyService lobbyService;

    @Autowired
    public ServerEventService(LobbyService lobbyService) {
        this.lobbyService = lobbyService;
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
            default -> log.warn("Unknown event type: {}", event.getClass().getSimpleName());
        }
    }

    private void handleServerInfo(ServerInfoPayload serverInfo) {
        log.debug("Received server info: {}", serverInfo);
        // TODO what do with this?
        // snyc client with server time?
    }

    private void handleConnectAck(ConnectAckEventPayload connectAck) {
        log.debug("Connection acknowledged: {}", connectAck.getIdentifierToken());
        lobbyService.handleConnectAck(connectAck);
    }

    private void handleLobbyState(LobbyStateEventPayload lobbyState) {
        log.debug("Lobby state updated: {}", lobbyState);
        lobbyService.handleLobbyState(lobbyState);
    }
}
