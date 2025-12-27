package mci.dibse.crazylabyrinth.client.service;

import java.util.ArrayList;
import java.util.List;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.PathTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.util.Duration;
import labyrinth.contracts.models.Direction;
import lombok.extern.slf4j.Slf4j;
import mci.dibse.crazylabyrinth.client.view.viewmodel.item.CoordinatesItem;
import mci.dibse.crazylabyrinth.client.view.viewmodel.item.GameBoardItem;


@Slf4j
public class AnimationService {
    private int animationsInProgress = 0;

    private static final double PLAYER_ANIMATION_DURATION = 500;
    private static final double TILE_PUSH_ANIMATION_DURATION = 600;

    public boolean isAnimationInProgress() {
        return animationsInProgress > 0;
    }

    public void animatePlayerPath(
            GameBoardItem board,
            ImageView playerPawn,
            CoordinatesItem from,
            CoordinatesItem to,
            GridPane boardGrid) {
        if (from.equals(to)) {
            log.debug("No movement for player as from and to positions are the same");
            return;
        }
        animationsInProgress++;

        List<CoordinatesItem> path = new PathFindingService().findPath(board, from, to);

        Path animationPath = new Path();

        StackPane startCell = findCellAtPosition(from, boardGrid);
        startCell.toFront();

        double startX = startCell.getBoundsInParent().getCenterX();
        double startY = startCell.getBoundsInParent().getCenterY();

        double widthOffset = playerPawn.getBoundsInParent().getWidth() / 2;
        double heightOffset = playerPawn.getBoundsInParent().getHeight() / 2;

        MoveTo start = new MoveTo(0 + widthOffset, 0 + heightOffset);
        animationPath.getElements().add(start);

        List<LineTo> steps =
                path.stream()
                        .skip(1)
                        .map(
                                coordinates -> {
                                    StackPane targetCell =
                                            findCellAtPosition(coordinates, boardGrid);
                                    double relativeX =
                                            targetCell.getBoundsInParent().getCenterX() - startX;
                                    double relativeY =
                                            targetCell.getBoundsInParent().getCenterY() - startY;
                                    return new LineTo(relativeX + widthOffset, relativeY + heightOffset);
                                })
                        .toList();

        animationPath.getElements().addAll(steps);

        PathTransition transition =
                new PathTransition(calculateDuration(path), animationPath, playerPawn);
        transition.setInterpolator(Interpolator.LINEAR);

        transition.setOnFinished(
                event -> {
                    log.debug("Movement animation completed for player");
                    animationsInProgress--;
                });

        transition.play();
    }

    private static Duration calculateDuration(List<CoordinatesItem> path) {
        int pathSteps = path.size() - 1;
        double speedMultiplier = Math.max(0.5, 1.0 - (pathSteps - 1) * 0.1);
        return Duration.millis(PLAYER_ANIMATION_DURATION * pathSteps * speedMultiplier);
    }

    public void animateTilePush(int rowOrColIndex, Direction pushDirection, GridPane boardGrid) {
        animationsInProgress++;

        log.debug(
                "Animating tile push for {} index {} in direction {}",
                (pushDirection == Direction.UP || pushDirection == Direction.DOWN)
                        ? "column"
                        : "row",
                rowOrColIndex,
                pushDirection);

        List<TranslateTransition> transitions = new ArrayList<>();
        List<StackPane> cellsToAnimate = new ArrayList<>();

        switch (pushDirection) {
            case DOWN ->
                    animateColumnPush(rowOrColIndex, true, boardGrid, transitions, cellsToAnimate);
            case UP ->
                    animateColumnPush(rowOrColIndex, false, boardGrid, transitions, cellsToAnimate);
            case RIGHT ->
                    animateRowPush(rowOrColIndex, true, boardGrid, transitions, cellsToAnimate);
            case LEFT ->
                    animateRowPush(rowOrColIndex, false, boardGrid, transitions, cellsToAnimate);
        }

        if (transitions.isEmpty()) {
            log.debug("No tiles to animate for push");
            animationsInProgress--;
            return;
        }

        cellsToAnimate.forEach(StackPane::toFront);

        ParallelTransition parallelTransition = new ParallelTransition();
        parallelTransition.getChildren().addAll(transitions);

        parallelTransition.setOnFinished(
                event -> {
                    log.debug("Tile push animation completed for {} tiles", transitions.size());
                    animationsInProgress--;
                });

        parallelTransition.play();
    }

    private void animateColumnPush(
            int columnIndex,
            boolean downward,
            GridPane boardGrid,
            List<TranslateTransition> transitions,
            List<StackPane> cellsToAnimate) {
        int gridSize = boardGrid.getRowConstraints().size();

        for (int row = 0; row < gridSize; row++) {
            StackPane currentCell = findCellAtGridPosition(row, columnIndex, boardGrid);
            if (currentCell == null) continue;

            CoordinatesItem to;

            if (downward) {
                if (row == gridSize - 1) {
                    continue;
                } else {
                    to = new CoordinatesItem(row + 1, columnIndex);
                }
            } else {
                if (row == 0) {
                    continue;
                } else {
                    to = new CoordinatesItem(row - 1, columnIndex);
                }
            }

            StackPane targetCell =
                    findCellAtGridPosition(to.getRowAsInt(), to.getColumnAsInt(), boardGrid);
            if (targetCell != null) {
                TranslateTransition transition =
                        createTileTransition(currentCell, targetCell, currentCell);
                transitions.add(transition);
                cellsToAnimate.add(currentCell);
            }
        }
    }

    private void animateRowPush(
            int rowIndex,
            boolean rightward,
            GridPane boardGrid,
            List<TranslateTransition> transitions,
            List<StackPane> cellsToAnimate) {
        int gridSize = boardGrid.getColumnConstraints().size();

        for (int col = 0; col < gridSize; col++) {
            StackPane currentCell = findCellAtGridPosition(rowIndex, col, boardGrid);
            if (currentCell == null) continue;

            CoordinatesItem to;

            if (rightward) {
                if (col == gridSize - 1) {
                    continue;
                } else {
                    to = new CoordinatesItem(rowIndex, col + 1);
                }
            } else {
                if (col == 0) {
                    continue;
                } else {
                    to = new CoordinatesItem(rowIndex, col - 1);
                }
            }

            StackPane targetCell =
                    findCellAtGridPosition(to.getRowAsInt(), to.getColumnAsInt(), boardGrid);
            if (targetCell != null) {
                TranslateTransition transition =
                        createTileTransition(currentCell, targetCell, currentCell);
                transitions.add(transition);
                cellsToAnimate.add(currentCell);
            }
        }
    }

    private TranslateTransition createTileTransition(
            StackPane animatedNode, StackPane targetCell, StackPane sourceCell) {
        double deltaX =
                targetCell.getBoundsInParent().getCenterX()
                        - sourceCell.getBoundsInParent().getCenterX();
        double deltaY =
                targetCell.getBoundsInParent().getCenterY()
                        - sourceCell.getBoundsInParent().getCenterY();

        TranslateTransition transition =
                new TranslateTransition(
                        Duration.millis(TILE_PUSH_ANIMATION_DURATION), animatedNode);
        transition.setToX(deltaX);
        transition.setToY(deltaY);
        transition.setInterpolator(Interpolator.EASE_OUT);

        return transition;
    }

    private StackPane findCellAtPosition(CoordinatesItem position, GridPane boardGrid) {
        return findCellAtGridPosition(position.getRowAsInt(), position.getColumnAsInt(), boardGrid);
    }

    private StackPane findCellAtGridPosition(int row, int col, GridPane boardGrid) {
        for (Node node : boardGrid.getChildren()) {
            if (node instanceof StackPane cellContainer) {
                Integer cellRow = GridPane.getRowIndex(cellContainer);
                Integer cellCol = GridPane.getColumnIndex(cellContainer);

                if (cellRow != null && cellCol != null && cellRow == row && cellCol == col) {
                    return cellContainer;
                }
            }
        }
        return null;
    }
}
