package mci.dibse.crazylabyrinth.client;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;
import mci.dibse.crazylabyrinth.client.domain.model.GameServer;
import mci.dibse.crazylabyrinth.client.view.ViewLoader;
import mci.dibse.crazylabyrinth.client.view.viewmodel.item.GameServerItem;
import mci.dibse.crazylabyrinth.client.view.viewmodel.navigation.NavigationStateViewModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
/**
 *  @author Julian Janetschek
 */
@Component
public class NavigationController {

    private static final String MAIN_VIEW = "gameServerDashboard.fxml";
    private static final String LOBBY_VIEW = "gameLobby.fxml";
    private static final String GAMEBOARD_VIEW = "gameView.fxml";
    private static final String RESULTS_VIEW = "";

    @Autowired private NavigationStateViewModel navState;
    @Autowired private ViewLoader viewLoader;
    @Setter @Getter private Stage primaryStage;

    @PostConstruct
    public void init() {
        navState.getCurrentScreen().addListener(navListener);
    }



    private final ChangeListener<NavigationStateViewModel.ScreenId> navListener =
            (obs, oldView, newView) -> {
                if (newView != null) {
                    Runnable task = () -> showScreen(newView);
                    if (javafx.application.Platform.isFxApplicationThread()) task.run();
                    else javafx.application.Platform.runLater(task);
                }
            };

    public void showMainScreen() {
        try {
            Parent mainRoot = viewLoader.loadFXML(MAIN_VIEW);
            Scene scene = new Scene(mainRoot);
            primaryStage.setScene(scene);
            primaryStage.setTitle("The Amazing Labyrinth");
            primaryStage.show();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void showScreen(NavigationStateViewModel.ScreenId newScreen) {
        String viewFile = mapScreenIdToFile(newScreen);
        try {
            Parent mainRoot = viewLoader.loadFXML(viewFile);
            setUpStageAndScene(mainRoot);
        } catch (IOException e) {
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
            default -> MAIN_VIEW;
        };
    }
}
