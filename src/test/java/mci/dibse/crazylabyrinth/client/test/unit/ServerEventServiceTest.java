package mci.dibse.crazylabyrinth.client.test.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import labyrinth.contracts.models.*;
import labyrinth.contracts.models.LobbyStateEventPayload;
import labyrinth.contracts.models.NextTreasureCardEventPayload;
import mci.dibse.crazylabyrinth.client.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class ServerEventServiceTest {

    private ServerEventService eventService;
    private LobbyService lobbyServiceMock;
    private GameService gameServiceMock;
    private ApplicationEventPublisher eventPublisherMock;
    private TimerService timerServiceMock;
    private AchievementService achievementService;

    @BeforeEach
    void setUp() {
        // GIVEN: ServerEventService is initialized with mocked dependencies
        lobbyServiceMock = mock(LobbyService.class);
        gameServiceMock = mock(GameService.class);
        eventPublisherMock = mock(ApplicationEventPublisher.class);
        timerServiceMock = mock(TimerService.class);
        achievementService = mock(AchievementService.class);

        eventService =
                new ServerEventService(
                        lobbyServiceMock,
                        gameServiceMock,
                        eventPublisherMock,
                        timerServiceMock,
                        achievementService);
    }

    @Test
    @DisplayName("UT-EVENT-01 & 02 - ServerInfoEvent is recognized correctly")
    void testServerInfoEvent_UT_01_02() {
        // GIVEN: Valid JSON representing a server info update
        String json = "{\"type\":\"SERVER_INFO\",\"serverName\":\"TestServer\"}";

        // WHEN: Event is processed
        assertDoesNotThrow(() -> eventService.handleEvent(json));

        // THEN: Primarily verifying that no exceptions occur during parsing/routing
        System.out.println("UT-EVENT-01/02 Result: ServerInfo parsed without exception.");
    }

    @Test
    @DisplayName("UT-EVENT-03 - LobbyStateEvent is successfully deserialized")
    void testLobbyStateEvent_UT_03() {
        // GIVEN: JSON for LobbyState representing an unstarted game
        String json = "{\"type\":\"LOBBY_STATE\",\"gameStarted\":false,\"players\":[]}";

        // WHEN: Event is processed
        eventService.handleEvent(json);

        // THEN: Verify that the event was delegated to the LobbyService
        verify(lobbyServiceMock).handleLobbyState(any(LobbyStateEventPayload.class));
        System.out.println(
                "UT-EVENT-03 Result: LobbyStateEvent successfully delegated to LobbyService.");
    }

    @Test
    @DisplayName("UT-EVENT-04 - TreasureCard event updates the model")
    void testTreasureCardEvent_UT_04() {
        // GIVEN: JSON for a new treasure card assignment
        String json = "{\"type\":\"NEXT_TREASURE\",\"nextTreasure\":{\"symbol\":\"DIAMOND\"}}";

        // WHEN: Event is processed
        eventService.handleEvent(json);

        // THEN: Verify that the event was delegated to the GameService
        verify(gameServiceMock).handleGameEvent(any(NextTreasureCardEventPayload.class));
        System.out.println(
                "UT-EVENT-04 Result: Treasure event successfully delegated to GameService.");
    }

    @Test
    @DisplayName("UT-EVENT-05 - Malformed Server Event is intercepted safely")
    void testMalformedEvent_UT_05() {
        // GIVEN: Strictly invalid/broken JSON
        String malformedJson = "{ \"type\": \"UNKNOWN\", ";

        // WHEN & THEN: Client must remain stable (handled by internal catch block)
        assertDoesNotThrow(() -> eventService.handleEvent(malformedJson));

        // Ensure no delegation to services occurred due to parsing failure
        verifyNoInteractions(lobbyServiceMock, gameServiceMock);
        System.out.println(
                "UT-EVENT-05 Result: Malformed JSON handled via catch-block; client remains"
                        + " stable.");
    }
}
