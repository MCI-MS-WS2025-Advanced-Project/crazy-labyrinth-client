package mci.dibse.crazylabyrinth.client.infrastructure.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import labyrinth.contracts.models.*;

public class EventDeserializer extends JsonDeserializer<Object> {

    /**
     * Deserialize an event payload based on its "type" field.
     *
     * @param parser JSON parser
     * @param context deserialization context
     * @return typed event payload
     * @throws IOException when parsing fails
     */
    @Override
    public Object deserialize(JsonParser parser, DeserializationContext context)
            throws IOException {

        ObjectMapper mapper = (ObjectMapper) parser.getCodec();
        JsonNode node = parser.readValueAsTree();

        String type = node.get("type").asText();
        EventType eventType = EventType.valueOf(type);


        return switch (eventType) {
            case EventType.SERVER_INFO -> mapper.treeToValue(node, ServerInfoPayload.class);
            case EventType.CONNECT_ACK -> mapper.treeToValue(node, ConnectAckEventPayload.class);
            case EventType.LOBBY_STATE -> mapper.treeToValue(node, LobbyStateEventPayload.class);
            case EventType.ACTION_ERROR -> mapper.treeToValue(node, ActionErrorEventPayload.class);
            case EventType.GAME_STARTED, EventType.GAME_STATE_UPDATE ->
                    mapper.treeToValue(node, GameStateEventPayload.class);
            case EventType.NEXT_TREASURE ->
                    mapper.treeToValue(node, NextTreasureCardEventPayload.class);
            case EventType.ACHIEVEMENT_UNLOCKED ->
                    mapper.treeToValue(node, AchievementUnlockedEventPayload.class);
            case EventType.GAME_OVER -> mapper.treeToValue(node, GameOverEventPayload.class);
            case EventType.PLAYER_UPDATED ->
                    mapper.treeToValue(node, PlayerUpdatedEventPayload.class);
            default -> throw new IllegalArgumentException("Unknown event type: " + type);
        };
    }
}
// PLAYER_UPDATED
