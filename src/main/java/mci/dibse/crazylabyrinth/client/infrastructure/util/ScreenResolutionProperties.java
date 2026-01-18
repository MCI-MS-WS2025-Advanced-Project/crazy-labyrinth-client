package mci.dibse.crazylabyrinth.client.infrastructure.util;

import jakarta.annotation.PostConstruct;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.geometry.Pos;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Component
@Slf4j
public class ScreenResolutionProperties {

    // stage size 1600X1200: viewport BoundingBox width:1600.0, height:1172.0,
    public static final double HEIGHT_OFFSET_TO_VIEWPORT_HEIGHT_ON_SCREEN = 28;

    private static final Pattern p = Pattern.compile("(\\d+)\\s*[xX×*]\\s*(\\d+)");

    @Value("${game.board.view.resolution:1024x768}")
    private String gameBoardResolution;
    @Getter private int width = 1024;
    @Getter private int height = 768;

    @Getter private final int minViewWidth = 1024;
    @Getter private final int minViewHeight = 768;


    @PostConstruct
    public void init() {
        log.info("Initializing game view resolution from properties: {}", gameBoardResolution);
        if (gameBoardResolution == null || gameBoardResolution.isBlank()) {
            log.error("property game.board.view.resolution is null or empty! Falling back to default 1024x768");
            return;
        }
        log.info("Parsing game view resolution: {}", gameBoardResolution);

        try {
            Matcher m = p.matcher(gameBoardResolution);
            if (m.matches()) {
                int parsedWidth = Integer.parseInt(m.group(1));
                int parsedHeight = Integer.parseInt(m.group(2));
                this.width = parsedWidth;
                this.height = parsedHeight;
                log.info("Resolution set to {}x{}", width, height);
            }
        } catch (Exception e) {
            log.error("Failed to parse resolution '{}'. Falling back to {}x{}. Error: {}",
                    gameBoardResolution, width, height, e.getMessage());
        }
    }


    public void initUiScaling(ResolutionProperties props) {
        double screenHeight = height - HEIGHT_OFFSET_TO_VIEWPORT_HEIGHT_ON_SCREEN;

        Runnable apply = () -> {
            // Center the root inside the StackPane
            StackPane.setAlignment(props.root, Pos.CENTER);

            // Available content area inside the viewport, excluding insets
            DoubleBinding availableW = Bindings.createDoubleBinding(
                    () -> {
                        props.root.setPrefWidth(width);
                        return props.viewport.getWidth()
                                - props.viewport.getInsets().getLeft()
                                - props.viewport.getInsets().getRight();
                    },
                    props.viewport.widthProperty(),
                    props.viewport.insetsProperty()
            );

            DoubleBinding availableH = Bindings.createDoubleBinding(
                    () -> {
                        props.root.setPrefHeight(screenHeight);
                        return props.viewport.getHeight()
                                - props.viewport.getInsets().getTop()
                                - props.viewport.getInsets().getBottom();
                    },
                    props.viewport.heightProperty(),
                    props.viewport.insetsProperty()
            );

            // Scale factor based on available area vs. design (root) size
            DoubleBinding scaleFactor = Bindings.createDoubleBinding(() -> {
                double scaleX = availableW.get() / width;
                double scaleY = availableH.get() / screenHeight;
                return Math.min(scaleX, scaleY);
            }, availableW, availableH);

            // Fix root to design size
            props.root.setPrefWidth(width);
            props.root.setPrefHeight(screenHeight);
            props.root.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            props.root.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

            // Create scale and bind to factor + pivot at center
            Scale scale = new Scale(1, 1);
            props.root.getTransforms().setAll(scale);
            scale.xProperty().bind(scaleFactor);
            scale.yProperty().bind(scaleFactor);

            // Use layoutBounds to determine visual center for pivot
            DoubleBinding pivotX = Bindings.createDoubleBinding(
                    () -> props.root.getLayoutBounds().getWidth() / 2.0,
                    props.root.layoutBoundsProperty()
            );
            DoubleBinding pivotY = Bindings.createDoubleBinding(
                    () -> props.root.getLayoutBounds().getHeight() / 2.0,
                    props.root.layoutBoundsProperty()
            );
            scale.pivotXProperty().bind(pivotX);
            scale.pivotYProperty().bind(pivotY);

            // Clip to available area
            Rectangle clip = (Rectangle) props.viewport.getClip();
            clip.xProperty().bind(Bindings.createDoubleBinding(
                    () -> props.viewport.getInsets().getLeft(),
                    props.viewport.insetsProperty()
            ));
            clip.yProperty().bind(Bindings.createDoubleBinding(
                    () -> props.viewport.getInsets().getTop(),
                    props.viewport.insetsProperty()
            ));
            clip.widthProperty().bind(availableW);
            clip.heightProperty().bind(availableH);
        };

        JavaFXUtils.runOnFxThread(apply);
    }


    public record ResolutionProperties(BorderPane root, StackPane viewport) {}

}
