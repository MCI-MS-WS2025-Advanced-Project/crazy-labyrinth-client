package mci.dibse.crazylabyrinth.client.test.configuration;

import static mci.dibse.crazylabyrinth.client.configuration.SpringBootConfiguration.BASE_PACKAGE;

import java.io.IOException;
import java.lang.reflect.Method;
import javafx.application.Application;
import mci.dibse.crazylabyrinth.client.JavaFXClientApplication;
import mci.dibse.crazylabyrinth.client.configuration.SpringBootConfiguration;
import org.mockito.Mockito;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.util.ReflectionUtils;
/**
 *  @author Julian Janetschek
 */
@TestConfiguration
@ComponentScan(BASE_PACKAGE)
public class MinimalTestConfig {

    private static final String APPLICATION_PROPERTIES = "application.properties";
    private static final String APPLICATION_DEV_PROPERTIES = "application-dev.properties";

    @Bean
    static BeanFactoryPostProcessor envInitializer() {
        return bf -> {
            ConfigurableEnvironment env = bf.getBean(ConfigurableEnvironment.class);

            try {
                var sources = env.getPropertySources();
                sources.addLast(new ResourcePropertySource("classpath:" + APPLICATION_PROPERTIES));
                sources.addLast(
                        new ResourcePropertySource("classpath:" + APPLICATION_DEV_PROPERTIES));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
    }

    @Bean
    Application app() {
        return Mockito.mock(JavaFXClientApplication.class);
    }

    @Bean
    static PropertySourcesPlaceholderConfigurer props() {
        Method m =
                ReflectionUtils.findMethod(
                        SpringBootConfiguration.class, "setupResourcesForMinimalSetup");
        assert m != null;
        ReflectionUtils.makeAccessible(m);
        return (PropertySourcesPlaceholderConfigurer) ReflectionUtils.invokeMethod(m, null);
    }
}
