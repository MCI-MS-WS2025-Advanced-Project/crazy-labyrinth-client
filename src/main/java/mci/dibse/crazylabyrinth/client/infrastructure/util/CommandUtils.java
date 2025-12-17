package mci.dibse.crazylabyrinth.client.infrastructure.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import labyrinth.contracts.models.*;

public class CommandUtils {

    public static String mapCommandToMessage(CommandType command, Object payload)
            throws JsonProcessingException {
        ObjectMapper mapper = ObjectMapperConfig.createCustomObjectMapper();
        return switch (command) {
            case CommandType.CONNECT -> mapper.writeValueAsString((ConnectCommandPayload) payload);
            case CommandType.DISCONNECT -> mapper.writeValueAsString((DisconnectCommandPayload) payload);
            default -> throw new IllegalStateException("Unexpected value: " + command);
        };
    }
}
