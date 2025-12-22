package mci.dibse.crazylabyrinth.client.view.viewmodel.navigation;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import labyrinth.contracts.models.ActionErrorEventPayload;
import labyrinth.contracts.models.ErrorCode;
import lombok.Getter;
import mci.dibse.crazylabyrinth.client.infrastructure.util.JavaFXUtils;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 *  View Model for the navigation state
 *  @author Julian Janetschek
 */
@Component
public class NavigationStateViewModel {

    public enum ScreenId {
        MAIN,
        LOBBY,
        GAMEBOARD,
        RESULTS
    }

    @Getter
    private final ObjectProperty<ScreenId> currentScreen =
            new SimpleObjectProperty<>(ScreenId.MAIN);

    @EventListener
    public void handleReconnectionAbort(ActionErrorEventPayload error) {
        if (ErrorCode.PLAYER_NOT_FOUND.equals(error.getErrorCode())) navigateTo(ScreenId.LOBBY);
    }

    public void navigateTo(ScreenId screen) {
        JavaFXUtils.runOnFxThread(() -> currentScreen.set(screen));
    }
}
