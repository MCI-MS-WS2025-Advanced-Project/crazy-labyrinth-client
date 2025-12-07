package mci.dibse.crazylabyrinth.client.service;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import labyrinth.contracts.models.*;
import lombok.extern.slf4j.Slf4j;
import mci.dibse.crazylabyrinth.client.domain.context.ClientInfo;
import mci.dibse.crazylabyrinth.client.domain.model.LobbyState;
import mci.dibse.crazylabyrinth.client.infrastructure.util.ObjectMapperConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


@Slf4j
@Service
public class ServerEventService {

    private final List<LobbyStateUpdateListener> lobbyStateListeners = new CopyOnWriteArrayList<>();

    @Autowired
    private ClientInfo clientInfo;


    public void addListener(LobbyStateUpdateListener l) {
        lobbyStateListeners.add(l);
    }
    public void removeListener(LobbyStateUpdateListener l) {
        lobbyStateListeners.remove(l);
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
        log.info("Received server info: {}", serverInfo);
        //TODO save server info here
    }

    private void handleConnectAck(ConnectAckEventPayload connectAck) {
        log.info("Connection acknowledged: {}", connectAck.getIdentifierToken());
        // save identifier token for reconnects
        clientInfo.setIdentifierToken(connectAck.getIdentifierToken());
       //TODO wie spieler eindeutig identifizieren?
       // {"type":"CONNECT_ACK","identifierToken":"8a4a74a4-72d5-4742-9961-72abcd1d9b50"}
       // {"type":"LOBBY_STATE","players":[{"id":"7c561a10-1220-4bf1-a8c4-4fbdc77b55f9","name":"Julian","color":"RED","isAdmin":true,"isAiControlled":false}]}
    }

    private void handleGameState(GameStateUpdateEventPayload gameState) {
        log.info("Game state updated: {}", gameState);
    }

    private void handleGameStarted(GameStartedEventPayload gameStarted) {
        log.info("Game started: {}", gameStarted);
    }

    private void handleLobbyState(LobbyStateEventPayload lobbyState) {
        log.info("Lobby state updated: {}", lobbyState);
        // {"type":"LOBBY_STATE","players":[{"id":"7c561a10-1220-4bf1-a8c4-4fbdc77b55f9","name":"Julian","color":"RED","isAdmin":true,"isAiControlled":false}]}
        LobbyState lobby = new LobbyState(lobbyState);

        lobbyStateListeners.forEach(l -> l.onLobbyStateUpdated(lobby));
    }


}
