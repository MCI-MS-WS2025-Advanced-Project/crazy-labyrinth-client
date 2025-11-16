package mci.dibse.crazylabyrinth.client.view.controller.impl;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.springframework.stereotype.Component;

@Component
public class HelloJavaFXController extends ViewControllerImpl {

    @FXML private VBox box;

    private Label field;

    @FXML
    private void onClick() {
        field.setText("Hello from FXML Controller");
    }

    @FXML
    private void initialize() {
        field = new Label();
        box.getChildren().add(field);
    }
}
