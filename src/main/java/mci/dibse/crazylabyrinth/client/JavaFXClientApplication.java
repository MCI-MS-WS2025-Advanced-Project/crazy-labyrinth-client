package mci.dibse.crazylabyrinth.client;

import static mci.dibse.crazylabyrinth.client.configuration.SpringBootConfiguration.initSpringBasedOnProperties;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * JavaFX entry point responsible for integrating the JavaFX lifecycle
 * with a Spring-managed application context.
 * <p>
 * During initialization, this class bootstraps the Spring container using
 * a configuration strategy determined by application properties. The chosen
 * Spring context is then used to obtain UI-related beans and launch the
 * primary view.
 * <p>
 * When the application shuts down, the Spring context is closed cleanly and
 * the JavaFX platform is terminated.
 * @author Julian Janetschek
 */
@Slf4j
public class JavaFXClientApplication extends Application {

    private ConfigurableApplicationContext applicationContext;

    @Override
    public void init() {
        log.debug("Initializing JavaFX application...");

    }

    @Override
    public void stop() {
        log.debug("Stopping JavaFX application...");
        applicationContext.close();
        Platform.exit();
    }

    @Override
    public void start(Stage stage) {
        log.debug("Init spring ...");
        applicationContext = initSpringBasedOnProperties(JavaFXClientApplication.this);
        log.debug("Starting JavaFX application...");
        NavigationController navigationController =
                applicationContext.getBean(NavigationController.class);
        navigationController.setPrimaryStage(stage);
        navigationController.showMainScreen();
    }
}
