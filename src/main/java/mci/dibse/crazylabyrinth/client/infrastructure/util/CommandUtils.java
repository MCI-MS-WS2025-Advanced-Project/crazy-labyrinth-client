package mci.dibse.crazylabyrinth.client.infrastructure.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import labyrinth.contracts.models.*;

public class CommandUtils {

    /**
     * Serialize a command payload to JSON based on command type.
     *
     * @param command command type
     * @param payload command payload
     * @return JSON string for the payload
     * @throws JsonProcessingException when serialization fails
     */
    public static String mapCommandToMessage(CommandType command, Object payload)
            throws JsonProcessingException {
        ObjectMapper mapper = ObjectMapperConfig.createCustomObjectMapper();
        return switch (command) {
            case CommandType.CONNECT -> mapper.writeValueAsString((ConnectCommandPayload) payload);
            case CommandType.DISCONNECT ->
                    mapper.writeValueAsString((DisconnectCommandPayload) payload);
            case CommandType.START_GAME ->
                    mapper.writeValueAsString((StartGameCommandPayload) payload);
            case CommandType.ROTATE_TILE ->
                    mapper.writeValueAsString((RotateTileCommandPayload) payload);
            case CommandType.PUSH_TILE ->
                    mapper.writeValueAsString((PushTileCommandPayload) payload);
            case CommandType.MOVE_PAWN ->
                    mapper.writeValueAsString((MovePawnCommandPayload) payload);
            case CommandType.TOGGLE_AI ->
                    mapper.writeValueAsString((ToggleAiCommandPayload) payload);
            case CommandType.USE_BEAM -> mapper.writeValueAsString((UseBeamCommandPayload) payload);
            case CommandType.USE_SWAP -> mapper.writeValueAsString((UseSwapCommandPayload) payload);
            case CommandType.USE_PUSH_FIXED ->
                    mapper.writeValueAsString((UsePushFixedCommandPayload) payload);
            case CommandType.USE_PUSH_TWICE ->
                    mapper.writeValueAsString((UsePushTwiceCommandPayload) payload);
            default -> throw new IllegalStateException("Unexpected value: " + command);
        };
    }
}
