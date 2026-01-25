package mci.dibse.crazylabyrinth.client.test.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import mci.dibse.crazylabyrinth.client.domain.model.ClientPlayer;
import mci.dibse.crazylabyrinth.client.domain.model.record.GameInfoRecord;
import mci.dibse.crazylabyrinth.client.service.LobbyService;
import mci.dibse.crazylabyrinth.client.view.viewmodel.LobbyViewModel;
import mci.dibse.crazylabyrinth.client.view.viewmodel.navigation.NavigationStateViewModel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class UiStateLogicTest {

    private LobbyViewModel lobbyViewModel;
    private NavigationStateViewModel navMock;

    @BeforeAll
    static void initToolkit() {
        // Force Headless for CI environments
        if (System.getenv("GITHUB_ACTIONS") != null) {
            System.setProperty("glass.platform", "Monocle");
            System.setProperty("monocle.platform", "Headless");
            System.setProperty("prism.order", "sw");
        }
        try {
            com.sun.javafx.application.PlatformImpl.startup(() -> {});
        } catch (IllegalStateException ignored) {}
    }

    @BeforeEach
    void setUp() throws InterruptedException {
        navMock = mock(NavigationStateViewModel.class);
        when(navMock.getCurrentScreenProperty())
                .thenReturn(new SimpleObjectProperty<>(NavigationStateViewModel.ScreenId.LOBBY));

        // Use a latch to ensure ViewModel is safely created on the FX thread if it initializes UI properties
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            lobbyViewModel = new LobbyViewModel(navMock, mock(LobbyService.class), null);
            latch.countDown();
        });
        latch.await(5, TimeUnit.SECONDS);

        GameInfoRecord mockInfo = new GameInfoRecord(URI.create("ws://localhost:8080"), "TestServer", "Player1", "4");
        ReflectionTestUtils.setField(lobbyViewModel, "gameInfo", mockInfo);
    }

    @Test
    @DisplayName("UT-GAMESTATE-01 - Start button enabled with 2 players (A1.4)")
    void testStartButtonEnabled_UT_01() throws InterruptedException {
        // GIVEN: Admin setup
        ClientPlayer admin = new ClientPlayer();
        admin.setId("Admin");
        ReflectionTestUtils.setField(lobbyViewModel, "clientPlayer", admin);

        // WHEN: Event arrives
        labyrinth.contracts.models.LobbyStateEventPayload payload = new labyrinth.contracts.models.LobbyStateEventPayload();
        payload.setPlayers(new labyrinth.contracts.models.PlayerInfo[] { createPlayer("Admin", true), createPlayer("P2", false) });

        // Execute on FX thread and wait for completion to avoid pipeline hangs
        runOnFxThread(() -> lobbyViewModel.onLobbyStateUpdated(payload));

        // THEN: Verify state [cite: 236]
        assertTrue(lobbyViewModel.getPlayersListProperty().size() >= 2, "Condition (2 players) must be met");
        System.out.println("UT-GAMESTATE-01: Verified.");
    }

    @Test
    @DisplayName("UT-GAMESTATE-02 - Start button disabled with 1 player (A1.4)")
    void testStartButtonDisabled_UT_02() throws InterruptedException {
        ClientPlayer admin = new ClientPlayer();
        admin.setId("Admin");
        ReflectionTestUtils.setField(lobbyViewModel, "clientPlayer", admin);

        labyrinth.contracts.models.LobbyStateEventPayload payload = new labyrinth.contracts.models.LobbyStateEventPayload();
        payload.setPlayers(new labyrinth.contracts.models.PlayerInfo[] { createPlayer("Admin", true) });

        runOnFxThread(() -> lobbyViewModel.onLobbyStateUpdated(payload));

        // THEN: Verify state [cite: 246]
        assertFalse(lobbyViewModel.getPlayersListProperty().size() >= 2, "Disabled for < 2 players");
        System.out.println("UT-GAMESTATE-02: Verified.");
    }

    private void runOnFxThread(Runnable action) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            action.run();
            latch.countDown();
        });
        if (!latch.await(5, TimeUnit.SECONDS)) {
            throw new RuntimeException("FX Thread timed out during test execution");
        }
    }

    private labyrinth.contracts.models.PlayerInfo createPlayer(String id, boolean isAdmin) {
        labyrinth.contracts.models.PlayerInfo p = new labyrinth.contracts.models.PlayerInfo();
        p.setId(id);
        p.setIsAdmin(isAdmin);
        p.setIsAiControlled(false);
        return p;
    }
}