package mci.dibse.crazylabyrinth.client.test.integration;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import mci.dibse.crazylabyrinth.client.test.configuration.JavaFXPlatformTestConfig;

import mci.dibse.crazylabyrinth.client.test.configuration.MinimalTestConfig;
import mci.dibse.crazylabyrinth.client.view.controller.impl.GameServerDashboardController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.*;

/**
 *  @author Julian Janetschek
 */
@SpringJUnitConfig({MinimalTestConfig.class, JavaFXPlatformTestConfig.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class JavaFXPlatformIT {

    @Autowired ApplicationContext applicationContext;

    //TODO refactor or remove
    @Test
    public void testHelloJavaFX() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/gameServerDashboard.fxml"));
        loader.setControllerFactory(applicationContext::getBean);
        Parent root = loader.load();
        GameServerDashboardController controller = loader.getController();
        assertNotNull(controller);
    }


    @Test
    void javafxToolkitIsInitialized_runLaterExecutesOnFxThread() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] onFxThread = {false};

        Platform.runLater(() -> {
            onFxThread[0] = Platform.isFxApplicationThread();
            latch.countDown();
        });

        assertTrue(latch.await(2, TimeUnit.SECONDS), "runLater did not execute in time");
        assertTrue(onFxThread[0], "Task did not run on FX Application Thread");
    }

    @Test
    void subsequentStartupThrows() {
        // JavaFX can only be started once per JVM; calling again throws IllegalStateException
        assertThrows(IllegalStateException.class, () -> Platform.startup(() -> {}));
    }
}
