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

    /**
     * Animate the tile push for the configured row/column and direction.
     */
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

    /**
     * Callback for animation completion; no cleanup currently required.
     */
    @Override
    public void onCompleted() {
        // Cleanup if needed
    }

    /**
     * Report whether the animation is currently running.
     *
     * @return true when the animation is in progress
     */
    @Override
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Animate pushing a column up or down.
     *
     * @param columnIndex column index to move
     * @param downward true to push down, false to push up
     * @param boardGrid grid containing tiles
     * @param transitions list to collect transitions
     * @param cellsToAnimate list to collect animated cells
     */
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

    /**
     * Animate pushing a row left or right.
     *
     * @param rowIndex row index to move
     * @param rightward true to push right, false to push left
     * @param boardGrid grid containing tiles
     * @param transitions list to collect transitions
     * @param cellsToAnimate list to collect animated cells
     */
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

    /**
     * Build a transition that moves a tile to the target cell.
     *
     * @param animatedNode node to animate
     * @param targetCell destination cell
     * @param sourceCell source cell for delta calculation
     * @return configured translate transition
     */
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
