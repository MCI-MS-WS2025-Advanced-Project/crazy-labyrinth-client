package mci.dibse.crazylabyrinth.client.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import mci.dibse.crazylabyrinth.client.domain.model.management.ErrorResponse;
import mci.dibse.crazylabyrinth.client.domain.model.management.GameServer;
import mci.dibse.crazylabyrinth.client.infrastructure.api.dto.GameServerDTO;
import mci.dibse.crazylabyrinth.client.infrastructure.exceptions.GameManagementServerException;
import mci.dibse.crazylabyrinth.client.infrastructure.exceptions.GeneralUiException;
import mci.dibse.crazylabyrinth.client.infrastructure.management.ManagementServerClient;
import mci.dibse.crazylabyrinth.client.infrastructure.util.ObjectMapperConfig;
import mci.dibse.crazylabyrinth.client.infrastructure.util.ValidatorConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *  Single Source of truth for GameServer management
 *  holds all GameServers model objects
 *  @author Julian Janetschek
 */
@Slf4j
@Service
public class GameServerManagementService {

    private final String CUSTOMER_SERVICE_MESSAGE = " Please try again later ";
    private final String GAME_MANAGEMENT_SERVER_EXCEPTION =
            "Failed to retrieve game server list from management server.";
    private final String GAME_MANAGEMENT_SERVER_PARSING_EXCEPTION =
            "Failed to parse game server list from management server.";
    private final String GAME_MANAGEMENT_SERVER_INVALID_EXCEPTION =
            "Invalid game server list received from management server";

    private final ManagementServerClient client;

    private final Map<UUID, GameServer> servers = new ConcurrentHashMap<>();
    private final List<GameServerUpdateListener> listeners = new CopyOnWriteArrayList<>();

    @Autowired
    public GameServerManagementService(ManagementServerClient client) {
        this.client = client;
    }

    /**
     * Register a listener for game server list updates.
     *
     * @param l listener to add
     */
    public void addListener(GameServerUpdateListener l) {
        listeners.add(l);
    }

    /**
     * Unregister a listener for game server list updates.
     *
     * @param l listener to remove
     */
    public void removeListener(GameServerUpdateListener l) {
        listeners.remove(l);
    }

    /**
     * Return an unmodifiable view of the currently known game servers.
     *
     * @return immutable collection of game servers
     */
    public Collection<GameServer> getServers() {
        return Collections.unmodifiableCollection(servers.values());
    }

    /**
     * Fetch the latest game server list and refresh local state.
     *
     * @throws GeneralUiException when the management server call fails or returns invalid data
     */
    public void refreshGameServerList() throws GeneralUiException {

        try {
            String jsonString = client.getGameServers();

            processGameServerResponseBody(jsonString.getBytes(StandardCharsets.UTF_8));

        } catch (GameManagementServerException e) {
            log.error(GAME_MANAGEMENT_SERVER_EXCEPTION, e);
            ErrorResponse errorResponse = null;
            try {
                errorResponse = processGameServerErrorResponseBody(e.getErrorResponseBody());
            } catch (Exception errorException) {
                log.error(GAME_MANAGEMENT_SERVER_EXCEPTION, errorException);
                throwUiException(
                        GAME_MANAGEMENT_SERVER_EXCEPTION + CUSTOMER_SERVICE_MESSAGE,
                        errorException,
                        null);
            }
            throwUiException(
                    GAME_MANAGEMENT_SERVER_EXCEPTION + CUSTOMER_SERVICE_MESSAGE, e, errorResponse);
        } catch (IOException | IllegalArgumentException e) {
            String logMessage =
                    (e instanceof IOException)
                            ? GAME_MANAGEMENT_SERVER_PARSING_EXCEPTION
                            : GAME_MANAGEMENT_SERVER_INVALID_EXCEPTION;
            log.error(logMessage, e);
            throwUiException(GAME_MANAGEMENT_SERVER_EXCEPTION + CUSTOMER_SERVICE_MESSAGE, e, null);
        }
    }

    /**
     * Throw a UI exception with an optional error response payload.
     *
     * @param message user-facing message
     * @param cause root cause
     * @param errorResponse optional error response from server
     */
    private void throwUiException(String message, Throwable cause, ErrorResponse errorResponse) {
        throw new GeneralUiException(message, cause, errorResponse);
    }

    /**
     * Parse and apply a raw game server response body.
     *
     * @param raw raw response bytes
     * @throws IOException when parsing fails
     * @throws IllegalArgumentException when validation fails
     */
    private void processGameServerResponseBody(byte[] raw)
            throws IOException, IllegalArgumentException {
        List<GameServerDTO> gameServersDTO = parseGameServerDTOsFromBytes(raw);
        validateGameServerDTOs(gameServersDTO);
        validateGameServerUrl(gameServersDTO);
        List<GameServer> gameServers = mapToGameServers(gameServersDTO);
        refresh(gameServers);
    }

    /**
     * Ensure all game server URLs have the expected "/game" path.
     *
     * @param gameServersDTO server DTOs to validate
     */
    private void validateGameServerUrl(List<GameServerDTO> gameServersDTO) {
        gameServersDTO.forEach(
                dto -> {
                    URI uri = dto.getUri();
                    String path = uri.getPath();
                    if (path == null || !uri.getPath().startsWith("/game")) {
                        URI baseWithPath = null;
                        try {
                            baseWithPath =
                                    new URI(
                                            uri.getScheme(),
                                            uri.getUserInfo(),
                                            uri.getHost(),
                                            uri.getPort(),
                                            "/game",
                                            uri.getQuery(),
                                            uri.getFragment());
                        } catch (URISyntaxException e) {
                            throw new RuntimeException(e);
                        }
                        dto.setUri(baseWithPath);
                    }
                });
    }

    /**
     * Parse a list of game server DTOs from raw response bytes.
     *
     * @param raw raw response bytes
     * @return list of game server DTOs
     * @throws IOException when JSON parsing fails
     */
    private List<GameServerDTO> parseGameServerDTOsFromBytes(byte[] raw) throws IOException {
        ObjectMapper mapper = ObjectMapperConfig.createCustomObjectMapper();
        return mapper.readValue(raw, new TypeReference<List<GameServerDTO>>() {});
    }

    /**
     * Validate a list of game server DTOs.
     *
     * @param gameServers DTOs to validate
     * @throws IllegalArgumentException when validation fails
     */
    private void validateGameServerDTOs(List<GameServerDTO> gameServers)
            throws IllegalArgumentException {
        Validator validator = ValidatorConfig.createBeanValidator();
        Set<ConstraintViolation<GameServerDTO>> violations =
                gameServers.stream()
                        .map((GameServerDTO dto) -> validator.validate(dto))
                        .flatMap(Set::stream)
                        .collect(Collectors.toSet());
        if (!violations.isEmpty()) throw new IllegalArgumentException(violations.toString());
    }

    /**
     * Map DTOs to domain game server objects.
     *
     * @param gameServersDTO DTOs to map
     * @return list of mapped domain objects
     */
    private List<GameServer> mapToGameServers(List<GameServerDTO> gameServersDTO) {
        List<GameServer> gameServers = new ArrayList<>();
        gameServersDTO.forEach(dto -> gameServers.add(new GameServer(dto)));
        return gameServers;
    }

    /**
     * Refresh local game server state and notify listeners.
     *
     * @param updatedServers latest list of servers
     */
    private void refresh(Collection<GameServer> updatedServers) {
        log.info("Refreshing game server list");
        Set<UUID> toRemove = new HashSet<>(servers.keySet());
        if (updatedServers != null) {
            for (GameServer incoming : updatedServers) {
                UUID id = incoming.getId();
                toRemove.remove(incoming.getId());
                GameServer existing = servers.get(id);
                // TODO what can server change?
                if (existing != null) {
                    existing.setCurrentPlayerCount(incoming.getCurrentPlayerCount());
                    existing.setStatus(incoming.getStatus());
                    existing.setLastSeen(incoming.getLastSeen());
                } else {
                    servers.put(id, incoming);
                }
            }
        }
        for (UUID id : toRemove) {
            servers.remove(id);
        }
        for (GameServerUpdateListener l : listeners) {
            l.onServersUpdated(getServers());
        }
    }

    /**
     * Parse and validate an error response payload.
     *
     * @param errorResponseBody raw error response
     * @return parsed error response object
     * @throws JsonProcessingException when parsing fails
     */
    private ErrorResponse processGameServerErrorResponseBody(String errorResponseBody)
            throws JsonProcessingException {
        ObjectMapper mapper = ObjectMapperConfig.createCustomObjectMapper();
        ErrorResponse errorResponse = mapper.readValue(errorResponseBody, ErrorResponse.class);
        validateErrorResponse(errorResponse);
        return errorResponse;
    }

    /**
     * Validate an error response object.
     *
     * @param errorResponse error response to validate
     * @throws IllegalArgumentException when validation fails
     */
    private void validateErrorResponse(ErrorResponse errorResponse)
            throws IllegalArgumentException {
        Validator validator = ValidatorConfig.createBeanValidator();
        Set<ConstraintViolation<ErrorResponse>> violations = validator.validate(errorResponse);
        if (!violations.isEmpty()) throw new IllegalArgumentException(violations.toString());
    }
}
