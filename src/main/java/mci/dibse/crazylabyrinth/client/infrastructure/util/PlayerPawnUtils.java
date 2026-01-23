package mci.dibse.crazylabyrinth.client.infrastructure.util;

import javafx.scene.paint.Color;
import labyrinth.contracts.models.PlayerColor;

public class PlayerPawnUtils {

    /**
     * Map a player color enum to a JavaFX color.
     *
     * @param playerColor player color enum
     * @return JavaFX color
     */
    public static Color mapPlayerColorToFxColor(PlayerColor playerColor) {
        return switch (playerColor) {
            case RED -> Color.RED;
            case BLUE -> Color.BLUE;
            case GREEN -> Color.GREEN;
            case YELLOW -> Color.YELLOW;
        };
    }
}
