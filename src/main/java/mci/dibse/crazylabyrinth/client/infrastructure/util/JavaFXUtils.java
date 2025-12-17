package mci.dibse.crazylabyrinth.client.infrastructure.util;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import javafx.application.Platform;
import lombok.Getter;

public class JavaFXUtils {

    @Getter
    private static final DateTimeFormatter LAST_SEEN_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    public static void runOnFxThread(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
        } else {
            Platform.runLater(action);
        }
    }
}
