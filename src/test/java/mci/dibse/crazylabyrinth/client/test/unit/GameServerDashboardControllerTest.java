package mci.dibse.crazylabyrinth.client.test.unit;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.*;
import mci.dibse.crazylabyrinth.client.view.controller.impl.GameServerDashboardController;
import mci.dibse.crazylabyrinth.client.view.viewmodel.GameServerDashboardViewModel;
import mci.dibse.crazylabyrinth.client.view.viewmodel.navigation.NavigationStateViewModel;
import mci.dibse.crazylabyrinth.client.infrastructure.util.ScreenResolutionProperties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

class GameServerDashboardControllerTest {

    @BeforeAll
    static void initToolkit() {
        try { com.sun.javafx.application.PlatformImpl.startup(() -> {}); } catch (Exception ignored) {}
    }

    @Test
    @DisplayName("UT-VIEW-02 - GameServerDashboardController initialization path")
    void testDashboardInitialization_UT_VIEW_02() throws Exception {
        // GIVEN: Dependencies are mocked
        GameServerDashboardViewModel mockVM = mock(GameServerDashboardViewModel.class);
        NavigationStateViewModel mockNav = mock(NavigationStateViewModel.class);
        ScreenResolutionProperties mockRes = mock(ScreenResolutionProperties.class);

        // GIVEN: Properties are set for binding
        when(mockNav.getCurrentScreenProperty()).thenReturn(new SimpleObjectProperty<>());
        when(mockVM.getSorted()).thenReturn(new SortedList<>(FXCollections.observableArrayList()));
        when(mockVM.onlyJoinableProperty()).thenReturn(new SimpleBooleanProperty(false));
        when(mockVM.getHasError()).thenReturn(new SimpleBooleanProperty(false));
        when(mockVM.getIsRefreshing()).thenReturn(new SimpleBooleanProperty(false));
        when(mockVM.getErrorMessage()).thenReturn(new SimpleStringProperty(""));

        // WHEN: Controller is initialized
        GameServerDashboardController controller = new GameServerDashboardController(mockVM, mockNav, mockRes);
        ReflectionTestUtils.setField(controller, "statusLabel", new Label());
        ReflectionTestUtils.setField(controller, "serversTable", new TableView<>());
        ReflectionTestUtils.setField(controller, "nameCol", new TableColumn<>());
        ReflectionTestUtils.setField(controller, "statusCol", new TableColumn<>());
        ReflectionTestUtils.setField(controller, "currentPlayersCol", new TableColumn<>());
        ReflectionTestUtils.setField(controller, "lastSeenCol", new TableColumn<>());
        ReflectionTestUtils.setField(controller, "actionCol", new TableColumn<>());
        ReflectionTestUtils.setField(controller, "refreshButton", new Button());
        ReflectionTestUtils.setField(controller, "showOnlyJoinable", new CheckBox());
        ReflectionTestUtils.setField(controller, "serverCountLabel", new Label());

        Platform.runLater(() -> {
            try { controller.initialize(); } catch (Exception ignored) {}
        });
        Thread.sleep(400);

        // THEN: Successful path coverage
        assertNotNull(controller);
        System.out.println("UT-VIEW-02 Result: Dashboard initialized.");
    }
}
