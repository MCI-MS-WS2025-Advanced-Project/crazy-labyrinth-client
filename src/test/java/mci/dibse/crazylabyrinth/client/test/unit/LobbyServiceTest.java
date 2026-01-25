package mci.dibse.crazylabyrinth.client.test.unit;

import labyrinth.contracts.models.ConnectAckEventPayload;
import labyrinth.contracts.models.LobbyStateEventPayload;
import mci.dibse.crazylabyrinth.client.service.LobbyService;
import mci.dibse.crazylabyrinth.client.service.LobbyStateUpdateListener;
import mci.dibse.crazylabyrinth.client.service.PlayerConnectedAckListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

class LobbyServiceTest {

    private LobbyService lobbyService;

    @BeforeEach
    void setUp() {
        lobbyService = new LobbyService();
    }

    @Test
    @DisplayName("UT-SERV-03 - LobbyService listener registration and notification")
    void testLobbyStateNotification() {
        System.out.println("Starting UT-SERV-03: Testing LobbyState listeners...");

        // GIVEN: A mocked listener and payload
        LobbyStateUpdateListener mockListener = mock(LobbyStateUpdateListener.class);
        LobbyStateEventPayload payload = mock(LobbyStateEventPayload.class);

        // WHEN: Adding listener and handling state
        lobbyService.addLobbyListener(mockListener);
        lobbyService.handleLobbyState(payload);

        // THEN: Listener must be notified
        verify(mockListener, times(1)).onLobbyStateUpdated(payload);

        // AND: Removing listener stops notifications
        lobbyService.removeLobbyListener(mockListener);
        lobbyService.handleLobbyState(payload);
        verify(mockListener, times(1)).onLobbyStateUpdated(payload); // Count remains 1

        System.out.println("Result: Lobby listener logic verified (100% method coverage for these parts).");
    }

    @Test
    @DisplayName("UT-SERV-04 - LobbyService player connection handling")
    void testConnectAckNotification() {
        System.out.println("Starting UT-SERV-04: Testing Player connection acknowledgement...");

        // GIVEN: A mocked listener and real payload
        PlayerConnectedAckListener mockListener = mock(PlayerConnectedAckListener.class);
        ConnectAckEventPayload payload = new ConnectAckEventPayload();
        payload.setIdentifierToken("token-123");
        payload.setPlayerId("player-456");

        lobbyService.addPlayerListener(mockListener);

        // WHEN: Handling connection ack
        lobbyService.handleConnectAck(payload);

        // THEN: Verify the listener received the correctly mapped ClientPlayer
        verify(mockListener).onPlayerConnected(argThat(player ->
                player.getId().equals("player-456") &&
                        player.getIdentifierToken().equals("token-123")
        ));

        System.out.println("Result: Player connection mapping and notification verified.");
    }
}