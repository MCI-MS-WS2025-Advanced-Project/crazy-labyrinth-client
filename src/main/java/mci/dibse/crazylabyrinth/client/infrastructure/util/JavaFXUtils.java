package mci.dibse.crazylabyrinth.client.infrastructure.util;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Skin;
import javafx.scene.control.TableView;
import javafx.scene.shape.Rectangle;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JavaFXUtils {

    private static final String CLIP_KEY = "tableClipRect";

    @Getter
    private static final DateTimeFormatter LAST_SEEN_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    public static void runOnFxThread(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
        } else {
            Platform.runLater(action);
        }
    }

    public static void showAlertErrorModal(
            String title, String header, String content, Runnable onClose) {
        runOnFxThread(
                () -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle(title);
                    alert.setHeaderText(header);
                    alert.setContentText(content);
                    alert.showAndWait();
                    alert.setOnHidden(
                            e -> {
                                onClose.run();
                            });
                });
    }

    public static <T> Runnable setupTableClip(TableView<T> table, Double fixedCellSize) {
       if (fixedCellSize != null) table.setFixedCellSize(fixedCellSize);

       log.debug("FixedCellSize {}", table.getFixedCellSize());


        ChangeListener<Skin<?>> skinListener = (obs, oldSkin, newSkin) -> {
            if (newSkin == null) {
                return;
            }
            runOnFxThread(() -> installOrUpdateClip(table));
        };

        table.skinProperty().addListener(skinListener);
        if (table.getSkin() != null) {
            skinListener.changed(table.skinProperty(), null, table.getSkin());
        }

        // Disposer: remove listener and cleanup
        return () -> {
            table.skinProperty().removeListener(skinListener);
            Platform.runLater(() -> {
                Node flow = table.lookup(".virtual-flow");
                if (flow != null) {
                    Node sheet = flow.lookup(".sheet");
                    if (sheet != null) {
                        Object clipObj = sheet.getProperties().remove(CLIP_KEY);
                        if (clipObj instanceof Rectangle clip) {
                            clip.widthProperty().unbind();
                            clip.heightProperty().unbind();
                            sheet.setClip(null);
                        }
                    }
                }
            });
        };
    }

    private static void installOrUpdateClip(TableView<?> table) {
        Node flow = table.lookup(".virtual-flow");
        if (flow == null) {
            return;
        }
        Node sheet = flow.lookup(".sheet");
        if (sheet == null) {
            return;
        }

        Rectangle clip = null;
        Object existing = sheet.getProperties().get(CLIP_KEY);
        if (existing instanceof Rectangle r) {
            clip = r;
            clip.widthProperty().unbind();
            clip.heightProperty().unbind();
        } else {
            clip = new Rectangle();
            sheet.setClip(clip);
            sheet.getProperties().put(CLIP_KEY, clip);
        }

        // Width: match the flow's content width
        clip.widthProperty().bind(flow.layoutBoundsProperty().map(Bounds::getWidth));

        // Height: floor to an integer number of rows so the last partial row is fully hidden
        clip.heightProperty().bind(Bindings.createDoubleBinding(
                () -> {
                    Bounds b = flow.getLayoutBounds();
                    double cellSize = table.getFixedCellSize();
                    if (cellSize <= 0) return Math.max(0.0, b.getHeight());
                    double h = Math.floor(b.getHeight() / cellSize) * cellSize;
                    return Math.max(0.0, h);
                },
                flow.layoutBoundsProperty(),
                table.fixedCellSizeProperty()
        ));
    }
}
