package mci.dibse.crazylabyrinth.client.test.configuration;

import javafx.application.Platform;
import mci.dibse.crazylabyrinth.client.view.viewmodel.GameServerDashboardViewModel;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import static org.mockito.Mockito.mock;

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


    //to avoid invoking modal on screen
    @Bean
    GameServerDashboardViewModel gameServerDashboardViewModel(){
        return mock(GameServerDashboardViewModel.class);
    }
}
