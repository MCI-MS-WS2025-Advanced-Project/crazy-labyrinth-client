package mci.dibse.crazylabyrinth.client.configuration;

import java.io.IOException;
import java.util.Properties;
import javafx.application.Application;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import mci.dibse.crazylabyrinth.client.JavaFXClientApplication;
import mci.dibse.crazylabyrinth.client.MainApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.ResourcePropertySource;

/**
 * Central configuration class for initializing Spring within the JavaFX client.
 *<p>
 * This class decides whether the application starts with a full Spring Boot
 * context or a lightweight {@link org.springframework.context.support.GenericApplicationContext}
 * based on the property {@code app.spring.mode} defined in
 * {@code application.properties}. The hybrid design allows the client to run
 * without the full Boot stack when only a minimal container is required while
 * still supporting complete Boot features are needed.
 */
@Slf4j
public class SpringBootConfiguration {

    public static final String BASE_PACKAGE = "mci.dibse.crazylabyrinth.client";

    private static final String APPLICATION_PROPERTIES = "application.properties";
    private static final String APPLICATION_DEV_PROPERTIES = "application-dev.properties";
    private static final String APPLICATION_PROPERTIES_RESOURCE_PATH = "/application.properties";
    private static final String MODE_DEFAULT = "minimal";
    private static final String MODE_PROPERTY_NAME = "app.spring.mode";

    public static ConfigurableApplicationContext initSpringBasedOnProperties(
            JavaFXClientApplication app) {
        try {
            return checkSpringModeInProperties(app);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static ConfigurableApplicationContext checkSpringModeInProperties(
            JavaFXClientApplication app) throws IOException {

        String mode = loadApplicationProperty();
        System.out.println("DEBUG MODE: " + mode);
        if (SpringBootConfiguration.Mode.FULL.getValue().equalsIgnoreCase(mode)) {
          log.debug("init via full Spring Boot setup");
            return fullSpringBootSetup(app);
        } else if (SpringBootConfiguration.Mode.MINIMAL.getValue().equalsIgnoreCase(mode)) {
            log.debug("init via minimal Spring  setup");
            return minimalSpringContainerSetup(app);
        } else {
            throw new RuntimeException("Invalid mode: " + mode);
        }
    }

    private static String loadApplicationProperty() {
        String v = System.getProperty(MODE_PROPERTY_NAME);
        if (v != null) return v;

        Properties props = new Properties();
        try (var is =
                SpringBootConfiguration.class.getResourceAsStream(
                        APPLICATION_PROPERTIES_RESOURCE_PATH)) {
            if (is != null) props.load(is);
        } catch (IOException e) {
            throw new RuntimeException("Configuration load failed", e);
        }
        return props.getProperty(MODE_PROPERTY_NAME, MODE_DEFAULT);
    }

    private static ConfigurableApplicationContext minimalSpringContainerSetup(
            JavaFXClientApplication app) throws IOException {
        log.info("Minimal Spring Container setup");
        GenericApplicationContext ctx = new GenericApplicationContext();
        AnnotationConfigUtils.registerAnnotationConfigProcessors(ctx);
        setupEnvironmentForMinimalSetup(ctx);
        ctx.registerBean(Application.class, () -> app);
        ctx.registerBean(
                PropertySourcesPlaceholderConfigurer.class,
                SpringBootConfiguration::setupResourcesForMinimalSetup);
        new ClassPathBeanDefinitionScanner(ctx).scan(BASE_PACKAGE);
        ctx.refresh();
        ctx.registerShutdownHook();
        return ctx;
    }

    private static ConfigurableApplicationContext fullSpringBootSetup(JavaFXClientApplication app) {
        log.info("Full Spring Boot setup");
        return new SpringApplicationBuilder()
                .sources(MainApplication.class)
                .initializers(new JavaFXSpringApplicationInitializer(app))
                .run(app.getParameters().getRaw().toArray(new String[0]));
    }

    private static void setupEnvironmentForMinimalSetup(ConfigurableApplicationContext ctx)
            throws IOException {
        var env = ctx.getEnvironment();
        var sources = env.getPropertySources();
        sources.addLast(new ResourcePropertySource("classpath:" + APPLICATION_PROPERTIES));
        sources.addLast(new ResourcePropertySource("classpath:" + APPLICATION_DEV_PROPERTIES));
    }

    private static PropertySourcesPlaceholderConfigurer setupResourcesForMinimalSetup() {
        var cfg = new PropertySourcesPlaceholderConfigurer();
        cfg.setLocations(
                new ClassPathResource(APPLICATION_PROPERTIES),
                new ClassPathResource(APPLICATION_DEV_PROPERTIES));
        return cfg;
    }

    @Getter
    private enum Mode {
        FULL("full"),
        MINIMAL("minimal");

        private final String value;

        Mode(String value) {
            this.value = value;
        }
    }
}
