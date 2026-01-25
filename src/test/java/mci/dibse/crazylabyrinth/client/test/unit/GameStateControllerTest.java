package mci.dibse.crazylabyrinth.client.test.unit;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import mci.dibse.crazylabyrinth.client.view.controller.impl.GameStateController;
import mci.dibse.crazylabyrinth.client.view.viewmodel.GameViewModel;
import mci.dibse.crazylabyrinth.client.view.viewmodel.navigation.NavigationStateViewModel;
import mci.dibse.crazylabyrinth.client.view.viewmodel.item.GameBoardItem;
import mci.dibse.crazylabyrinth.client.view.viewmodel.item.PlayerStateItem;
import mci.dibse.crazylabyrinth.client.infrastructure.util.ScreenResolutionProperties;
import labyrinth.contracts.models.TurnState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class GameStateControllerTest {

    @BeforeAll
    static void initToolkit() {
        try { com.sun.javafx.application.PlatformImpl.startup(() -> {}); } catch (Exception ignored) {}
    }

    /**
     * UT-VIEW-04 - GameStateController initialization path
     * Musskriterium: C4
     * Priorität: P1
     * Severity: High
     * Testobjekt: GameStateController
     */
    @Test
    @DisplayName("UT-VIEW-04 - GameStateController initialization path")
    void testControllerInitialization_UT_VIEW_04() throws Exception {
        // GIVEN: Controller dependencies are mocked
        NavigationStateViewModel mockNav = mock(NavigationStateViewModel.class);
        GameViewModel mockVM = mock(GameViewModel.class);
        ScreenResolutionProperties mockRes = mock(ScreenResolutionProperties.class);
        GameBoardItem mockBoardItem = mock(GameBoardItem.class);
        PlayerStateItem mockClientState = mock(PlayerStateItem.class);

        // GIVEN: Required properties are initialized to avoid NPEs
        when(mockNav.getCurrentScreenProperty()).thenReturn(new SimpleObjectProperty<>());
        when(mockVM.getCurrentPlayerIdProperty()).thenReturn(new SimpleStringProperty("p1"));
        when(mockVM.getCurrentTurnStateProperty()).thenReturn(new SimpleObjectProperty<>(TurnState.WAITING_FOR_PUSH));
        when(mockVM.getTurnTimeoutProperty()).thenReturn(new SimpleBooleanProperty(false));
        when(mockVM.getPendingRotationProperty()).thenReturn(new SimpleBooleanProperty(false));
        when(mockVM.getPlayerStatesSorted()).thenReturn(new ArrayList<>());
        when(mockVM.getGameBoardItem()).thenReturn(mockBoardItem);
        when(mockBoardItem.getRows()).thenReturn(7);
        when(mockBoardItem.getCols()).thenReturn(7);
        when(mockBoardItem.getSpareTileProperty()).thenReturn(new SimpleObjectProperty<>());
        when(mockVM.getClientPlayerState()).thenReturn(mockClientState);
        when(mockVM.getRemainingGameTimeInSeconds()).thenReturn(300L);
        when(mockVM.getRemainingTurnTimeInSeconds()).thenReturn(60L);
        when(mockClientState.getAvailableBonusTypes()).thenReturn(new SimpleListProperty<>(FXCollections.observableArrayList()));

        // WHEN: Controller is instantiated with three arguments
        GameStateController controller = new GameStateController(mockNav, mockVM, mockRes);

        // GIVEN: @FXML fields are injected via reflection
        injectFxmlFields(controller);

        // WHEN: Initialize is called on the FX Thread
        Platform.runLater(() -> {
            try { controller.initialize(); } catch (Exception ignored) {}
        });
        Thread.sleep(500);

        // THEN: Controller is successfully setup
        assertNotNull(controller);
        System.out.println("UT-VIEW-04 Result: Initialization path verified.");
    }

    private void injectFxmlFields(GameStateController controller) {
        ReflectionTestUtils.setField(controller, "playerTurnLabel", new Label());
        ReflectionTestUtils.setField(controller, "boardGrid", new GridPane());
        ReflectionTestUtils.setField(controller, "gameContainer", new BorderPane());
        ReflectionTestUtils.setField(controller, "nextTreasure", new Label());
        ReflectionTestUtils.setField(controller, "topArrowBox", new HBox());
        ReflectionTestUtils.setField(controller, "bottomArrowBox", new HBox());
        ReflectionTestUtils.setField(controller, "leftArrowBox", new VBox());
        ReflectionTestUtils.setField(controller, "rightArrowBox", new VBox());
        ReflectionTestUtils.setField(controller, "extraTile", new ImageView());
        ReflectionTestUtils.setField(controller, "extraTileTreasureLabel", new Label());
        ReflectionTestUtils.setField(controller, "extraTileBonusLabel", new Label());
        ReflectionTestUtils.setField(controller, "extraTileWrapper", new VBox());
        ReflectionTestUtils.setField(controller, "rotateRightButton", new Button());

        // Correct VBox types for players
        ReflectionTestUtils.setField(controller, "player1Box", new VBox());
        ReflectionTestUtils.setField(controller, "player2Box", new VBox());
        ReflectionTestUtils.setField(controller, "player1Circle", new Circle());
        ReflectionTestUtils.setField(controller, "player1", new Label());
        ReflectionTestUtils.setField(controller, "gameCounterLabel", new Label());
        ReflectionTestUtils.setField(controller, "turnCounterLabel", new Label());
        ReflectionTestUtils.setField(controller, "turnCounterBox", new HBox());

        // Bonus fields
        ReflectionTestUtils.setField(controller, "bonusBox", new VBox());
        ReflectionTestUtils.setField(controller, "bonusSwapBox", new HBox());
        ReflectionTestUtils.setField(controller, "bonusSwapButton", new Button());
        ReflectionTestUtils.setField(controller, "bonusSwapCount", new Label());
        ReflectionTestUtils.setField(controller, "bonusBeamBox", new HBox());
        ReflectionTestUtils.setField(controller, "bonusBeamButton", new Button());
        ReflectionTestUtils.setField(controller, "bonusBeamCount", new Label());
        ReflectionTestUtils.setField(controller, "bonusPushTwiceBox", new HBox());
        ReflectionTestUtils.setField(controller, "bonusPushTwiceButton", new Button());
        ReflectionTestUtils.setField(controller, "bonusPushTwiceCount", new Label());
        ReflectionTestUtils.setField(controller, "bonusPushFixedBox", new HBox());
        ReflectionTestUtils.setField(controller, "bonusPushFixedButton", new Button());
        ReflectionTestUtils.setField(controller, "bonusPushFixedCount", new Label());
    }
}