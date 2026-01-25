package mci.dibse.crazylabyrinth.client.view.controller.impl;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import mci.dibse.crazylabyrinth.client.view.viewmodel.HelloJavaFXViewModel;
import mci.dibse.crazylabyrinth.client.view.viewmodel.navigation.NavigationStateViewModel;
import org.springframework.stereotype.Component;

// TODO Remove + helloJavaFX.fxml & helloJavaFX2.fxml
@Component
@RequiredArgsConstructor
public class HelloJavaFXController extends ViewControllerImpl {

    private final HelloJavaFXViewModel viewModel;
    private final NavigationStateViewModel navState;

    @FXML private VBox box;
    @FXML private Label field;

    @FXML
    private void onClick() {
        viewModel.onButtonClicked();
    }

    @FXML
    private void initialize() {
        field.textProperty().bind(viewModel.getMessage());
    }

    @FXML
    public void onClick2(ActionEvent actionEvent) {
        navState.navigateTo(NavigationStateViewModel.ScreenId.LOBBY);
    }

    @FXML
    public void onClick3(ActionEvent actionEvent) {
        navState.navigateTo(NavigationStateViewModel.ScreenId.MAIN);
    }
}
