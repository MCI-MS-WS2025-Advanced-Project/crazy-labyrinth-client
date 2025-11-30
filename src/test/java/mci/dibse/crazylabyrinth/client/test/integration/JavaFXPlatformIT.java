package mci.dibse.crazylabyrinth.client.test.integration;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import mci.dibse.crazylabyrinth.client.test.configuration.JavaFXPlatformTestConfig;
import mci.dibse.crazylabyrinth.client.test.configuration.MinimalTestConfig;
import mci.dibse.crazylabyrinth.client.view.controller.impl.HelloJavaFXController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
/**
 *  @author Julian Janetschek
 */
@SpringJUnitConfig({MinimalTestConfig.class, JavaFXPlatformTestConfig.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class JavaFXPlatformIT {

    @Autowired ApplicationContext applicationContext;

    @Test
    public void testHelloJavaFX() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/helloJavaFX.fxml"));
        loader.setControllerFactory(applicationContext::getBean);
        Parent root = loader.load();
        HelloJavaFXController controller = loader.getController();
        assertNotNull(controller);
    }
}
