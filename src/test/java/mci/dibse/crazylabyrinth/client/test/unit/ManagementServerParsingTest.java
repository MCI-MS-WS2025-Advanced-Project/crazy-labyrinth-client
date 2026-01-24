package mci.dibse.crazylabyrinth.client.test.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collection;
import mci.dibse.crazylabyrinth.client.domain.model.management.GameServer;
import mci.dibse.crazylabyrinth.client.infrastructure.exceptions.GeneralUiException;
import mci.dibse.crazylabyrinth.client.infrastructure.management.ManagementServerClient;
import mci.dibse.crazylabyrinth.client.service.GameServerManagementService;
import mci.dibse.crazylabyrinth.client.test.data.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ManagementServerParsingTest {

    private GameServerManagementService gameServerService;

    @Mock private ManagementServerClient clientMock;

    @BeforeEach
    void setUp() {
        // GIVEN: Service is initialized with a mocked infrastructure client
        gameServerService = new GameServerManagementService(clientMock);
    }

    @Test
    @DisplayName("UT-SERVERLIST-01 - Parsing the server list (Valid Data)")
    void testParsingServerList_UT_SERVERLIST_01() throws Exception {
        // GIVEN: A valid JSON response body from the management server
        String jsonBody = TestDataFactory.valid_getServers_ResponseBody();
        when(clientMock.getGameServers()).thenReturn(jsonBody);

        // WHEN: Refreshing the server list
        gameServerService.refreshGameServerList();

        // THEN: The server collection should not be empty
        Collection<GameServer> servers = gameServerService.getServers();
        assertFalse(servers.isEmpty());

        // Log output for documentation purposes
        System.out.println(
                "UT-SERVERLIST-01 Result: Successfully parsed " + servers.size() + " servers.");
        servers.forEach(
                s ->
                        System.out.println(
                                " -> Found Server: " + s.getName() + " [" + s.getId() + "]"));
    }

    @Test
    @DisplayName("UT-SERVERLIST-02 - Parsing the server list (Empty List)")
    void testParsingEmptyServerList_UT_SERVERLIST_02() throws Exception {
        // GIVEN: An empty JSON array response
        when(clientMock.getGameServers()).thenReturn("[]");

        // WHEN: Refreshing the list
        gameServerService.refreshGameServerList();

        // THEN: Resulting server size should be 0
        System.out.println("UT-SERVERLIST-02 Result: Successfully handled empty server list.");
        assertEquals(0, gameServerService.getServers().size());
    }

    @Test
    @DisplayName("UT-SERVERLIST-03 - Invalid server list is intercepted")
    void testParsingInvalidJson_UT_SERVERLIST_03() throws Exception {
        // GIVEN: Intentionally broken/malformed JSON
        when(clientMock.getGameServers()).thenReturn("{ \"invalid\": ");

        // WHEN & THEN: Verify that a GeneralUiException is thrown due to parsing error
        GeneralUiException ex =
                assertThrows(
                        GeneralUiException.class,
                        () -> {
                            gameServerService.refreshGameServerList();
                        });

        // Confirmation of successful error handling
        System.out.println(
                "UT-SERVERLIST-03 Result: Exception correctly caught. Message: " + ex.getMessage());
    }
}
