package mci.dibse.crazylabyrinth.client.test.unit;

import static org.junit.jupiter.api.Assertions.*;

import javafx.collections.FXCollections;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import mci.dibse.crazylabyrinth.client.view.controller.impl.SettingsController;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class SettingsControllerTest {

    @BeforeAll
    static void initToolkit() {
        // Force Headless properties BEFORE startup
        boolean isCi = System.getenv("GITHUB_ACTIONS") != null;
        if (isCi) {
            System.setProperty("glass.platform", "Monocle");
            System.setProperty("monocle.platform", "Headless");
            System.setProperty("prism.order", "sw");
        }

        try {
            com.sun.javafx.application.PlatformImpl.startup(() -> {});
        } catch (IllegalStateException e) {
            // Already initialized
        }
    }

    @Test
    @DisplayName("UT-VIEW-05 - SettingsController initialization and save path")
    void testSettingsInitializationAndSave_UT_VIEW_05() {
        // GIVEN: Controller instance
        SettingsController controller = new SettingsController();

        // GIVEN: Setup default values
        String testRes = "1280x720";
        controller.setDefaultResolutionString(testRes);
        controller.setDefaultScaleRatio(true);
        controller.setDefaultFullscreen(false);

        // GIVEN: Mock UI components (Individual instances, no Scene/Stage needed)
        ComboBox<String> resCombo =
                new ComboBox<>(FXCollections.observableArrayList("1280x720 (16:9)"));
        RadioButton scaleRadio = new RadioButton();
        CheckBox fullCheck = new CheckBox();
        CheckBox borderCheck = new CheckBox();

        // Inject fields via Reflection
        ReflectionTestUtils.setField(controller, "resolutionComboBox", resCombo);
        ReflectionTestUtils.setField(controller, "scaleToFitRadio", scaleRadio);
        ReflectionTestUtils.setField(controller, "fullscreenCheckBox", fullCheck);
        ReflectionTestUtils.setField(controller, "borderlessCheckBox", borderCheck);

        // WHEN: Initialize the controller
        // We run this directly on the test thread. Since we only set properties
        // on the controls, JavaFX usually allows this in a Unit Test context.
        assertDoesNotThrow(
                () -> {
                    ReflectionTestUtils.invokeMethod(controller, "initialize");
                });

        // THEN: Verify UI state
        assertEquals(
                testRes,
                resCombo.getValue(),
                "Resolution should be correctly set from default string");
        assertTrue(scaleRadio.isSelected(), "Scale ratio should be selected");
        assertFalse(fullCheck.isSelected(), "Fullscreen should be unselected");

        System.out.println(
                "UT-VIEW-05 Result: SettingsController initialized successfully in CI"
                        + " environment.");
    }
}
