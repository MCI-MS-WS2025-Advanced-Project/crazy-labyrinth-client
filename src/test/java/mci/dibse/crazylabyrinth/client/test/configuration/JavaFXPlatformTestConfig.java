package mci.dibse.crazylabyrinth.client.test.configuration;

import static org.mockito.Mockito.mock;

import javafx.application.Platform;
import mci.dibse.crazylabyrinth.client.view.viewmodel.GameServerDashboardViewModel;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 *  @author Julian Janetschek
 */
@TestConfiguration
public class JavaFXPlatformTestConfig {

    static {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
        }
    }

    // to avoid invoking modal on screen
    @Bean
    GameServerDashboardViewModel gameServerDashboardViewModel() {
        return mock(GameServerDashboardViewModel.class);
    }
}
