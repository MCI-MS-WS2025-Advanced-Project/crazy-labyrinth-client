package mci.dibse.crazylabyrinth.client.infrastructure.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import labyrinth.contracts.models.*;

public class EventDeserializer extends JsonDeserializer<Object> {

    @Override
    public Object deserialize(JsonParser parser, DeserializationContext context)
            throws IOException {

        ObjectMapper mapper = (ObjectMapper) parser.getCodec();
        JsonNode node = parser.readValueAsTree();

        String type = node.get("type").asText();
        EventType eventType = EventType.valueOf(type);

        // TODO complete
        return switch (eventType) {
            case EventType.SERVER_INFO -> mapper.treeToValue(node, ServerInfoPayload.class);
            case EventType.CONNECT_ACK -> mapper.treeToValue(node, ConnectAckEventPayload.class);
            case EventType.LOBBY_STATE -> mapper.treeToValue(node, LobbyStateEventPayload.class);
            default -> throw new IllegalArgumentException("Unknown event type: " + type);
        };
    }
}
