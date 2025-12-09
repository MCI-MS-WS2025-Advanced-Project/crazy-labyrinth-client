package mci.dibse.crazylabyrinth.client.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.io.IOException;
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
import mci.dibse.crazylabyrinth.client.domain.model.GameServer;
import mci.dibse.crazylabyrinth.client.infrastructure.api.dto.GameServerDTO;
import mci.dibse.crazylabyrinth.client.infrastructure.exceptions.GameManagementServerException;
import mci.dibse.crazylabyrinth.client.infrastructure.exceptions.GeneralUiException;
import mci.dibse.crazylabyrinth.client.infrastructure.management.ManagementServerClient;
import mci.dibse.crazylabyrinth.client.infrastructure.util.ObejctMapperConfig;
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

    private final ManagementServerClient client;

    private final Map<UUID, GameServer> servers = new ConcurrentHashMap<>();
    private final List<GameServerUpdateListener> listeners = new CopyOnWriteArrayList<>();

    @Autowired
    public GameServerManagementService(ManagementServerClient client) {
        this.client = client;
    }

    public void addListener(GameServerUpdateListener l) {
        listeners.add(l);
    }

    public void removeListener(GameServerUpdateListener l) {
        listeners.remove(l);
    }

    public Collection<GameServer> getServers() {
        return Collections.unmodifiableCollection(servers.values());
    }

    public void refreshGameServerList() throws GeneralUiException {
        try {
            String jsonString = client.getGameServers();
            processGameServerResponseBody(jsonString.getBytes(StandardCharsets.UTF_8));
        } catch (GameManagementServerException e) {
            log.error("Failed to retrieve game server list from management server", e);
            throwUiException(
                    "Failed to retrieve game server list from management server. Please try again"
                            + " later.");
        } catch (IOException e) {
            log.error("Failed to parse game server list from management server", e);
            throwUiException(
                    "Failed to retrieve game server list from management server. Please try again"
                            + " later.");
        } catch (IllegalArgumentException e) {
            log.error("Invalid game server list received from management server", e);
            throwUiException(
                    "Failed to retrieve game server list from management server. Please try again"
                            + " later.");
        }
    }

    private void throwUiException(String message) {
        throw new GeneralUiException(message);
    }

    private void processGameServerResponseBody(byte[] raw)
            throws IOException, IllegalArgumentException {
        List<GameServerDTO> gameServersDTO = parseGameServerDTOsFromBytes(raw);
        validateGameServerDTOs(gameServersDTO);
        List<GameServer> gameServers = mapToGameServers(gameServersDTO);
        refresh(gameServers);
    }

    private List<GameServerDTO> parseGameServerDTOsFromBytes(byte[] raw) throws IOException {
        ObjectMapper mapper = ObejctMapperConfig.createCustomObjectMapper();
        List<GameServerDTO> gameServersDTO = mapper.readValue(raw, new TypeReference<List<GameServerDTO>>() {});
        return gameServersDTO;
    }

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

    private List<GameServer> mapToGameServers(List<GameServerDTO> gameServersDTO) {
        List<GameServer> gameServers = new ArrayList<>();
        gameServersDTO.forEach(dto -> gameServers.add(new GameServer(dto)));
        return gameServers;
    }

    private void refresh(Collection<GameServer> updatedServers) {
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

}
