package mci.dibse.crazylabyrinth.client;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import javafx.beans.value.ChangeListener;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCharacterCombination;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import mci.dibse.crazylabyrinth.client.infrastructure.util.JavaFXUtils;
import mci.dibse.crazylabyrinth.client.infrastructure.util.ScreenResolutionProperties;
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
@Slf4j
public class NavigationController {


    private boolean startUp = true;

    private static final String MAIN_VIEW = "gameServerDashboard.fxml";
    private static final String LOBBY_VIEW = "gameLobby.fxml";
    private static final String GAMEBOARD_VIEW = "gameBoard.fxml";


    private final NavigationStateViewModel navState;
    private final ViewLoader viewLoader;
    @Setter @Getter private Stage primaryStage;
    private ScreenResolutionProperties screenResolutionProperties;



    @Autowired
    public NavigationController(NavigationStateViewModel navState, ViewLoader viewLoader, ScreenResolutionProperties screenResolutionProperties) {
        this.navState = navState;
        this.viewLoader = viewLoader;
        this.screenResolutionProperties = screenResolutionProperties;
    }

    @PostConstruct
    public void init() {
        navState.getCurrentScreenProperty().addListener(navListener);


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
            setStageMinMaxSizes(newScreen);
            setUpStageAndScene(newScreen, mainRoot);
        } catch (IOException | IllegalArgumentException e) {
            throw new RuntimeException(e);
        }
    }

    private void setUpStageAndScene(NavigationStateViewModel.ScreenId newScreen, Parent mainRoot) {
        Scene scene = new Scene(mainRoot);
        setupFullScreen(scene);
        primaryStage.setFullScreenExitKeyCombination(new KeyCodeCombination(KeyCode.ESCAPE));
        primaryStage.setScene(scene);
        primaryStage.setTitle("The Amazing Labyrinth");
        primaryStage.show();
    }

    private String mapScreenIdToFile(NavigationStateViewModel.ScreenId newScreen) {
        return switch (newScreen) {
            case MAIN -> MAIN_VIEW;
            case LOBBY -> LOBBY_VIEW;
            case GAMEBOARD -> GAMEBOARD_VIEW;
            default -> throw new IllegalArgumentException("Unknown screen id: " + newScreen);
        };
    }

    private void setStageMinMaxSizes(NavigationStateViewModel.ScreenId newScreen) {
        primaryStage.setMinWidth(screenResolutionProperties.getMinViewWidth());
        primaryStage.setMinHeight(screenResolutionProperties.getMinViewHeight());

        if(!startUp) return;
        startUp = false;
        primaryStage.setWidth(screenResolutionProperties.getWidth());
        primaryStage.setHeight(screenResolutionProperties.getHeight());

    }

    private void setupFullScreen(Scene scene) {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE && primaryStage.isFullScreen()) {
                primaryStage.setFullScreen(false);
                event.consume(); // Prevent further processing
            }
        });
    }



}
