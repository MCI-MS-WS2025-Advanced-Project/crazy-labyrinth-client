package mci.dibse.crazylabyrinth.client.test.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
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

    private static final String ONE_VALID_ONE_SCHEMELESS_SERVER =
            """
            [
              {
                "id": "7b5f0b3b-3d2a-4f69-a2d0-8bfa2ec1c9c1",
                "name": "Missing scheme",
                "uri": "172.16.20.21:42080",
                "maxPlayers": 4,
                "currentPlayerCount": 0,
                "status": "LOBBY",
                "lastSeen": "2025-12-05T12:07:21Z"
              },
              {
                "id": "0f2e6d71-3a90-4d7a-9f3a-5d6d3d5b2b41",
                "name": "Valid",
                "uri": "wss://example.com:443/game",
                "maxPlayers": 4,
                "currentPlayerCount": 1,
                "status": "LOBBY",
                "lastSeen": "2025-12-05T11:58:03Z"
              }
            ]
            """;

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
    void parse_DTO_skipsEntryWithUnparseableUri() {
        List<GameServerDTO> dtos =
                ReflectionTestUtils.invokeMethod(
                        service,
                        "parseGameServerDTOsFromBytes",
                        (Object) ONE_VALID_ONE_SCHEMELESS_SERVER.getBytes());

        assertNotNull(dtos);
        assertEquals(1, dtos.size());
        assertEquals("Valid", dtos.get(0).getName());
    }

    @Test
    void parse_DTO_rejectsNonArrayBody() {
        byte[] raw = "{\"error\":\"nope\"}".getBytes();
        var ex =
                assertThrows(
                        UndeclaredThrowableException.class,
                        () ->
                                ReflectionTestUtils.invokeMethod(
                                        service, "parseGameServerDTOsFromBytes", (Object) raw));
        assertInstanceOf(IOException.class, ex.getUndeclaredThrowable());
    }

    @Test
    void removeInvalid_gameServerDTOs_keepsValid() {
        List<GameServerDTO> dtos = TestDataFactory.createRandomGameServersDTOs(2);
        List<GameServerDTO> result =
                ReflectionTestUtils.invokeMethod(service, "removeInvalidGameServerDTOs", dtos);
        assertEquals(2, result.size());
    }

    @Test
    void removeInvalid_gameServerDTOs_empty() {
        List<GameServerDTO> dtos = TestDataFactory.createRandomGameServersDTOs(0);
        List<GameServerDTO> result =
                ReflectionTestUtils.invokeMethod(service, "removeInvalidGameServerDTOs", dtos);
        assertTrue(result.isEmpty());
    }

    @Test
    void removeInvalid_gameServerDTOs_dropsOnlyInvalid() {
        List<GameServerDTO> dtos = new ArrayList<>(TestDataFactory.createRandomGameServersDTOs(2));
        dtos.add(TestDataFactory.createInvalidGameServersDTO());
        List<GameServerDTO> result =
                ReflectionTestUtils.invokeMethod(service, "removeInvalidGameServerDTOs", dtos);
        assertEquals(2, result.size());
    }

    @Test
    void refreshGameServerList_oneBadEntry_keepsOtherServers() throws Exception {
        when(client.getGameServers()).thenReturn(ONE_VALID_ONE_SCHEMELESS_SERVER);

        assertDoesNotThrow(() -> service.refreshGameServerList());

        assertEquals(1, service.getServers().size());
        assertEquals("Valid", service.getServers().iterator().next().getName());
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
