package mci.dibse.crazylabyrinth.client.service;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import labyrinth.contracts.models.*;
import lombok.extern.slf4j.Slf4j;
import mci.dibse.crazylabyrinth.client.infrastructure.util.ObjectMapperConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;


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

    //TODO complete
    private void handleTypedEvent(Object event) {
        switch (event) {
            case ServerInfoPayload serverInfo -> handleServerInfo(serverInfo);
            case ConnectAckEventPayload connectAck -> handleConnectAck(connectAck);
            case GameStateUpdateEventPayload gameState -> handleGameState(gameState);
            case GameStartedEventPayload gameStarted -> handleGameStarted(gameStarted);
            case LobbyStateEventPayload lobbyState -> handleLobbyState(lobbyState);
            case ActionErrorEventPayload errorState -> handleErrorState(errorState);
            default -> log.warn("Unknown event type: {}", event.getClass().getSimpleName());
        }
    }


    private String extractTypeFromStream(JsonParser parser) throws IOException {
        while (parser.nextToken() != null) {
            if (parser.getCurrentToken() == JsonToken.FIELD_NAME &&
                    "type".equals(parser.currentName())) {
                parser.nextToken(); // Move to field value
                return parser.getValueAsString();
            }
        }
        throw new IllegalArgumentException("No 'type' field found in JSON");
    }





    private void handleServerInfo(ServerInfoPayload serverInfo) {
        log.debug("Received server info: {}", serverInfo);
        //TODO save server info here
    }

    private void handleConnectAck(ConnectAckEventPayload connectAck) {
        log.debug("Connection acknowledged: {}", connectAck.getIdentifierToken());
        lobbyService.handleConnectAck(connectAck);
    }

    private void handleGameState(GameStateUpdateEventPayload gameState) {
        log.debug("Game state updated: {}", gameState);
    }

    private void handleGameStarted(GameStartedEventPayload gameStarted) {
        log.debug("Game started: {}", gameStarted);
    }

    private void handleLobbyState(LobbyStateEventPayload lobbyState) {
        log.debug("Lobby state updated: {}", lobbyState);
        // {"type":"LOBBY_STATE","players":[{"id":"7c561a10-1220-4bf1-a8c4-4fbdc77b55f9","name":"Julian","color":"RED","isAdmin":true,"isAiControlled":false}]}
        lobbyService.handleLobbyState( lobbyState);
    }

    private void handleErrorState(ActionErrorEventPayload errorState) {
        log.debug("Action error: {}", errorState);
        //TODO impl
       // errorHandler.handleErrorState(errorState);
    }


}
