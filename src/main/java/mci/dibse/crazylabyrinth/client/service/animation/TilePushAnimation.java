package mci.dibse.crazylabyrinth.client.service.animation;

import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import labyrinth.contracts.models.Direction;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Animation for tile pushing in the game board.
 */
@Slf4j
public class TilePushAnimation implements GameAnimation {
    
    private final AnimationService animationService;
    private final int rowOrColIndex;
    private final Direction pushDirection;
    private final GridPane boardGrid;
    private boolean isRunning = false;

    private static final double TILE_PUSH_ANIMATION_DURATION = 600;

    public TilePushAnimation(AnimationService animationService, int rowOrColIndex,
                           Direction pushDirection, GridPane boardGrid) {
        this.animationService = animationService;
        this.rowOrColIndex = rowOrColIndex;
        this.pushDirection = pushDirection;
        this.boardGrid = boardGrid;
    }

    @Override
    public void animate() {
        isRunning = true;

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
            isRunning = false;
            animationService.onAnimationCompleted(this);
            return;
        }

        cellsToAnimate.forEach(StackPane::toFront);

        ParallelTransition parallelTransition = new ParallelTransition();
        parallelTransition.getChildren().addAll(transitions);

        parallelTransition.setOnFinished(
                event -> {
                    log.debug("Tile push animation completed for {} tiles", transitions.size());
                    isRunning = false;
                    animationService.onAnimationCompleted(this);
                });

        parallelTransition.play();
    }

    @Override
    public void onCompleted() {
        // Cleanup if needed
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    private void animateColumnPush(
            int columnIndex,
            boolean downward,
            GridPane boardGrid,
            List<TranslateTransition> transitions,
            List<StackPane> cellsToAnimate) {
        int gridSize = boardGrid.getRowConstraints().size();

        CellFinder cellFinder = new CellFinder();
        for (int row = 0; row < gridSize; row++) {
            StackPane currentCell = cellFinder.findCellAtGridPosition(row, columnIndex, boardGrid);
            if (currentCell == null) continue;

            if (downward) {
                if (row == gridSize - 1) {
                    continue;
                }
            } else {
                if (row == 0) {
                    continue;
                }
            }

            StackPane targetCell;
            if (downward) {
                targetCell = cellFinder.findCellAtGridPosition(row + 1, columnIndex, boardGrid);
            } else {
                targetCell = cellFinder.findCellAtGridPosition(row - 1, columnIndex, boardGrid);
            }

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
        CellFinder cellFinder = new CellFinder();

        for (int col = 0; col < gridSize; col++) {
            StackPane currentCell = cellFinder.findCellAtGridPosition(rowIndex, col, boardGrid);
            if (currentCell == null) continue;

            if (rightward) {
                if (col == gridSize - 1) {
                    continue;
                }
            } else {
                if (col == 0) {
                    continue;
                }
            }

            StackPane targetCell;
            if (rightward) {
                targetCell = cellFinder.findCellAtGridPosition(rowIndex, col + 1, boardGrid);
            } else {
                targetCell = cellFinder.findCellAtGridPosition(rowIndex, col - 1, boardGrid);
            }

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
}