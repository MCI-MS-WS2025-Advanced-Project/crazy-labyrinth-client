package mci.dibse.crazylabyrinth.client.infrastructure.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ScreenResolutionProperties {

    // stage size 1600X1200: viewport BoundingBox width:1600.0, height:1172.0,
    public static final double HEIGHT_OFFSET_TO_VIEWPORT_HEIGHT_ON_SCREEN = 28;
    @Setter private boolean borderless = false;
    private boolean isWindowDecorated = true;

    private static final Pattern p = Pattern.compile("(\\d+)\\s*[xX×*]\\s*(\\d+)");

    @Value(
            "${default.app.scale.ratio:#{T(mci.dibse.crazylabyrinth.client.domain.context.ApplicationSettingsRecord).DEFAULT_SCALE_RATIO}}")
    @Setter
    private Boolean scaleRatio;

    @Value(
            "${default.game.board.view.resolution:#{T(mci.dibse.crazylabyrinth.client.domain.context.ApplicationSettingsRecord).DEFAULT_RESOLUTION}}")
    @Setter
    private String gameBoardResolution;

    @Getter private int width;
    @Getter private int height;

    @Getter private double screenHeight;

    @Getter private int minViewWidth;
    @Getter private int minViewHeight;

    @Setter private Stage primaryStage;

    /**
     * Initialize the screen resolution settings from properties.
     */
    //  @PostConstruct
    public void init() {
        log.info("Initializing game view resolution from properties: {}", gameBoardResolution);
        log.info("Scale Ratio: {}", scaleRatio);
        if (gameBoardResolution == null || gameBoardResolution.isBlank()) {
            log.error(
                    "property game.board.view.resolution is null or empty! Falling back to default"
                            + " 1024x768");
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
                } else if (Ratio.SIXTEEN_NINE == ratio) {
                    minViewWidth = 1280;
                    minViewHeight = 720;
                }
            }
        } catch (Exception e) {
            log.error(
                    "Failed to parse resolution '{}'. Falling back to {}x{}. Error: {}",
                    gameBoardResolution,
                    width,
                    height,
                    e.getMessage());
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
        double screenWidth = width;

        if (!primaryStage.isFullScreen() && !borderless) {
            isWindowDecorated = true;
            screenHeight = (int) (height - heightWithRegardsToWindowDecoration());
            screenWidth = (int) (width - widthWithRegardsToWindowDecoration());
        } else {
            isWindowDecorated = false;
        }

        double finalScreenHeight = screenHeight;
        double finalScreenWidth = screenWidth;
        Runnable apply =
                () -> {

                    // Center the root inside the StackPane
                    StackPane.setAlignment(props.root, Pos.CENTER);
                    props.viewport.setPrefWidth(finalScreenWidth);
                    props.root.setPrefWidth(finalScreenWidth);

                    double rootHeight =
                            primaryStage.getHeight() - primaryStage.getScene().getHeight();
                    props.viewport.setPrefHeight(rootHeight);

                    props.viewport.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
                    props.viewport.setMaxSize(finalScreenWidth, finalScreenHeight);

                    props.root.setPrefHeight(rootHeight);

                    props.root.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
                    props.root.setMaxSize(finalScreenWidth, finalScreenHeight);

                    // Available content area inside the viewport, excluding insets
                    DoubleBinding availableW =
                            Bindings.createDoubleBinding(
                                    () -> {
                                        return props.viewport.getWidth()
                                                - props.viewport.getInsets().getLeft()
                                                - props.viewport.getInsets().getRight();
                                    },
                                    props.viewport.widthProperty(),
                                    props.viewport.insetsProperty());

                    DoubleBinding availableH =
                            Bindings.createDoubleBinding(
                                    () -> {
                                        return props.viewport.getHeight()
                                                - props.viewport.getInsets().getTop()
                                                - props.viewport.getInsets().getBottom();
                                    },
                                    props.viewport.heightProperty(),
                                    props.viewport.insetsProperty());

                    // Scale factor based on available area vs. design (root) size
                    DoubleBinding scaleFactorX =
                            Bindings.createDoubleBinding(
                                    () -> {
                                        ScaleRatio ratio =
                                                getScaleRatio(availableW, availableH, props);
                                        if (!scaleRatio
                                                || primaryStage.isMaximized()
                                                || primaryStage.isFullScreen()) {

                                            return ratio.ratioX();
                                        }
                                        return ratio.min();
                                    },
                                    primaryStage.heightProperty(),
                                    primaryStage.widthProperty(),
                                    primaryStage.fullScreenProperty(),
                                    primaryStage.maximizedProperty());

                    // Scale factor based on available area vs. design (root) size
                    DoubleBinding scaleFactorY =
                            Bindings.createDoubleBinding(
                                    () -> {
                                        ScaleRatio ratio =
                                                getScaleRatio(availableW, availableH, props);

                                        if (!scaleRatio
                                                || primaryStage.isMaximized()
                                                || primaryStage.isFullScreen()) {
                                            return ratio.ratioY();
                                        }
                                        return ratio.min();
                                    },
                                    primaryStage.heightProperty(),
                                    primaryStage.widthProperty(),
                                    primaryStage.fullScreenProperty(),
                                    primaryStage.maximizedProperty());

                    // Create scale and bind to factor + pivot at center
                    Scale scale = new Scale(1, 1);
                    props.root.getTransforms().setAll(scale);
                    scale.xProperty().bind(scaleFactorX);
                    scale.yProperty().bind(scaleFactorY);

                    // Use layoutBounds to determine visual center for pivot
                    DoubleBinding pivotX =
                            Bindings.createDoubleBinding(
                                    () -> props.root.getLayoutBounds().getWidth() / 2.0,
                                    props.root.layoutBoundsProperty());
                    DoubleBinding pivotY =
                            Bindings.createDoubleBinding(
                                    () -> props.root.getLayoutBounds().getHeight() / 2.0,
                                    props.root.layoutBoundsProperty());
                    scale.pivotXProperty().bind(pivotX);
                    scale.pivotYProperty().bind(pivotY);

                    // Clip to available area
                    Rectangle clip = (Rectangle) props.viewport.getClip();
                    clip.xProperty()
                            .bind(
                                    Bindings.createDoubleBinding(
                                            () -> props.viewport.getInsets().getLeft(),
                                            props.viewport.insetsProperty()));
                    clip.yProperty()
                            .bind(
                                    Bindings.createDoubleBinding(
                                            () -> props.viewport.getInsets().getTop(),
                                            props.viewport.insetsProperty()));
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
     * @param props
     * @return scale ratio container
     */
    private ScaleRatio getScaleRatio(
            DoubleBinding availableW, DoubleBinding availableH, ResolutionProperties props) {
        double offsetW = width - widthWithRegardsToWindowDecoration();
        double offsetH = height - heightWithRegardsToWindowDecoration();

        props.root.setMaxSize(offsetW, offsetH);
        props.viewport.setMaxSize(offsetW, offsetH);

        double scaleX = availableW.get() / offsetW;
        double scaleY = availableH.get() / offsetH;

        return new ScaleRatio(scaleX, scaleY, Math.min(scaleX, scaleY));
    }

    private double widthWithRegardsToWindowDecoration() {
        return primaryStage.getWidth() - primaryStage.getScene().getWidth();
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
        while (b != 0) {
            int t = b;
            b = a % b;
            a = t;
        }
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
