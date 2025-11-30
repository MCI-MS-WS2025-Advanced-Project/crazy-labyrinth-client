package mci.dibse.crazylabyrinth.client;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import javafx.beans.value.ChangeListener;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;
import mci.dibse.crazylabyrinth.client.infrastructure.util.JavaFXUtils;
import mci.dibse.crazylabyrinth.client.view.ViewLoader;
import mci.dibse.crazylabyrinth.client.view.viewmodel.navigation.NavigationStateViewModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Controls the primary navigation logic for the client application.
 * <p>
 * This controller observes the {@link NavigationStateViewModel} to switch between different
 * application screens (e.g. Dashboard, Lobby, Gameboard) by loading the appropriate
 * FXML views onto the primary {@link Stage}.
 * </p>
 */

@Component
public class NavigationController {

    private static final String MAIN_VIEW           = "gameServerDashboard.fxml";
    private static final String LOBBY_VIEW          = "";
    private static final String GAMEBOARD_VIEW      = "";
    private static final String RESULTS_VIEW        = "";

    private NavigationStateViewModel navState;
    private ViewLoader viewLoader;
    @Setter @Getter private Stage primaryStage;

    @Autowired
    public NavigationController(NavigationStateViewModel navState, ViewLoader viewLoader) {
        this.navState = navState;
        this.viewLoader = viewLoader;
    }


    @PostConstruct
    public void init() {
        navState.getCurrentScreen().addListener(navListener);
    }

    private final ChangeListener<NavigationStateViewModel.ScreenId> navListener =
            (obs, oldView, newView) -> {
                if (newView != null) {
                    JavaFXUtils.runOnFxThread(() -> showScreen(newView));
                }
            };

    public void showMainScreen() {
        showScreen(NavigationStateViewModel.ScreenId.MAIN);
    }


    private void showScreen(NavigationStateViewModel.ScreenId newScreen) {
        String viewFile = mapScreenIdToFile(newScreen);
        try {
            Parent mainRoot = viewLoader.loadFXML(viewFile);
            setStageMinMaxByScreenId(newScreen);
            setUpStageAndScene(mainRoot);
        } catch (IOException | IllegalArgumentException e) {
            throw new RuntimeException(e);
        }
    }

    private void setUpStageAndScene(Parent mainRoot) {
        Scene scene = new Scene(mainRoot);
        primaryStage.setScene(scene);
        primaryStage.setTitle("The Amazing Labyrinth");
        primaryStage.show();
    }

    private String mapScreenIdToFile(NavigationStateViewModel.ScreenId newScreen) {
        return switch (newScreen) {
            case MAIN -> MAIN_VIEW;
            case LOBBY -> LOBBY_VIEW;
            case GAMEBOARD -> GAMEBOARD_VIEW;
            case RESULTS -> RESULTS_VIEW;
            default -> throw new IllegalArgumentException("Unknown screen id: " + newScreen);
        };
    }

    private void setStageMinMaxByScreenId(NavigationStateViewModel.ScreenId newScreen) {
        switch (newScreen) {
            case MAIN -> setStageMinMaxSizes(712, 400);
            case LOBBY -> setStageMinMaxSizes(800, 450);
            case GAMEBOARD -> setStageMinMaxSizes(1024, 768);
        }
    }

    private void setStageMinMaxSizes(int width, int height) {
        primaryStage.setMinWidth(width);
        primaryStage.setMinHeight(height);
    }
}
