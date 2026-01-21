package mci.dibse.crazylabyrinth.client.service.animation;

/**
 * Interface for game animations that can be scheduled and executed sequentially.
 */
public interface GameAnimation {
    
    /**
     * Execute the animation.
     */
    void animate();
    
    /**
     * Called when the animation completes.
     */
    void onCompleted();
    
    /**
     * Check if the animation is currently running.
     * @return true if animation is running, false otherwise
     */
    boolean isRunning();
}