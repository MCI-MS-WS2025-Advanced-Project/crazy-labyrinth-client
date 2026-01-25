package mci.dibse.crazylabyrinth.client.infrastructure.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import labyrinth.contracts.models.*;

import java.io.IOException;

public class EventDeserializer extends JsonDeserializer<Object> {

    @Override
    public Object deserialize(JsonParser parser, DeserializationContext context)
            throws IOException {

        ObjectMapper mapper = (ObjectMapper) parser.getCodec();
        JsonNode node = parser.readValueAsTree();

        String type = node.get("type").asText();
        EventType eventType = EventType.valueOf(type);

        //TODO complete
        return switch (eventType) {
            case EventType.SERVER_INFO -> mapper.treeToValue(node, ServerInfoPayload.class);
            case EventType.CONNECT_ACK -> mapper.treeToValue(node, ConnectAckEventPayload.class);
            case EventType.GAME_STATE_UPDATE -> mapper.treeToValue(node, GameStateUpdateEventPayload.class);
            case EventType.GAME_STARTED -> mapper.treeToValue(node, GameStartedEventPayload.class);
            case EventType.LOBBY_STATE -> mapper.treeToValue(node, LobbyStateEventPayload.class);
            case EventType.PLAYER_TURN -> mapper.treeToValue(node, PlayerTurnEventPayload.class);
            case EventType.GAME_OVER -> mapper.treeToValue(node, GameOverEventPayload.class);
            case EventType.ACHIEVEMENT_UNLOCKED -> mapper.treeToValue(node, AchievementUnlockedEventPayload.class);
            case EventType.ACTION_ERROR -> mapper.treeToValue(node, ActionErrorEventPayload.class);
            case EventType.NEXT_TREASURE -> mapper.treeToValue(node, NextTreasureCardEventPayload.class);
            case EventType.PLAYER_DISCONNECTED -> mapper.treeToValue(node, PlayerDisconnectedEventPayload.class);
            default -> throw new IllegalArgumentException("Unknown event type: " + type);
        };
    }
}

