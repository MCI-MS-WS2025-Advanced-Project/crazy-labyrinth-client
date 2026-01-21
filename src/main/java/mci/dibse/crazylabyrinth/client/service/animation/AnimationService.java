package mci.dibse.crazylabyrinth.client.service.animation;

import labyrinth.contracts.models.Direction;
import lombok.extern.slf4j.Slf4j;
import mci.dibse.crazylabyrinth.client.view.viewmodel.item.CoordinatesItem;
import mci.dibse.crazylabyrinth.client.view.viewmodel.item.GameBoardItem;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;

/**
 * Coordinator service for managing game animations.
 * Ensures animations are executed sequentially and handles animation scheduling.
 */
@Slf4j
public class AnimationService {
    private final Object animationLock = new Object();
    private GameAnimation currentAnimation = null;
    private GameAnimation pendingAnimation = null;

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

    /**
     * Schedule an animation for execution.
     * @param animation the animation to schedule
     */
    private void scheduleAnimation(GameAnimation animation) {
        synchronized (animationLock) {
            if (currentAnimation != null && currentAnimation.isRunning()) {
                log.debug("Animation already in progress, queueing new animation");
                pendingAnimation = animation;
                return;
            }
            currentAnimation = animation;
        }

        log.debug("Starting new animation: {}", animation.getClass().getSimpleName());
        
        animation.animate();
    }

    /**
     * Called by animations when they complete.
     * @param completedAnimation the animation that completed
     */
    public void onAnimationCompleted(GameAnimation completedAnimation) {
        synchronized (animationLock) {
            if (currentAnimation == completedAnimation) {
                log.debug("Animation completed: {}", completedAnimation.getClass().getSimpleName());
                currentAnimation = null;
                
                if (pendingAnimation != null) {
                    GameAnimation nextAnimation = pendingAnimation;
                    pendingAnimation = null;
                    log.debug("Starting pending animation: {}", nextAnimation.getClass().getSimpleName());
                    scheduleAnimation(nextAnimation);
                }
            }
        }
    }
}