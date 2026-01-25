package mci.dibse.crazylabyrinth.client.test.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import labyrinth.contracts.models.RankingEntry;
import mci.dibse.crazylabyrinth.client.NavigationController;
import mci.dibse.crazylabyrinth.client.infrastructure.util.ScreenResolutionProperties;
import mci.dibse.crazylabyrinth.client.service.AchievementService;
import mci.dibse.crazylabyrinth.client.view.controller.impl.LobbyController;
import mci.dibse.crazylabyrinth.client.view.viewmodel.GameViewModel;
import mci.dibse.crazylabyrinth.client.view.viewmodel.LobbyViewModel;
import mci.dibse.crazylabyrinth.client.view.viewmodel.navigation.NavigationStateViewModel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class LobbyControllerTest {

    @BeforeAll
    static void initToolkit() {
        // Check if we are running in a headless environment (CI)
        // Most CI runners set 'headless' or 'GITHUB_ACTIONS'
        boolean isCi = System.getenv("GITHUB_ACTIONS") != null;
        if (isCi) {
            System.setProperty("glass.platform", "Monocle");
            System.setProperty("monocle.platform", "Headless");
            System.setProperty("prism.order", "sw");
        }

        try {
            com.sun.javafx.application.PlatformImpl.startup(() -> {});
        } catch (IllegalStateException e) {
            // Toolkit already initialized
        }
    }

    @Test
    @DisplayName("UT-VIEW-04 - LobbyController initialization path")
    void testLobbyInitialization_UT_VIEW_04() {
        // GIVEN: Mock dependencies
        NavigationStateViewModel mockNavState = mock(NavigationStateViewModel.class);
        LobbyViewModel mockLobbyVM = mock(LobbyViewModel.class);

        // Use RETURNS_MOCKS to prevent NPEs during deep calls (e.g., Ratio enum)
        ScreenResolutionProperties mockRes = mock(ScreenResolutionProperties.class, RETURNS_MOCKS);
        GameViewModel mockGameVM = mock(GameViewModel.class);
        AchievementService mockAchievementService = mock(AchievementService.class);
        NavigationController mockNavController = mock(NavigationController.class);

        // FIX for BaseViewCleanup NPE: Property must exist before controller instantiation
        when(mockNavState.getCurrentScreenProperty()).thenReturn(new SimpleObjectProperty<>());

        // GIVEN: Mocking ViewModel Properties to prevent NPEs during JavaFX binding
        when(mockLobbyVM.getPlayersListProperty())
                .thenReturn(new SimpleListProperty<>(FXCollections.observableArrayList()));
        when(mockLobbyVM.isClientAdmin()).thenReturn(true);
        when(mockLobbyVM.getMaxPlayersProperty()).thenReturn(new SimpleStringProperty("4"));
        when(mockLobbyVM.getPlayerCountProperty()).thenReturn(new SimpleStringProperty("1"));
        when(mockLobbyVM.getGameNameProperty())
                .thenReturn(new SimpleStringProperty("Test Labyrinth"));
        when(mockLobbyVM.getHasError()).thenReturn(new SimpleBooleanProperty(false));
        when(mockLobbyVM.getErrorMessage()).thenReturn(new SimpleStringProperty(""));

        // FIX for Generics: Correctly typed SortedList for RankingEntry
        ObservableList<RankingEntry> baseList = FXCollections.observableArrayList();
        SortedList<RankingEntry> mockSortedList = new SortedList<>(baseList);

        // GIVEN: Mocking default values for Spinners and Checkboxes
        when(mockLobbyVM.getGameDefaultBonusCount()).thenReturn(0);
        when(mockLobbyVM.getGameDefaultTreasureCountPerPlayer()).thenReturn(6);
        when(mockLobbyVM.getDefaultBoardSize()).thenReturn(7);
        when(mockLobbyVM.getDefaultTimer()).thenReturn(3600);
        when(mockLobbyVM.isDefaultTimerEnabled()).thenReturn(false);
        when(mockLobbyVM.isDefaultBonusEnabled()).thenReturn(false);

        // WHEN: Instantiate Controller
        LobbyController controller =
                new LobbyController(
                        mockNavState,
                        mockLobbyVM,
                        mockRes,
                        mockGameVM,
                        mockAchievementService,
                        mockNavController);

        // GIVEN: Inject FXML fields manually using Reflection
        injectFxmlFields(controller);

        // THEN: Ensure initialize does not throw exceptions
        assertDoesNotThrow(
                controller::initialize,
                "Initialization failed. Check field names or missing properties.");
        System.out.println(
                "UT-VIEW-04 Result: LobbyController successfully initialized. Symbol errors"
                        + " fixed.");
    }

    private void injectFxmlFields(LobbyController controller) {
        // Player Table and Columns
        ReflectionTestUtils.setField(controller, "playerTable", new TableView<>());
        ReflectionTestUtils.setField(controller, "playerNameColumn", new TableColumn<>());
        ReflectionTestUtils.setField(controller, "colorColumn", new TableColumn<>());
        ReflectionTestUtils.setField(controller, "adminColumn", new TableColumn<>());

        // Admin Settings and Buttons
        ReflectionTestUtils.setField(controller, "startGameButton", new Button());
        ReflectionTestUtils.setField(controller, "adminSettings", new VBox());
        ReflectionTestUtils.setField(controller, "maxPlayers", new Label());
        ReflectionTestUtils.setField(controller, "connectedPlayers", new Label());
        ReflectionTestUtils.setField(controller, "gameNameLabel", new Label());
        ReflectionTestUtils.setField(controller, "boardSizeCombo", new ComboBox<>());
        ReflectionTestUtils.setField(controller, "treasureCardsSpinner", new Spinner<>());

        // Timer and Bonus Settings
        ReflectionTestUtils.setField(controller, "timerEnabledCheck", new CheckBox());
        ReflectionTestUtils.setField(controller, "timerSettings", new HBox());
        ReflectionTestUtils.setField(controller, "gameTimerSpinner", new Spinner<>());
        ReflectionTestUtils.setField(controller, "bonusEnabledCheck", new CheckBox());
        ReflectionTestUtils.setField(controller, "bonusSetting", new HBox());
        ReflectionTestUtils.setField(controller, "bonusCountSpinner", new Spinner<>());

        // Results Table and Viewport Layout
        ReflectionTestUtils.setField(controller, "resultsTable", new TableView<>());
        ReflectionTestUtils.setField(controller, "resultsBox", new VBox());
        ReflectionTestUtils.setField(controller, "boardTheme", new ComboBox<>());
        ReflectionTestUtils.setField(controller, "leftBar", new VBox());
        ReflectionTestUtils.setField(controller, "rightBar", new VBox());
        ReflectionTestUtils.setField(controller, "viewport", new StackPane());
        ReflectionTestUtils.setField(controller, "root", new BorderPane());
        ReflectionTestUtils.setField(controller, "countdown", new Label());
        ReflectionTestUtils.setField(controller, "countDownBox", new HBox());

        // Ranking/Result Columns
        ReflectionTestUtils.setField(controller, "resultNameColumn", new TableColumn<>());
        ReflectionTestUtils.setField(controller, "resultScoreColumn", new TableColumn<>());
        ReflectionTestUtils.setField(controller, "achievements", new TableColumn<>());
    }
}
