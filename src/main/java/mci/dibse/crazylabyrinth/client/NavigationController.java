package mci.dibse.crazylabyrinth.client;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyEvent;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import mci.dibse.crazylabyrinth.client.domain.context.ApplicationSettingsRecord;
import mci.dibse.crazylabyrinth.client.infrastructure.util.JavaFXUtils;
import mci.dibse.crazylabyrinth.client.infrastructure.util.ScreenResolutionProperties;
import mci.dibse.crazylabyrinth.client.view.ViewLoader;
import mci.dibse.crazylabyrinth.client.view.controller.impl.SettingsController;
import mci.dibse.crazylabyrinth.client.view.viewmodel.navigation.NavigationStateViewModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Value(
            "${default.game.board.view.resolution:#{T(mci.dibse.crazylabyrinth.client.domain.context.ApplicationSettingsRecord).DEFAULT_RESOLUTION}}")
    private String defaultViewResolution;

    @Value(
            "${default.app.scale.ratio:#{T(mci.dibse.crazylabyrinth.client.domain.context.ApplicationSettingsRecord).DEFAULT_SCALE_RATIO}}")
    private boolean defaultScaleRatio;

    @Value(
            "${default.app.fullscreen:#{T(mci.dibse.crazylabyrinth.client.domain.context.ApplicationSettingsRecord).DEFAULT_FULLSCREEN}}")
    private boolean defaultFullscreen;

    private final NavigationStateViewModel navState;
    private final ViewLoader viewLoader;
    @Setter @Getter private Stage primaryStage;
    private ScreenResolutionProperties screenResolutionProperties;

    @Autowired
    public NavigationController(
            NavigationStateViewModel navState,
            ViewLoader viewLoader,
            ScreenResolutionProperties screenResolutionProperties) {
        this.navState = navState;
        this.viewLoader = viewLoader;
        this.screenResolutionProperties = screenResolutionProperties;
    }

    /**
     * Register the navigation listener after bean construction.
     */
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

    /**
     * Show the main screen and initialize stage sizing.
     */
    public void showMainScreen() {
        log.debug("Showing main screen...");
        ApplicationSettingsRecord settings = null;
        try {
            showSettingsScreenAndSetValues();
        } catch (Exception e) {
            log.error("Error while showing settings screen", e);
        }
        screenResolutionProperties.setPrimaryStage(primaryStage);
        setStageMinMaxSizes();
        showScreen(NavigationStateViewModel.ScreenId.MAIN);
    }

    private ApplicationSettingsRecord showSettingsScreenAndSetValues() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/settings.fxml"));

        SettingsController controller = new SettingsController();
        controller.setDefaultResolutionString(defaultViewResolution);
        controller.setDefaultScaleRatio(defaultScaleRatio);
        controller.setDefaultFullscreen(defaultFullscreen);

        loader.setController(controller);
        Parent settingsRoot = loader.load();

        Stage settingsStage = new Stage();
        Scene scene = new Scene(settingsRoot);

        settingsStage.setTitle("The Amazing Labyrinth - Launcher");
        settingsStage.initModality(Modality.APPLICATION_MODAL);
        settingsStage.setScene(scene);

        settingsStage.initStyle(StageStyle.UNDECORATED);
        settingsStage.showAndWait();

        ApplicationSettingsRecord settings = controller.getSettings();
        screenResolutionProperties.setScaleRatio(settings.scaleRatio());
        screenResolutionProperties.setGameBoardResolution(settings.selectedResolution());
        screenResolutionProperties.setBorderless(settings.borderless());
        screenResolutionProperties.init();

        primaryStage.setFullScreen(settings.fullscreen());

        System.out.println("borderless: " + settings.borderless());

        if (settings.borderless()) {
            System.out.println("UNDECORATED: ");
            primaryStage.initStyle(StageStyle.UNDECORATED);
        }
        return settings;
    }

    public void toogleFullScreen() {
        if (primaryStage != null) {
            primaryStage.setFullScreen(!primaryStage.isFullScreen());
        }
    }

    /**
     * Load and show a screen by its screen id.
     *
     * @param newScreen target screen id
     */
    private void showScreen(NavigationStateViewModel.ScreenId newScreen) {
        log.debug("Showing main screen... {}", newScreen.name());
        String viewFile = mapScreenIdToFile(newScreen);
        try {
            Parent mainRoot = viewLoader.loadFXML(viewFile);
            setUpStageAndScene(newScreen, mainRoot);
        } catch (IOException | IllegalArgumentException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Configure the stage with a new scene while preserving window state.
     *
     * @param newScreen target screen id
     * @param mainRoot loaded root node
     */
    private void setUpStageAndScene(NavigationStateViewModel.ScreenId newScreen, Parent mainRoot) {
        boolean ismaximized = primaryStage.isMaximized();
        boolean isFullScreen = primaryStage.isFullScreen();

        double prevWidth = primaryStage.getWidth();
        double prevHeight = primaryStage.getHeight();

        Scene scene = new Scene(mainRoot);
        // setupFullScreen(scene);
        primaryStage.setFullScreenExitKeyCombination(new KeyCodeCombination(KeyCode.ESCAPE));
        primaryStage.setFullScreenExitHint("");
        primaryStage.setScene(scene);
        primaryStage.setTitle("The Amazing Labyrinth");
        primaryStage.setFullScreen(isFullScreen);
        primaryStage.setMaximized(ismaximized);

        primaryStage.setWidth(prevWidth);
        primaryStage.setHeight(prevHeight);
        primaryStage.show();
    }

    /**
     * Map a screen id to an FXML file name.
     *
     * @param newScreen screen id
     * @return FXML file name
     */
    private String mapScreenIdToFile(NavigationStateViewModel.ScreenId newScreen) {
        return switch (newScreen) {
            case MAIN -> MAIN_VIEW;
            case LOBBY -> LOBBY_VIEW;
            case GAMEBOARD -> GAMEBOARD_VIEW;
            default -> throw new IllegalArgumentException("Unknown screen id: " + newScreen);
        };
    }

    /**
     * Set minimum and initial stage sizes.
     */
    private void setStageMinMaxSizes() {
        primaryStage.setMinWidth(screenResolutionProperties.getMinViewWidth());
        primaryStage.setMinHeight(screenResolutionProperties.getMinViewHeight());

        if (!startUp) return;
        Rectangle2D bounds = Screen.getPrimary().getBounds();         // full monitor
        Rectangle2D visual = Screen.getPrimary().getVisualBounds();   // minus taskbar/dock

        boolean osBarReservesSpace =
                bounds.getWidth()  != visual.getWidth() ||
                        bounds.getHeight() != visual.getHeight();

        if (osBarReservesSpace && (visual.getWidth() < screenResolutionProperties.getWidth() || visual.getHeight() < screenResolutionProperties.getHeight())) {
            log.info("available screen is smaller than set Window site, setting stage to maximized");
            primaryStage.setMaximized(true);
        }
        startUp = false;
        log.debug(
                "Setting stage w/h sizes to: {}x{}",
                screenResolutionProperties.getWidth(),
                screenResolutionProperties.getHeight());
        primaryStage.setWidth(screenResolutionProperties.getWidth());
        primaryStage.setHeight(screenResolutionProperties.getHeight());
    }

    /**
     * Configure fullscreen behavior for the scene.
     *
     * @param scene scene to configure
     */
    private void setupFullScreen(Scene scene) {
        scene.addEventFilter(
                KeyEvent.KEY_PRESSED,
                event -> {
                    if (event.getCode() == KeyCode.ESCAPE && primaryStage.isFullScreen()) {
                        primaryStage.setFullScreen(false);
                        event.consume(); // Prevent further processing
                    }
                });
    }
}
