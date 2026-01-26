package mci.dibse.crazylabyrinth.client.service.animation;

import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import lombok.extern.log4j.Log4j2;
import mci.dibse.crazylabyrinth.client.view.viewmodel.item.CoordinatesItem;

@Log4j2
public class BeamAnimation extends AbstractGameAnimation {
    private final ImageView playerPawn;
    private final CoordinatesItem from;
    private final CoordinatesItem to;
    private final GridPane boardGrid;

    public BeamAnimation(
            AnimationService animationService,
            ImageView playerPawn,
            CoordinatesItem from,
            CoordinatesItem coordinatesItem,
            GridPane boardGrid) {
        super(animationService);
        this.playerPawn = playerPawn;
        this.from = from;
        this.to = coordinatesItem;
        this.boardGrid = boardGrid;
    }

    @Override
    protected boolean preAnimationValidation() {
        if (playerPawn == null) {
            log.error("Player pawn is null, cannot animate beam");
            return false;
        }

        CellFinder cellFinder = new CellFinder();
        StackPane fromCell =
                cellFinder.findCellAtGridPosition(
                        from.getRowAsInt(), from.getColumnAsInt(), boardGrid);
        StackPane targetCell =
                cellFinder.findCellAtGridPosition(to.getRowAsInt(), to.getColumnAsInt(), boardGrid);

        if (fromCell == null || targetCell == null) {
            log.error("Could not find source or target cell for beam animation");
            return false;
        }

        fromCell.toFront();
        return true;
    }

    @Override
    protected void executeAnimation() {
        CellFinder cellFinder = new CellFinder();
        StackPane fromCell =
                cellFinder.findCellAtGridPosition(
                        from.getRowAsInt(), from.getColumnAsInt(), boardGrid);
        StackPane targetCell =
                cellFinder.findCellAtGridPosition(to.getRowAsInt(), to.getColumnAsInt(), boardGrid);

        double deltaX =
                targetCell.getBoundsInParent().getCenterX()
                        - fromCell.getBoundsInParent().getCenterX();
        double deltaY =
                targetCell.getBoundsInParent().getCenterY()
                        - fromCell.getBoundsInParent().getCenterY();

        double duration = animationService.getAnimationConfig().getBeamAnimationDuration();
        TranslateTransition transition =
                new TranslateTransition(Duration.millis(duration), playerPawn);
        transition.setToX(deltaX);
        transition.setToY(deltaY);
        transition.setInterpolator(Interpolator.EASE_OUT);
        transition.setOnFinished(
                event -> {
                    handleAnimationCompletion();
                });

        transition.play();
    }

    @Override
    public void onCompleted() {}
}
