package mci.dibse.crazylabyrinth.client.test.unit;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import mci.dibse.crazylabyrinth.client.domain.model.management.GameServer;
import mci.dibse.crazylabyrinth.client.infrastructure.api.dto.GameServerDTO;
import mci.dibse.crazylabyrinth.client.infrastructure.management.ManagementServerClient;
import mci.dibse.crazylabyrinth.client.service.GameServerManagementService;
import mci.dibse.crazylabyrinth.client.service.GameServerUpdateListener;
import mci.dibse.crazylabyrinth.client.test.data.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
/**
 *  @author Julian Janetschek
 */
@ExtendWith(MockitoExtension.class)
public class GameServerManagementServiceTest {

    @Mock private ManagementServerClient client;
    @InjectMocks private GameServerManagementService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "servers", new ConcurrentHashMap<>());
    }

    @Test
    void getServers_initiallyEmpty_unmodifiable() {
        var servers = service.getServers();
        assertTrue(servers.isEmpty());
        assertThrows(UnsupportedOperationException.class, servers::clear);
    }

    @Test
    void refresh_addsAndNotifies() {

        List<GameServer> servers = TestDataFactory.createRandomGameServers(2);

        var notifyCount = new AtomicInteger();
        GameServerUpdateListener listener = updatedServers -> notifyCount.incrementAndGet();
        service.addListener(listener);

        ReflectionTestUtils.invokeMethod(service, "refresh", servers);

        assertEquals(1, notifyCount.get());
    }

    @Test
    void parse_DTO_from_valid_bytes() {
        byte[] raw = TestDataFactory.valid_getServers_ResponseBody().getBytes();

        try {
            List<GameServerDTO> dtos =
                    ReflectionTestUtils.invokeMethod(
                            service, "parseGameServerDTOsFromBytes", (Object) raw);
            assertNotNull(dtos);
            assertFalse(dtos.isEmpty());
        } catch (Exception e) {
            System.err.println(e.getCause());
            fail();
        }
    }

    @Test
    void parse_DTO_from_invalid_bytes() {
        byte[] raw = "invalid".getBytes();
        // using ReflectionTestUtils.invokeMethod does wrap checked exceptions in
        // UndeclaredThrowableException even if declared in throws
        var ex =
                assertThrows(
                        java.lang.reflect.UndeclaredThrowableException.class,
                        () ->
                                ReflectionTestUtils.invokeMethod(
                                        service, "parseGameServerDTOsFromBytes", (Object) raw));
        Throwable cause =
                ex.getUndeclaredThrowable() != null ? ex.getUndeclaredThrowable() : ex.getCause();
        assertNotNull(cause);
        assertInstanceOf(IOException.class, cause);
    }

    @Test
    void validate_gameServerDTOs_valid() {
        List<GameServerDTO> dtos = TestDataFactory.createRandomGameServersDTOs(2);
        assertDoesNotThrow(
                () -> ReflectionTestUtils.invokeMethod(service, "validateGameServerDTOs", dtos));
    }

    @Test
    void validate_gameServerDTOs_empty() {
        List<GameServerDTO> dtos = TestDataFactory.createRandomGameServersDTOs(0);
        assertDoesNotThrow(
                () -> ReflectionTestUtils.invokeMethod(service, "validateGameServerDTOs", dtos));
    }

    @Test
    void validate_gameServerDTOs_invalid() {
        List<GameServerDTO> dtos = List.of(TestDataFactory.createInvalidGameServersDTO());
        // using ReflectionTestUtils.invokeMethod does not wrap runtime exceptions
        assertThrows(
                IllegalArgumentException.class,
                () -> ReflectionTestUtils.invokeMethod(service, "validateGameServerDTOs", dtos));
    }

    @Test
    void map_to_gameServers_valid() {
        List<GameServerDTO> dtos = TestDataFactory.createRandomGameServersDTOs(2);
        List<GameServer> servers =
                ReflectionTestUtils.invokeMethod(service, "mapToGameServers", dtos);
        assertNotNull(servers);
        assertEquals(2, servers.size());
    }

    @Test
    void refresh_gameServers_valid_allNewServers() {
        List<GameServer> servers = TestDataFactory.createRandomGameServers(4);
        ReflectionTestUtils.invokeMethod(service, "refresh", servers);

        var expected = servers.stream().sorted(Comparator.comparing(GameServer::getId)).toList();
        var actual =
                service.getServers().stream()
                        .sorted(Comparator.comparing(GameServer::getId))
                        .toList();
        assertEquals(expected, actual);
    }

    @Test
    void refresh_gameServers_valid_someExistingServers() {
        List<GameServer> servers = TestDataFactory.createRandomGameServers(4);

        Map<UUID, GameServer> existingServers = new ConcurrentHashMap<>();
        servers.forEach(
                gs -> {
                    existingServers.put(gs.getId(), gs);
                });
        ReflectionTestUtils.setField(service, "servers", existingServers);

        GameServer gs = servers.get(0);
        int currentPlayerCount = gs.getCurrentPlayerCount();

        int newPlayerCount = (currentPlayerCount + 1) % 5;
        gs.setCurrentPlayerCount(newPlayerCount);

        UUID id = gs.getId();
        ReflectionTestUtils.invokeMethod(service, "refresh", servers);

        Map<UUID, GameServer> refreshedServers =
                (Map<UUID, GameServer>) ReflectionTestUtils.getField(service, "servers");
        GameServer rgs = refreshedServers.get(id);
        assertNotNull(rgs);
        assertEquals(newPlayerCount, rgs.getCurrentPlayerCount());
    }

    @Test
    void refresh_gameServers_valid_removeSomeServers() {
        List<GameServer> servers = TestDataFactory.createRandomGameServers(4);
        assert servers.size() == 4;
        Map<UUID, GameServer> existingServers = new ConcurrentHashMap<>();
        servers.forEach(
                gs -> {
                    existingServers.put(gs.getId(), gs);
                });
        ReflectionTestUtils.setField(service, "servers", existingServers);

        servers.removeFirst();
        assert servers.size() == 3;

        ReflectionTestUtils.invokeMethod(service, "refresh", servers);
        Map<UUID, GameServer> refreshedServers =
                (Map<UUID, GameServer>) ReflectionTestUtils.getField(service, "servers");
        assertEquals(3, refreshedServers.size());
    }

    @Test
    void refresh_gameServers_emtpy() {
        assertDoesNotThrow(
                () ->
                        ReflectionTestUtils.invokeMethod(
                                service, "refresh", Collections.emptyList()));
    }

    @Test
    void refresh_gameServers_emtpyUpdate_removeExistingServers() {
        List<GameServer> servers = TestDataFactory.createRandomGameServers(4);
        assert servers.size() == 4;
        Map<UUID, GameServer> existingServers = new ConcurrentHashMap<>();
        servers.forEach(
                gs -> {
                    existingServers.put(gs.getId(), gs);
                });
        ReflectionTestUtils.setField(service, "servers", existingServers);

        assertDoesNotThrow(
                () ->
                        ReflectionTestUtils.invokeMethod(
                                service, "refresh", Collections.emptyList()));

        Map<UUID, GameServer> refreshedServers =
                (Map<UUID, GameServer>) ReflectionTestUtils.getField(service, "servers");
        assertEquals(0, refreshedServers.size());
    }
}
