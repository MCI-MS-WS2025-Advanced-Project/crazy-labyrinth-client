package mci.dibse.crazylabyrinth.client.view.viewmodel.navigation;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import lombok.Getter;
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
    private final ObjectProperty<ScreenId> currentScreen = new SimpleObjectProperty<>(ScreenId.MAIN);

    public void navigateTo(ScreenId screen) {
        currentScreen.set(screen);
    }
}
