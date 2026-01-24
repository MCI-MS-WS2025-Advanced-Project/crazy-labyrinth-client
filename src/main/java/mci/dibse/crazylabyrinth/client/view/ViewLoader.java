package mci.dibse.crazylabyrinth.client.view;

import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Component responsible for loading JavaFX FXML views and integrating them with the Spring application context
 * to enable dependency injection in controllers.
 *
 *
 */
@Component
public class ViewLoader {

    private static final String VIEW_LOCATION = "/fxml/";

    private final ApplicationContext applicationContext;

    @Autowired
    public ViewLoader(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * Load an FXML view with Spring-managed controllers.
     *
     * @param viewName FXML filename under the view location
     * @return loaded JavaFX parent node
     * @throws IOException when the FXML cannot be loaded
     */
    public Parent loadFXML(String viewName) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(VIEW_LOCATION + viewName));
        loader.setControllerFactory(applicationContext::getBean);
        return loader.load();
    }
}
