package mci.dibse.crazylabyrinth.client.view.controller.impl;

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

@Getter
@Slf4j
@Component
public class SettingsController {

    private String defaultViewResolutionString;
    @Setter private boolean defaultScaleRatio;
    @Setter private boolean defaultFullscreen;

    @FXML private ComboBox<String> resolutionComboBox;
    @FXML private RadioButton scaleToFitRadio;
    @FXML private CheckBox fullscreenCheckBox;
    @FXML private CheckBox borderlessCheckBox;

    private boolean saved = false;
    private String selectedResolution;
    private boolean scaleRatio;
    private boolean fullscreen;
    private boolean borderless;

    public ApplicationSettingsRecord getSettings() {
        return new ApplicationSettingsRecord(
                selectedResolution, scaleRatio, fullscreen, borderless);
    }

    @FXML
    void initialize() {
        log.info("SettingsController initialized with default {}" + defaultViewResolutionString);
        resolutionComboBox.setValue(defaultViewResolutionString);
        scaleToFitRadio.setSelected(defaultScaleRatio);
        fullscreenCheckBox.setSelected(defaultFullscreen);
        borderlessCheckBox.setSelected(false);
    }

    @FXML
    private void onSave() {
        this.selectedResolution = resolutionComboBox.getValue().split(" ")[0];
        this.scaleRatio = scaleToFitRadio.isSelected();
        this.fullscreen = fullscreenCheckBox.isSelected();
        this.borderless = borderlessCheckBox.isSelected();
        this.saved = true;

        closeWindow();
    }

    @FXML
    private void onCancel() {
        this.saved = false;
        System.exit(0);
    }

    private void closeWindow() {
        Stage stage = (Stage) resolutionComboBox.getScene().getWindow();
        stage.close();
    }

    public void setDefaultResolutionString(String defaultViewResolutionString) {
        this.defaultViewResolutionString = defaultViewResolutionString;
    }
}
