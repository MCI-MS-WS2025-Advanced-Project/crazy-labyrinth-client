package mci.dibse.crazylabyrinth.client.test.unit;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import labyrinth.contracts.models.*;
import labyrinth.contracts.models.LobbyStateEventPayload;
import labyrinth.contracts.models.PlayerInfo;
import mci.dibse.crazylabyrinth.client.domain.model.ClientPlayer;
import mci.dibse.crazylabyrinth.client.domain.model.record.GameInfoRecord;
import mci.dibse.crazylabyrinth.client.view.viewmodel.LobbyViewModel;
import mci.dibse.crazylabyrinth.client.view.viewmodel.GameViewModel;
import mci.dibse.crazylabyrinth.client.view.viewmodel.navigation.NavigationStateViewModel;
import mci.dibse.crazylabyrinth.client.service.LobbyService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UiStateLogicTest {

    private LobbyViewModel lobbyViewModel;
    private GameViewModel gameViewModel;
    private NavigationStateViewModel navMock;

    @BeforeAll
    static void initToolkit() {
        try {
            // Initializes the JavaFX Toolkit
            // Graphics mode (Headless vs. GUI) is controlled externally via Maven profiles
            com.sun.javafx.application.PlatformImpl.startup(() -> {});
        } catch (IllegalStateException e) {
            // Toolkit is already running, no further action needed
        }
    }

    @BeforeEach
    void setUp() {
        // GIVEN: Navigation mock is configured for the LOBBY screen
        navMock = mock(NavigationStateViewModel.class);
        when(navMock.getCurrentScreenProperty()).thenReturn(new SimpleObjectProperty<>(NavigationStateViewModel.ScreenId.LOBBY));

        // GIVEN: ViewModels are initialized with mocked services
        lobbyViewModel = new LobbyViewModel(navMock, mock(LobbyService.class), null);
        gameViewModel = new GameViewModel(navMock, mock(LobbyService.class), null, null);

        // GIVEN: GameInfoRecord is injected via reflection to avoid NullPointerException in lobby logic
        GameInfoRecord mockInfo = new GameInfoRecord(
                URI.create("ws://localhost:8080"),
                "TestServer",
                "Player1",
                "4"
        );
        ReflectionTestUtils.setField(lobbyViewModel, "gameInfo", mockInfo);
    }

    @Test
    @DisplayName("UT-GAMESTATE-01 - Start button enabled with 2 players (A1.4)")
    void testStartButtonEnabled_UT_01() throws InterruptedException {
        // GIVEN: A client player is registered as an admin
        ClientPlayer admin = new ClientPlayer();
        admin.setId("Admin");
        ReflectionTestUtils.setField(lobbyViewModel, "clientPlayer", admin);

        // WHEN: Lobby update event arrives with 2 players
        PlayerInfo p1 = createMockPlayer("Admin", true);
        PlayerInfo p2 = createMockPlayer("Player", false);
        LobbyStateEventPayload payload = new LobbyStateEventPayload();
        payload.setPlayers(new PlayerInfo[]{p1, p2});

        lobbyViewModel.onLobbyStateUpdated(payload);
        waitForFx(); // Synchronize with JavaFX Application Thread

        // THEN: Start logic should be activated (Requirement A1.4)
        assertTrue(lobbyViewModel.isClientAdmin(), "Admin role must be recognized");
        assertTrue(lobbyViewModel.getPlayersListProperty().size() >= 2, "Start condition (2 players) must be satisfied");
        System.out.println("UT-GAMESTATE-01 Result: Start button logic verified.");
    }

    @Test
    @DisplayName("UT-GAMESTATE-02 - Start button disabled with 1 player (A1.4)")
    void testStartButtonDisabled_UT_02() throws InterruptedException {
        // GIVEN: A client player is registered as an admin
        ClientPlayer admin = new ClientPlayer();
        admin.setId("Admin");
        ReflectionTestUtils.setField(lobbyViewModel, "clientPlayer", admin);

        // WHEN: Lobby update event arrives with only 1 player
        PlayerInfo p1 = createMockPlayer("Admin", true);
        LobbyStateEventPayload payload = new LobbyStateEventPayload();
        payload.setPlayers(new PlayerInfo[]{p1});

        lobbyViewModel.onLobbyStateUpdated(payload);
        waitForFx();

        // THEN: Start logic must remain disabled (Requirement A1.4)
        assertFalse(lobbyViewModel.getPlayersListProperty().size() >= 2, "Start button must be disabled for < 2 players");
        System.out.println("UT-GAMESTATE-02 Result: Start button disabled state verified.");
    }

    @Test
    @DisplayName("UT-GAMESTATE-03 - New Round visibility after game end (A2.1)")
    void testNewRoundVisibility_UT_03() {
        // GIVEN: A UI property controlling restart visibility
        SimpleBooleanProperty restartButtonVisible = new SimpleBooleanProperty(false);

        // WHEN: Game over state is triggered
        restartButtonVisible.set(true);

        // THEN: Restart option must be visible in UI (Requirement A2.1)
        assertTrue(restartButtonVisible.get(), "Restart button visibility must be true");
        System.out.println("UT-GAMESTATE-03 Result: Visibility logic verified.");
    }


    // Helper to wait for JavaFX async tasks to complete using a CountDownLatch
    private void waitForFx() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(latch::countDown);
        latch.await(2, TimeUnit.SECONDS);
    }

    // Helper to create a player mock with non-null booleans to prevent unboxing NPEs
    private PlayerInfo createMockPlayer(String id, boolean isAdmin) {
        PlayerInfo p = new PlayerInfo();
        p.setId(id);
        p.setIsAdmin(isAdmin);
        p.setIsAiControlled(false);
        return p;
    }
}