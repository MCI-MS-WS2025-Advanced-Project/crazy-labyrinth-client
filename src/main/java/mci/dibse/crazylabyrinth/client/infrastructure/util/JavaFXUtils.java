package mci.dibse.crazylabyrinth.client.infrastructure.util;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Skin;
import javafx.scene.control.TableView;
import javafx.scene.shape.Rectangle;
import lombok.Getter;

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

    public static void showAlertModal(
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

    public static <T> Runnable setupTableClip(TableView<T> table, int fixedCellSize) {
        table.setFixedCellSize(fixedCellSize);

        ChangeListener<Skin<?>> skinListener =
                (obs, oldSkin, newSkin) -> {
                    if (newSkin == null) return;

                    Runnable task =
                            () -> {
                                Node flow = table.lookup(".virtual-flow");
                                if (flow == null) return;
                                Rectangle clip = new Rectangle();
                                flow.setClip(clip);
                                flow.getProperties().put(CLIP_KEY, clip);

                                clip.widthProperty()
                                        .bind(flow.layoutBoundsProperty().map(Bounds::getWidth));

                                clip.heightProperty()
                                        .bind(
                                                Bindings.createDoubleBinding(
                                                        () -> {
                                                            Bounds b = flow.getLayoutBounds();
                                                            double row = table.getFixedCellSize();
                                                            double h =
                                                                    Math.floor(b.getHeight() / row)
                                                                            * row;
                                                            // Defensive: non-negative
                                                            return Math.max(0.0, h);
                                                        },
                                                        flow.layoutBoundsProperty(),
                                                        table.fixedCellSizeProperty()));
                            };
                    runOnFxThread(task);
                };

        table.skinProperty().addListener(skinListener);
        if (table.getSkin() != null) {
            skinListener.changed(table.skinProperty(), null, table.getSkin());
        }

        // Return a disposer you can call to remove listener and cleanup
        return () -> {
            table.skinProperty().removeListener(skinListener);

            Runnable task =
                    () -> {
                        Node flow = table.lookup(".virtual-flow");
                        if (flow != null) {
                            Object clipObj = flow.getProperties().remove(CLIP_KEY);
                            if (clipObj instanceof Rectangle clip) {
                                clip.widthProperty().unbind();
                                clip.heightProperty().unbind();
                                flow.setClip(null);
                            }
                        }
                    };
            runOnFxThread(task);
        };
    }
}
