package mci.dibse.crazylabyrinth.client.test.configuration;

import javafx.application.Application;
import mci.dibse.crazylabyrinth.client.JavaFXClientApplication;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BaseTestConfiguration {

    @Bean
    Application app() {
        return Mockito.mock(JavaFXClientApplication.class);
    }
}
