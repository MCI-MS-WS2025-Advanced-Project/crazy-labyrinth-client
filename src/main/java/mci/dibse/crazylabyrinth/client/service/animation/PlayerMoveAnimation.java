package mci.dibse.crazylabyrinth.client.service.animation;

import java.util.List;
import javafx.animation.Interpolator;
import javafx.animation.PathTransition;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;
import mci.dibse.crazylabyrinth.client.service.PathFindingService;
import mci.dibse.crazylabyrinth.client.view.viewmodel.item.CoordinatesItem;
import mci.dibse.crazylabyrinth.client.view.viewmodel.item.GameBoardItem;

/**
 * Animation for player movement across the game board.
 */
@Slf4j
public class PlayerMoveAnimation extends AbstractGameAnimation {

    private final GameBoardItem board;
    private final ImageView playerPawn;
    private final CoordinatesItem from;
    private final CoordinatesItem to;
    private final GridPane boardGrid;

    public PlayerMoveAnimation(
            AnimationService animationService,
            GameBoardItem board,
            ImageView playerPawn,
            CoordinatesItem from,
            CoordinatesItem to,
            GridPane boardGrid) {
        super(animationService);
        this.board = board;
        this.playerPawn = playerPawn;
        this.from = from;
        this.to = to;
        this.boardGrid = boardGrid;
    }

    /**
     * Perform pre-animation validation.
     */
    @Override
    protected boolean preAnimationValidation() {
        if (from.equals(to)) {
            log.debug("No movement for player as from and to positions are the same");
            return false;
        }
        return true;
    }

    /**
     * Execute the player movement animation.
     */
    @Override
    protected void executeAnimation() {
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
                                    return new LineTo(
                                            relativeX + widthOffset, relativeY + heightOffset);
                                })
                        .toList();

        animationPath.getElements().addAll(steps);

        PathTransition transition =
                new PathTransition(calculateDuration(path), animationPath, playerPawn);
        transition.setInterpolator(Interpolator.LINEAR);

        transition.setOnFinished(
                event -> {
                    log.debug("Movement animation completed for player");
                    handleAnimationCompletion();
                });

        transition.play();
    }

    /**
     * Callback for animation completion; no cleanup currently required.
     */
    @Override
    public void onCompleted() {
        // Cleanup if needed
    }

    /**
     * Calculate animation duration based on path length.
     *
     * @param path computed path
     * @return duration for the animation
     */
    private Duration calculateDuration(List<CoordinatesItem> path) {
        int pathSteps = path.size() - 1;
        double speedMultiplier = Math.max(0.5, 1.0 - (pathSteps - 1) * 0.1);
        double baseDuration = animationService.getAnimationConfig().getPlayerAnimationDuration();
        return Duration.millis(baseDuration * pathSteps * speedMultiplier);
    }

    /**
     * Find the stack pane for a given board position.
     *
     * @param position board coordinates
     * @param boardGrid grid containing cells
     * @return cell node for the requested position
     */
    private StackPane findCellAtPosition(CoordinatesItem position, GridPane boardGrid) {
        CellFinder cellFinder = new CellFinder();
        return cellFinder.findCellAtGridPosition(
                position.getRowAsInt(), position.getColumnAsInt(), boardGrid);
    }
}
