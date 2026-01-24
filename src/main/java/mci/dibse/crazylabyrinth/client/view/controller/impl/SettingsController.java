package mci.dibse.crazylabyrinth.client.view.controller.impl;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import mci.dibse.crazylabyrinth.client.domain.context.ApplicationSettingsRecord;
import org.springframework.stereotype.Component;

@Getter // Use Lombok to create getters for the result fields
@Slf4j
@Component
public class SettingsController {

    private String defaultViewResolutionString;
    @Setter
    private boolean defaultScaleRatio;
    @Setter
    private boolean defaultFullscreen ;

    @FXML private ComboBox<String> resolutionComboBox;
    @FXML private RadioButton scaleToFitRadio;
    @FXML private CheckBox fullscreenCheckBox;

    // Fields to hold the results
    private boolean saved = false;
    private String selectedResolution;
    private boolean scaleRatio;
    private boolean fullscreen;


    public ApplicationSettingsRecord getSettings() {
        return new ApplicationSettingsRecord(selectedResolution, scaleRatio, fullscreen);
    }

    @FXML
    void initialize() {
        System.out.println("SettingsController initialized " + defaultViewResolutionString);
        resolutionComboBox.setValue(defaultViewResolutionString);
        scaleToFitRadio.setSelected(defaultScaleRatio);
        fullscreenCheckBox.setSelected(defaultFullscreen);
    }

    @FXML
    private void onSave() {
        // 1. Capture the values
        this.selectedResolution = resolutionComboBox.getValue().split(" ")[0];
        this.scaleRatio = scaleToFitRadio.isSelected();
        this.fullscreen = fullscreenCheckBox.isSelected();
        this.saved = true;

        // 2. Close the window
        closeWindow();
    }

    @FXML
    private void onCancel() {
        this.saved = false;
        closeWindow();
        Platform.exit();
        System.exit(0);
    }

    private void closeWindow() {
        // Get the stage from any UI element and close it
        Stage stage = (Stage) resolutionComboBox.getScene().getWindow();
        stage.close();
    }


    public void setDefaultResolutionString(String defaultViewResolutionString) {
        this.defaultViewResolutionString = defaultViewResolutionString;
    }


}