package mci.dibse.crazylabyrinth.client;

import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;
import mci.dibse.crazylabyrinth.client.view.controller.ViewController;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 *
 * Configuration class responsible for loading and displaying JavaFX views
 * through Spring-managed controllers.
 * <p>
 * This class bridges JavaFX's {@link FXMLLoader} with Spring's
 * {@link ApplicationContext}, allowing controllers to be retrieved as
 * Spring beans. It manages the primary stage.
 */
@Configuration
@Lazy
public class ViewConfiguration {

    private static final String VIEW_LOCATION = "/fxml/";
    private static final String MAIN_VIEW = "helloJavaFX.fxml";

    private final ApplicationContext applicationContext;
    @Setter @Getter private Stage primaryStage;

    public ViewConfiguration(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public void showMainScreen() {
        try {
            Parent mainRoot = loadFXML(MAIN_VIEW);
            Scene scene = new Scene(mainRoot);
            primaryStage.setScene(scene);
            primaryStage.setTitle("The Amazing Labyrinth");
            primaryStage.show();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Parent loadFXML(String viewName) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(VIEW_LOCATION + viewName));
        loader.setControllerFactory(applicationContext::getBean);
        Parent root = loader.load();
        Object ctrl = loader.getController();
        if (ctrl instanceof ViewController) {
            ((ViewController) ctrl).setRoot(root);
        }
        return root;
    }
}
