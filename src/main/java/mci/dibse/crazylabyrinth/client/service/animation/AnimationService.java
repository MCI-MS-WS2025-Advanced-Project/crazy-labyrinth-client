package mci.dibse.crazylabyrinth.client.service.animation;

import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import labyrinth.contracts.models.Direction;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import mci.dibse.crazylabyrinth.client.view.viewmodel.item.CoordinatesItem;
import mci.dibse.crazylabyrinth.client.view.viewmodel.item.GameBoardItem;

/**
 * Coordinator service for managing game animations.
 * Ensures animations are executed sequentially and handles animation scheduling.
 */
@Slf4j
public class AnimationService {
    private final Object animationLock = new Object();
    private GameAnimation currentAnimation = null;
    private GameAnimation pendingAnimation = null;
    @Getter @Setter private AnimationConfig animationConfig;

    /**
     * Constructor for AnimationService with custom configuration.
     * @param animationConfig the animation configuration to use
     */
    public AnimationService(AnimationConfig animationConfig) {
        this.animationConfig = animationConfig;
    }

    /**
     * Constructor for AnimationService with default configuration.
     */
    public AnimationService() {
        this(AnimationConfig.createDefault());
    }

    /**
     * Check if any animation is currently in progress.
     * @return true if an animation is running, false otherwise
     */
    public boolean isAnimationInProgress() {
        synchronized (animationLock) {
            return currentAnimation != null && currentAnimation.isRunning();
        }
    }

    /**
     * Animate player movement from one position to another.
     * @param board the game board
     * @param playerPawn the player pawn to animate
     * @param from starting position
     * @param to target position
     * @param boardGrid the UI grid pane
     */
    public void animatePlayerPath(
            GameBoardItem board,
            ImageView playerPawn,
            CoordinatesItem from,
            CoordinatesItem to,
            GridPane boardGrid) {
        scheduleAnimation(new PlayerMoveAnimation(this, board, playerPawn, from, to, boardGrid));
    }

    /**
     * Animate tile pushing in a specific direction.
     * @param rowOrColIndex the row or column index to push
     * @param pushDirection the direction to push tiles
     * @param boardGrid the UI grid pane
     */
    public void animateTilePush(int rowOrColIndex, Direction pushDirection, GridPane boardGrid) {
        scheduleAnimation(new TilePushAnimation(this, rowOrColIndex, pushDirection, boardGrid));
    }

    public void animateBeam(
            ImageView playerPawn, CoordinatesItem from, CoordinatesItem to, GridPane boardGrid) {
        scheduleAnimation(new BeamAnimation(this, playerPawn, from, to, boardGrid));
    }

    /**
     * Animate two players swapping positions.
     * @param playerPawn1 the first player pawn to animate
     * @param playerPawn2 the second player pawn to animate
     * @param position1 starting position of first player / target position of second player
     * @param position2 starting position of second player / target position of first player
     * @param boardGrid the UI grid pane
     */
    public void animateSwap(
            ImageView playerPawn1,
            ImageView playerPawn2,
            CoordinatesItem position1,
            CoordinatesItem position2,
            GridPane boardGrid) {
        scheduleAnimation(
                new SwapAnimation(this, playerPawn1, playerPawn2, position1, position2, boardGrid));
    }

    /**
     * Schedule an animation for execution.
     * @param animation the animation to schedule
     */
    private void scheduleAnimation(GameAnimation animation) {
        synchronized (animationLock) {
            if (currentAnimation != null && currentAnimation.isRunning()) {
                pendingAnimation = animation;
                return;
            }
            currentAnimation = animation;
        }

        animation.animate();
    }

    /**
     * Called by animations when they complete.
     * @param completedAnimation the animation that completed
     */
    public void onAnimationCompleted(GameAnimation completedAnimation) {
        synchronized (animationLock) {
            if (currentAnimation == completedAnimation) {
                currentAnimation = null;

                if (pendingAnimation != null) {
                    GameAnimation nextAnimation = pendingAnimation;
                    pendingAnimation = null;
                    scheduleAnimation(nextAnimation);
                }
            }
        }
    }
}
