package mci.dibse.crazylabyrinth.client.test.configuration;

import javafx.application.Platform;
import org.springframework.boot.test.context.TestConfiguration;

@TestConfiguration
public class JavaFXPlatformTestConfig {

    static {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
        }
    }
}
