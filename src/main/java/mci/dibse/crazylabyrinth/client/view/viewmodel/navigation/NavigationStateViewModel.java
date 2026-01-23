package mci.dibse.crazylabyrinth.client.view.viewmodel.navigation;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import labyrinth.contracts.models.ActionErrorEventPayload;
import labyrinth.contracts.models.ErrorCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import mci.dibse.crazylabyrinth.client.infrastructure.util.JavaFXUtils;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 *  View Model for the navigation state
 *  @author Julian Janetschek
 */
@Component
@Slf4j
public class NavigationStateViewModel {

    public enum ScreenId {
        MAIN,
        LOBBY,
        GAMEBOARD,
        RESULTS
    }

    @Getter
    private final ObjectProperty<ScreenId> currentScreenProperty =
            new SimpleObjectProperty<>(ScreenId.MAIN);

    /**
     * Navigate back to the lobby when the server rejects reconnection.
     *
     * @param error action error payload from the server
     */
    @EventListener
    public void handleReconnectionAbort(ActionErrorEventPayload error) {
        if (ErrorCode.PLAYER_NOT_FOUND.equals(error.getErrorCode())) navigateTo(ScreenId.LOBBY);
    }


    /**
     * Return the currently active screen.
     *
     * @return current screen id
     */
    public ScreenId getCurrentScreenId() {
            return currentScreenProperty.get();
    }

    /**
     * Update the current screen and notify listeners on the FX thread.
     *
     * @param screen target screen
     */
    public void navigateTo(ScreenId screen) {
        log.debug("Navigating to screen: {}", screen);
        JavaFXUtils.runOnFxThread(() -> currentScreenProperty.set(screen));
    }
}
