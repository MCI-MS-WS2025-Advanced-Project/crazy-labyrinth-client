package mci.dibse.crazylabyrinth.client.domain.context;

public record ApplicationSettingsRecord(
        String selectedResolution, boolean scaleRatio, boolean fullscreen) {

    public static String DEFAULT_RESOLUTION = "1280x720";
    public static boolean DEFAULT_SCALE_RATIO = true;
    public static boolean DEFAULT_FULLSCREEN = false;
}
