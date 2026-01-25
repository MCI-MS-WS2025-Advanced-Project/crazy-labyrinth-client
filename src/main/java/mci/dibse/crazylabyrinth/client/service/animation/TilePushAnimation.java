package mci.dibse.crazylabyrinth.client.service.animation;

import java.util.ArrayList;
import java.util.List;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import labyrinth.contracts.models.Direction;
import lombok.extern.slf4j.Slf4j;

/**
 * Animation for tile pushing in the game board.
 */
@Slf4j
public class TilePushAnimation extends AbstractGameAnimation {

    private final int rowOrColIndex;
    private final Direction pushDirection;
    private final GridPane boardGrid;

    public TilePushAnimation(
            AnimationService animationService,
            int rowOrColIndex,
            Direction pushDirection,
            GridPane boardGrid) {
        super(animationService);
        this.rowOrColIndex = rowOrColIndex;
        this.pushDirection = pushDirection;
        this.boardGrid = boardGrid;
    }

    /**
     * Animate the tile push for the configured row/column and direction.
     */
    @Override
    protected void executeAnimation() {
        List<TranslateTransition> transitions = new ArrayList<>();
        List<StackPane> cellsToAnimate = new ArrayList<>();

        animatePush(rowOrColIndex, pushDirection, boardGrid, transitions, cellsToAnimate);

        if (transitions.isEmpty()) {
            handleAnimationCompletion();
            return;
        }

        cellsToAnimate.forEach(StackPane::toFront);

        ParallelTransition parallelTransition = new ParallelTransition();
        parallelTransition.getChildren().addAll(transitions);

        parallelTransition.setOnFinished(
                event -> {
                    handleAnimationCompletion();
                });

        parallelTransition.play();
    }

    /**
     * Callback for animation completion
     */
    @Override
    public void onCompleted() {}

    /**
     * Check if the current position is valid for pushing in the given direction.
     *
     * @param currentIndex current position index
     * @param isForward true if pushing forward (down/right), false if pushing backward (up/left)
     * @param gridSize total size of the grid dimension
     * @return true if position is valid for pushing
     */
    private boolean isValidPositionForPush(int currentIndex, boolean isForward, int gridSize) {
        if (isForward) {
            return currentIndex < gridSize - 1;
        } else {
            return currentIndex > 0;
        }
    }

    /**
     * Animate pushing tiles in the specified direction.
     * Handles both row and column pushes based on the direction parameter.
     *
     * @param index row or column index to move
     * @param pushDirection direction to push tiles
     * @param boardGrid grid containing tiles
     * @param transitions list to collect transitions
     * @param cellsToAnimate list to collect animated cells
     */
    private void animatePush(
            int index,
            Direction pushDirection,
            GridPane boardGrid,
            List<TranslateTransition> transitions,
            List<StackPane> cellsToAnimate) {

        boolean isVertical = pushDirection == Direction.UP || pushDirection == Direction.DOWN;
        int gridSize =
                isVertical
                        ? boardGrid.getRowConstraints().size()
                        : boardGrid.getColumnConstraints().size();
        boolean isForward = pushDirection == Direction.DOWN || pushDirection == Direction.RIGHT;

        CellFinder cellFinder = new CellFinder();

        for (int i = 0; i < gridSize; i++) {
            int row = isVertical ? i : index;
            int col = isVertical ? index : i;

            StackPane currentCell = cellFinder.findCellAtGridPosition(row, col, boardGrid);
            if (currentCell == null) continue;

            if (!isValidPositionForPush(i, isForward, gridSize)) {
                continue;
            }

            int targetRow = row;
            int targetCol = col;

            if (isVertical) {
                targetRow = isForward ? row + 1 : row - 1;
            } else {
                targetCol = isForward ? col + 1 : col - 1;
            }

            StackPane targetCell =
                    cellFinder.findCellAtGridPosition(targetRow, targetCol, boardGrid);
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

        double duration = animationService.getAnimationConfig().getTilePushAnimationDuration();
        TranslateTransition transition =
                new TranslateTransition(Duration.millis(duration), animatedNode);
        transition.setToX(deltaX);
        transition.setToY(deltaY);
        transition.setInterpolator(Interpolator.EASE_OUT);

        return transition;
    }
}
