package mci.dibse.crazylabyrinth.client.view;

import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import mci.dibse.crazylabyrinth.client.view.controller.ViewController;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;


/**
 *  @author Julian Janetschek
 */
@Component
public class ViewLoader {

    private static final String VIEW_LOCATION = "/fxml/";

    private final ApplicationContext applicationContext;

    public ViewLoader(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public Parent loadFXML(String viewName) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(VIEW_LOCATION + viewName));
        loader.setControllerFactory(applicationContext::getBean);
        Parent root = loader.load();
        Object ctrl = loader.getController();
        if (ctrl instanceof ViewController) {
            ((ViewController) ctrl).setRoot(root);
        }
        return root;
    }
}
