package mci.dibse.crazylabyrinth.client;

import javafx.application.Application;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Locale;

/**
 * Entry point for the Crazy Labyrinth client application.
 * <p>
 * This class configures the default locale and launches the JavaFX
 * application. Although it is annotated with {@link org.springframework.boot.autoconfigure.SpringBootApplication},
 * the application start through JavaFX rather than the standard
 * Spring Boot launcher. Spring Boot remains available for component
 * scanning and dependency injection within the JavaFX context.
 * @author Julian Janetschek
 */
@SpringBootApplication
public class MainApplication {

    public static void main(String[] args) {
        setDefaultLocale();
        Application.launch(JavaFXClientApplication.class, args);
    }

    /**
     * Set the default locale for the application.
     */
    private static void setDefaultLocale() {
        Locale.setDefault(Locale.GERMAN);
    }
}
