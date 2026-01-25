package mci.dibse.crazylabyrinth.client.service.animation;

import lombok.extern.slf4j.Slf4j;

/**
 * Abstract base class for game animations providing common functionality.
 * Implements the GameAnimation interface and handles common animation lifecycle management.
 */
@Slf4j
public abstract class AbstractGameAnimation implements GameAnimation {

    protected final AnimationService animationService;
    protected boolean isRunning = false;

    /**
     * Constructor for abstract game animation.
     *
     * @param animationService the animation service that manages this animation
     */
    protected AbstractGameAnimation(AnimationService animationService) {
        this.animationService = animationService;
    }

    /**
     * Template method for animation execution.
     * Handles the common lifecycle and delegates to concrete implementations.
     */
    @Override
    public final void animate() {
        if (isRunning) {
            log.warn("Animation {} is already running", getClass().getSimpleName());
            return;
        }

        try {
            isRunning = true;
            log.debug("Starting animation: {}", getClass().getSimpleName());

            if (!preAnimationValidation()) {
                handleAnimationCompletion();
                return;
            }

            executeAnimation();

        } catch (Exception e) {
            log.error(
                    "Error during animation {}: {}", getClass().getSimpleName(), e.getMessage(), e);
            handleAnimationError(e);
            handleAnimationCompletion();
        }
    }

    /**
     * Perform pre-animation validation.
     * Concrete implementations can override to add specific validation logic.
     *
     * @return true if animation should proceed, false to abort
     */
    protected boolean preAnimationValidation() {
        return true;
    }

    /**
     * Execute the concrete animation logic.
     * Must be implemented by subclasses.
     */
    protected abstract void executeAnimation();

    /**
     * Handle animation errors.
     * Can be overridden by subclasses for specific error handling.
     *
     * @param error the exception that occurred
     */
    protected void handleAnimationError(Exception error) {
        // Default error handling - can be overridden by subclasses
        log.error("Animation error in {}: {}", getClass().getSimpleName(), error.getMessage());
    }

    /**
     * Handle animation completion.
     * Notifies the animation service and performs cleanup.
     */
    protected final void handleAnimationCompletion() {
        isRunning = false;
        try {
            onCompleted();
        } catch (Exception e) {
            log.error(
                    "Error in onCompleted for {}: {}",
                    getClass().getSimpleName(),
                    e.getMessage(),
                    e);
        }
        animationService.onAnimationCompleted(this);
    }

    /**
     * Callback for animation completion.
     * Can be overridden by subclasses for cleanup logic.
     */
    @Override
    public void onCompleted() {
        log.debug("Animation completed: {}", getClass().getSimpleName());
    }

    /**
     * Check if the animation is currently running.
     *
     * @return true if animation is running, false otherwise
     */
    @Override
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Get the animation service associated with this animation.
     *
     * @return the animation service
     */
    protected AnimationService getAnimationService() {
        return animationService;
    }
}
