package mci.dibse.crazylabyrinth.client.test.integration;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import javafx.application.Application;
import mci.dibse.crazylabyrinth.client.JavaFXClientApplication;
import mci.dibse.crazylabyrinth.client.configuration.SpringBootConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.List;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class SpringBootConfigurationAndInitIT {


    private final Application javaFXClientApplication = mock(JavaFXClientApplication.class);


    @DisplayName( "Full Spring boot setup should not throw an exception")
    @Test
    public void full_spring_setup() {
        System.setProperty("app.spring.mode", "full");
        doReturn(new Application.Parameters() {

                     @Override
                     public List<String> getRaw() {
                         return List.of();
                     }
                     @Override
                     public List<String> getUnnamed() {
                         return List.of();
                     }
                     @Override
                     public Map<String, String> getNamed() {
                         return Map.of();
                     }
                 }
        ).when(javaFXClientApplication).getParameters();
        try (ConfigurableApplicationContext ctx =
                     SpringBootConfiguration.initSpringBasedOnProperties((JavaFXClientApplication) javaFXClientApplication)) {


            assertNotNull(ctx.getEnvironment().getProperty("spring.application.name"));
            assertEquals("full", ctx.getEnvironment().getProperty("app.spring.mode"));
        } catch (Exception e) {
            fail("Exception thrown: " + e.getMessage());
        } finally {
            System.clearProperty("app.spring.mode");
        }
    }

    @DisplayName( "Minimal Spring container setup should not throw an exception")
    @Test
    public void minimal_spring_setup() {
        System.setProperty("app.spring.mode", "minimal");
        try (ConfigurableApplicationContext ctx =
                     SpringBootConfiguration.initSpringBasedOnProperties((JavaFXClientApplication) javaFXClientApplication)) {
            assertNotNull(ctx.getEnvironment().getProperty("spring.application.name"));
            assertEquals("minimal", ctx.getEnvironment().getProperty("app.spring.mode"));
        } catch (Exception e) {
            fail("Exception thrown: " + e.getMessage());
        } finally {
            System.clearProperty("app.spring.mode");
        }
    }
}
