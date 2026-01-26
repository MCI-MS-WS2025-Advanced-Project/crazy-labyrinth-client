package mci.dibse.crazylabyrinth.client.service.animation;

import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import lombok.extern.log4j.Log4j2;
import mci.dibse.crazylabyrinth.client.view.viewmodel.item.CoordinatesItem;

@Log4j2
public class SwapAnimation extends AbstractGameAnimation {
    private final ImageView playerPawn1;
    private final ImageView playerPawn2;
    private final CoordinatesItem position1;
    private final CoordinatesItem position2;
    private final GridPane boardGrid;

    public SwapAnimation(
            AnimationService animationService,
            ImageView playerPawn1,
            ImageView playerPawn2,
            CoordinatesItem position1,
            CoordinatesItem position2,
            GridPane boardGrid) {
        super(animationService);
        this.playerPawn1 = playerPawn1;
        this.playerPawn2 = playerPawn2;
        this.position1 = position1;
        this.position2 = position2;
        this.boardGrid = boardGrid;
    }

    @Override
    protected boolean preAnimationValidation() {
        if (playerPawn1 == null || playerPawn2 == null) {
            log.error("Player pawns are null, cannot animate swap");
            return false;
        }

        CellFinder cellFinder = new CellFinder();
        StackPane cell1 =
                cellFinder.findCellAtGridPosition(
                        position1.getRowAsInt(), position1.getColumnAsInt(), boardGrid);
        StackPane cell2 =
                cellFinder.findCellAtGridPosition(
                        position2.getRowAsInt(), position2.getColumnAsInt(), boardGrid);

        if (cell1 == null || cell2 == null) {
            log.error("Could not find source or target cells for swap animation");
            return false;
        }

        cell1.toFront();
        cell2.toFront();
        return true;
    }

    @Override
    protected void executeAnimation() {
        CellFinder cellFinder = new CellFinder();
        StackPane cell1 =
                cellFinder.findCellAtGridPosition(
                        position1.getRowAsInt(), position1.getColumnAsInt(), boardGrid);
        StackPane cell2 =
                cellFinder.findCellAtGridPosition(
                        position2.getRowAsInt(), position2.getColumnAsInt(), boardGrid);

        // Calculate movement deltas for both players
        double deltaX1 =
                cell2.getBoundsInParent().getCenterX() - cell1.getBoundsInParent().getCenterX();
        double deltaY1 =
                cell2.getBoundsInParent().getCenterY() - cell1.getBoundsInParent().getCenterY();

        double deltaX2 = -deltaX1; // Player 2 moves in opposite direction
        double deltaY2 = -deltaY1;

        double duration = animationService.getAnimationConfig().getSwapAnimationDuration();

        // Create transitions for both players
        TranslateTransition transition1 =
                new TranslateTransition(Duration.millis(duration), playerPawn1);
        transition1.setToX(deltaX1);
        transition1.setToY(deltaY1);
        transition1.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition transition2 =
                new TranslateTransition(Duration.millis(duration), playerPawn2);
        transition2.setToX(deltaX2);
        transition2.setToY(deltaY2);
        transition2.setInterpolator(Interpolator.EASE_OUT);

        // Play both transitions in parallel
        ParallelTransition parallelTransition = new ParallelTransition(transition1, transition2);
        parallelTransition.setOnFinished(
                event -> {
                    handleAnimationCompletion();
                });

        parallelTransition.play();
    }

    @Override
    public void onCompleted() {
        // Reset translations after animation completes
        if (playerPawn1 != null) {
            playerPawn1.setTranslateX(0);
            playerPawn1.setTranslateY(0);
        }
        if (playerPawn2 != null) {
            playerPawn2.setTranslateX(0);
            playerPawn2.setTranslateY(0);
        }
    }
}
