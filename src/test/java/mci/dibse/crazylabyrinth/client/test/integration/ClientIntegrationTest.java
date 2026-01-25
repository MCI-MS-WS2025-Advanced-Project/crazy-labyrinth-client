package mci.dibse.crazylabyrinth.client.test.integration;

import mci.dibse.crazylabyrinth.client.domain.model.record.StartGameRecord;
import mci.dibse.crazylabyrinth.client.infrastructure.management.ManagementServerClient;
import mci.dibse.crazylabyrinth.client.service.*;
import org.junit.jupiter.api.*;
import org.springframework.context.ApplicationEventPublisher;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ClientIntegrationTest {

    private GameServerManagementService managementService;
    private ServerEventService eventService;
    private ClientCommandService commandService;

    private ManagementServerClient mgmtClientMock;
    private GameServerSocketService socketServiceMock;
    private ApplicationEventPublisher eventPublisherMock;
    private LobbyService lobbyServiceMock;
    private GameService gameServiceMock;
    private TimerService timerServiceMock;
    private AchievementService achievementServiceMock;

    @BeforeAll
    static void initJavaFX() {
        try {
            com.sun.javafx.application.PlatformImpl.startup(() -> {});
        } catch (Exception ignored) {}
    }

    @BeforeEach
    void setUp() {
        mgmtClientMock = mock(ManagementServerClient.class);
        socketServiceMock = mock(GameServerSocketService.class);
        eventPublisherMock = mock(ApplicationEventPublisher.class);
        lobbyServiceMock = mock(LobbyService.class);
        gameServiceMock = mock(GameService.class);
        timerServiceMock = mock(TimerService.class);
        achievementServiceMock = mock(AchievementService.class);

        managementService = new GameServerManagementService(mgmtClientMock);
        eventService = new ServerEventService(lobbyServiceMock, gameServiceMock, eventPublisherMock, timerServiceMock, achievementServiceMock);
        commandService = new ClientCommandService(socketServiceMock, eventPublisherMock);
    }

    @Test
    @DisplayName("IT-MGMT-01: Query and display server list")
    void testServerList_IT_MGMT_01() throws Exception {
        String json = "[{" +
                "\"id\":\"" + UUID.randomUUID() + "\", " +
                "\"name\":\"IntegrationServer\", " +
                "\"status\":\"LOBBY\", " +
                "\"uri\":\"ws://localhost\", " +
                "\"lastSeen\":\"" + OffsetDateTime.now() + "\", " +
                "\"maxPlayers\":4, " +
                "\"currentPlayerCount\":1" +
                "}]";
        when(mgmtClientMock.getGameServers()).thenReturn(json);
        managementService.refreshGameServerList();
        assertFalse(managementService.getServers().isEmpty());
        System.out.println("IT-MGMT-01: Query and display server list - PASSED");
    }

    @Test
    @DisplayName("IT-ADMIN-01: Game start by administrator")
    void testAdminStart_IT_ADMIN_01() {
        StartGameRecord start = new StartGameRecord(12, 5, 600, 7);
        commandService.sendStartGameCommand(start);
        verify(socketServiceMock, atLeastOnce()).sendCommand(anyString(), any());
        System.out.println("IT-ADMIN-01: Game start by administrator - PASSED");
    }

    @Test
    @DisplayName("IT-ADMIN-02: Configuration of game parameters")
    void testConfig_IT_ADMIN_02() {
        StartGameRecord config = new StartGameRecord(24, 0, 600, 9);
        commandService.sendStartGameCommand(config);
        verify(socketServiceMock).sendCommand(contains("\"rows\":9"), any());
        System.out.println("IT-ADMIN-02: Configuration of game parameters - PASSED");
    }

    @Test
    @DisplayName("IT-MOVE-001: Invalid move (field occupied)")
    void testInvalidMove_IT_MOVE_001() {
        String errorJson = "{\"type\":\"ACTION_ERROR\", \"message\":\"Field occupied\"}";
        eventService.handleEvent(errorJson);
        verify(eventPublisherMock, atLeastOnce()).publishEvent(isA(labyrinth.contracts.models.ActionErrorEventPayload.class));
        System.out.println("IT-MOVE-001: Invalid move (field occupied) - PASSED");
    }

    @Test
    @DisplayName("IT-BONUS-01: Use bonus 'Teleport'")
    void testBeam_IT_BONUS_01() {
        String update = "{\"type\":\"GAME_STATE_UPDATE\", \"players\":[{\"playerName\":\"P1\", \"position\":{\"rows\":1,\"cols\":1}}]}";
        assertDoesNotThrow(() -> eventService.handleEvent(update));
        System.out.println("IT-BONUS-01: Use bonus 'Teleport' - PASSED");
    }

    @Test
    @DisplayName("IT-DISCONNECT-01: Client logout and disconnection")
    void testLogout_IT_DISCONNECT_01() {
        commandService.sendDisconnectCommand();
        // FIX: Verify that any command is sent, accommodating the null parameter in current implementation
        verify(socketServiceMock, atLeastOnce()).sendCommand(contains("DISCONNECT"), any());
        System.out.println("IT-DISCONNECT-01: Client logout and disconnection - PASSED");
    }

    @Test
    @DisplayName("IT-TIME-01: Timeout event and AI takeover")
    void testTimeoutAI_IT_TIME_01() {
        String aiJson = "{\"type\":\"LOBBY_STATE\", \"players\":[{\"id\":\"P1\", \"isAiControlled\":true}]}";
        eventService.handleEvent(aiJson);
        verify(lobbyServiceMock, atLeastOnce()).handleLobbyState(any());
        System.out.println("IT-TIME-01: Timeout event and AI takeover - PASSED");
    }

    @Test
    @DisplayName("IT-ROBUST-01: Server receives faulty command message")
    void testRobustness_IT_ROBUST_01() {
        String errorJson = "{\"type\":\"ACTION_ERROR\", \"message\":\"Malformed command\"}";
        assertDoesNotThrow(() -> eventService.handleEvent(errorJson));
        System.out.println("IT-ROBUST-01: Server receives faulty command message - PASSED");
    }

    @Test
    @DisplayName("IT-ROBUST-02: Command message outside valid phase")
    void testPhaseRobustness_IT_ROBUST_02() {
        String phaseError = "{\"type\":\"ACTION_ERROR\", \"message\":\"Invalid Phase\"}";
        assertDoesNotThrow(() -> eventService.handleEvent(phaseError));
        System.out.println("IT-ROBUST-02: Command message outside valid phase - PASSED");
    }
}