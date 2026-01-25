package mci.dibse.crazylabyrinth.client.service.animation;

import lombok.Getter;

/**
 * Centralized configuration for animation parameters.
 * Provides configurable animation durations and settings.
 */
public class AnimationConfig {

    private double playerAnimationDuration = 500.0;
    private double tilePushAnimationDuration = 600.0;
    private double beamAnimationDuration = 600.0;

    @Getter private double globalSpeedMultiplier = 1.0;

    private static final double MIN_DURATION = 100.0;
    private static final double MAX_DURATION = 2000.0;

    /**
     * Get the duration for player movement animations.
     *
     * @return player animation duration in milliseconds
     */
    public double getPlayerAnimationDuration() {
        return constrainDuration(playerAnimationDuration * globalSpeedMultiplier);
    }

    /**
     * Set the duration for player movement animations.
     *
     * @param duration player animation duration in milliseconds
     */
    public void setPlayerAnimationDuration(double duration) {
        this.playerAnimationDuration = constrainDuration(duration);
    }

    /**
     * Get the duration for tile push animations.
     *
     * @return tile push animation duration in milliseconds
     */
    public double getTilePushAnimationDuration() {
        return constrainDuration(tilePushAnimationDuration * globalSpeedMultiplier);
    }

    /**
     * Set the duration for tile push animations.
     *
     * @param duration tile push animation duration in milliseconds
     */
    public void setTilePushAnimationDuration(double duration) {
        this.tilePushAnimationDuration = constrainDuration(duration);
    }

    /**
     * Get the duration for beam animations.
     *
     * @return beam animation duration in milliseconds
     */
    public double getBeamAnimationDuration() {
        return constrainDuration(beamAnimationDuration * globalSpeedMultiplier);
    }

    /**
     * Set the duration for beam animations.
     *
     * @param duration beam animation duration in milliseconds
     */
    public void setBeamAnimationDuration(double duration) {
        this.beamAnimationDuration = constrainDuration(duration);
    }

    /**
     * Set the global speed multiplier for all animations.
     * Values < 1.0 make animations faster, values > 1.0 make them slower.
     *
     * @param multiplier speed multiplier (must be positive)
     */
    public void setGlobalSpeedMultiplier(double multiplier) {
        if (multiplier > 0) {
            this.globalSpeedMultiplier = multiplier;
        }
    }

    /**
     * Reset all animation durations to their default values.
     */
    public void resetToDefaults() {
        this.playerAnimationDuration = 500.0;
        this.tilePushAnimationDuration = 600.0;
        this.beamAnimationDuration = 600.0;
        this.globalSpeedMultiplier = 1.0;
    }

    /**
     * Constrain a duration value to be within acceptable bounds.
     *
     * @param duration the duration to constrain
     * @return constrained duration value
     */
    private double constrainDuration(double duration) {
        return Math.max(MIN_DURATION, Math.min(MAX_DURATION, duration));
    }

    /**
     * Create a new AnimationConfig with default values.
     *
     * @return new AnimationConfig instance
     */
    public static AnimationConfig createDefault() {
        return new AnimationConfig();
    }

    /**
     * Create a new AnimationConfig with fast animation settings.
     *
     * @return new AnimationConfig instance with fast settings
     */
    public static AnimationConfig createFast() {
        AnimationConfig config = new AnimationConfig();
        config.setGlobalSpeedMultiplier(0.7);
        return config;
    }

    /**
     * Create a new AnimationConfig with slow animation settings.
     *
     * @return new AnimationConfig instance with slow settings
     */
    public static AnimationConfig createSlow() {
        AnimationConfig config = new AnimationConfig();
        config.setGlobalSpeedMultiplier(1.5);
        return config;
    }
}
