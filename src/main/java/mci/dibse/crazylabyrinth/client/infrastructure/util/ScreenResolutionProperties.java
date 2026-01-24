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
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import mci.dibse.crazylabyrinth.client.NavigationController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Component
@Slf4j
public class ScreenResolutionProperties {

    // stage size 1600X1200: viewport BoundingBox width:1600.0, height:1172.0,
    public static final double HEIGHT_OFFSET_TO_VIEWPORT_HEIGHT_ON_SCREEN = 28;
    private boolean isWindowDecorated = true;

    private static final Pattern p = Pattern.compile("(\\d+)\\s*[xX×*]\\s*(\\d+)");

    @Value("${app.scale.ratio}")
    private Boolean scaleRatio = true;

    @Value("${game.board.view.resolution:1024x768}")
    private String gameBoardResolution;
    @Getter private int width = 1024;
    @Getter private int height = 768;

    @Getter private double screenHeight  = 768;

    @Getter private  int minViewWidth = 1024;
    @Getter private  int minViewHeight = 768;

    @Setter private  Stage primaryStage;



    /**
     * Initialize the screen resolution settings from properties.
     */
    @PostConstruct
    public void init() {
        log.info("Initializing game view resolution from properties: {}", gameBoardResolution);
        log.info("Scale Ratio: {}", scaleRatio);
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
                Ratio ratio = aspectRatio(width, height);
                if (Ratio.FOUR_THREE == ratio) {
                   minViewWidth = 1024;
                   minViewHeight = 768;
                } else if(Ratio.SIXTEEN_NINE == ratio) {
                    minViewWidth = 1280;
                    minViewHeight = 720;
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse resolution '{}'. Falling back to {}x{}. Error: {}",
                    gameBoardResolution, width, height, e.getMessage());
        }
    }

    /**
     * Return the current aspect ratio based on configured width and height.
     *
     * @return aspect ratio
     */
    public Ratio getAspectRatio() {
        return aspectRatio(width, height);
    }


    /**
     * Initialize UI scaling for the provided root and viewport.
     *
     * @param props resolution properties container
     */
    public void initUiScaling(ResolutionProperties props) {


        screenHeight = height;
        if (!primaryStage.isFullScreen()) {
            isWindowDecorated = true;
            screenHeight = (int) (height - HEIGHT_OFFSET_TO_VIEWPORT_HEIGHT_ON_SCREEN);
        } else {
            isWindowDecorated = false;
        }


        double finalScreenHeight = screenHeight;
        Runnable apply = () -> {


            // Center the root inside the StackPane
            StackPane.setAlignment(props.root, Pos.CENTER);
            props.root.setPrefWidth(width);

            double rootHeight = primaryStage.getHeight() - primaryStage.getScene().getHeight();
            props.root.setPrefHeight(rootHeight);

            props.root.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            props.root.setMaxSize(width, finalScreenHeight);



            // Available content area inside the viewport, excluding insets
            DoubleBinding availableW = Bindings.createDoubleBinding(
                    () -> {
                        return props.viewport.getWidth()
                                - props.viewport.getInsets().getLeft()
                                - props.viewport.getInsets().getRight();

                    },
                    props.viewport.widthProperty(),
                    props.viewport.insetsProperty()
            );

            DoubleBinding availableH = Bindings.createDoubleBinding(
                    () -> {
                        return props.viewport.getHeight()
                                - props.viewport.getInsets().getTop()
                                - props.viewport.getInsets().getBottom();
                    },
                    props.viewport.heightProperty(),
                    props.viewport.insetsProperty()
            );

            // Scale factor based on available area vs. design (root) size
            DoubleBinding scaleFactorX = Bindings.createDoubleBinding(() -> {
                ScaleRatio ratio = getScaleRatio(availableW, availableH);
                if(!scaleRatio) return ratio.ratioX();
                return ratio.min();
            }, availableW);

            // Scale factor based on available area vs. design (root) size
            DoubleBinding scaleFactorY = Bindings.createDoubleBinding(() -> {
                ScaleRatio ratio = getScaleRatio(availableW, availableH);
                if(!scaleRatio || primaryStage.isMaximized()) return ratio.ratioY();
                return ratio.min();
            },availableH);


            // Create scale and bind to factor + pivot at center
            Scale scale = new Scale(1, 1);
            props.root.getTransforms().setAll(scale);
            scale.xProperty().bind(scaleFactorX);
            scale.yProperty().bind(scaleFactorY);

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

    /**
     * Calculate scale ratios based on available width and height.
     *
     * @param availableW available width binding
     * @param availableH available height binding
     * @return scale ratio container
     */
    private ScaleRatio getScaleRatio(DoubleBinding availableW, DoubleBinding availableH) {
        double offset = heightWithRegardsToWindowDecoration();

        double scaleX = availableW.get() / width;
        double scaleY = availableH.get() /  (height - offset);

        return new ScaleRatio(scaleX, scaleY, Math.min(scaleX, scaleY));
    }

    /**
     * Compute the height offset caused by window decorations.
     *
     * @return decoration height offset
     */
    private Double heightWithRegardsToWindowDecoration() {
        return primaryStage.getHeight() - primaryStage.getScene().getHeight();
    }



    /**
     * Compute the greatest common divisor.
     *
     * @param a first value
     * @param b second value
     * @return greatest common divisor
     */
    private static int gcd(int a, int b) {
        while (b != 0) { int t = b; b = a % b; a = t; }
        return Math.abs(a);
    }

    /**
     * Determine the aspect ratio from width and height.
     *
     * @param w width
     * @param h height
     * @return aspect ratio
     */
    private static Ratio aspectRatio(int w, int h) {
        int g = gcd(w, h);
        int first = w / g;
        int second = h / g;
        if (first == 16 && second == 9) return Ratio.SIXTEEN_NINE;
        if (first == 4 && second == 3) return Ratio.FOUR_THREE;
        return Ratio.FOUR_THREE;
    }




    private record ScaleRatio(double ratioX, double ratioY, double min) {}

    public record ResolutionProperties(BorderPane root, StackPane viewport) {}

}
