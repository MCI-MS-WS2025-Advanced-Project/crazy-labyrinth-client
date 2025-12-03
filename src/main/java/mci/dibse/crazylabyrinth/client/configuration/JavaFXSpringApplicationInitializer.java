package mci.dibse.crazylabyrinth.client.configuration;

import javafx.application.Application;
import mci.dibse.crazylabyrinth.client.JavaFXClientApplication;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;

/**
 * ApplicationContextInitializer that injects the running JavaFX application
 * into the Spring context.
 *
 * <p>This initializer is used when running the client in FULL Spring Boot mode.
 * It registers the JavaFX {@link Application} instance as a bean so that other
 * Spring-managed components can access it.
 */
public class JavaFXSpringApplicationInitializer
        implements ApplicationContextInitializer<GenericApplicationContext> {

    private final JavaFXClientApplication fxApplication;

    public JavaFXSpringApplicationInitializer(JavaFXClientApplication fxApplication) {
        this.fxApplication = fxApplication;
    }

    @Override
    public void initialize(GenericApplicationContext context) {
        context.registerBean(Application.class, () -> fxApplication);
    }
}
