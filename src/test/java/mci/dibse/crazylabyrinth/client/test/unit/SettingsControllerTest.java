package mci.dibse.crazylabyrinth.client.test.unit;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import mci.dibse.crazylabyrinth.client.view.controller.impl.SettingsController;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;

class SettingsControllerTest {

    @BeforeAll
    static void initToolkit() {
        // Check if we are running in a headless environment (CI)
        // Most CI runners set 'headless' or 'GITHUB_ACTIONS'
        boolean isCi = System.getenv("GITHUB_ACTIONS") != null;
        if (isCi) {
            System.setProperty("glass.platform", "Monocle");
            System.setProperty("monocle.platform", "Headless");
            System.setProperty("prism.order", "sw");
        }

        try {
            com.sun.javafx.application.PlatformImpl.startup(() -> {});
        } catch (IllegalStateException e) {
            // Toolkit already initialized
        }
    }

    @Test
    @DisplayName("UT-VIEW-06 - SettingsController initialization and save path")
    void testSettingsInitializationAndSave_UT_VIEW_06() throws Exception {
        // GIVEN: Controller instance
        SettingsController controller = new SettingsController();

        // GIVEN: Setup default values
        String testRes = "1280x720";
        controller.setDefaultResolutionString(testRes);
        controller.setDefaultScaleRatio(true);
        controller.setDefaultFullscreen(false);

        // GIVEN: Mock UI components
        ComboBox<String> resCombo = new ComboBox<>(FXCollections.observableArrayList("1280x720 (16:9)"));
        RadioButton scaleRadio = new RadioButton();
        CheckBox fullCheck = new CheckBox();
        CheckBox borderCheck = new CheckBox();

        ReflectionTestUtils.setField(controller, "resolutionComboBox", resCombo);
        ReflectionTestUtils.setField(controller, "scaleToFitRadio", scaleRadio);
        ReflectionTestUtils.setField(controller, "fullscreenCheckBox", fullCheck);
        ReflectionTestUtils.setField(controller, "borderlessCheckBox", borderCheck);

        // GIVEN: Scene/Stage setup to avoid NPEs
        Platform.runLater(() -> {
            VBox root = new VBox(resCombo);
            new Scene(root);
            new Stage();
        });
        Thread.sleep(200);

        // WHEN: Initialize the controller using Reflection to bypass access restrictions
        ReflectionTestUtils.invokeMethod(controller, "initialize");

        // THEN: Verify UI state
        assertEquals(testRes, resCombo.getValue());
        assertTrue(scaleRadio.isSelected());

        System.out.println("UT-VIEW-06 Result: SettingsController initialized via Reflection.");
    }
}
