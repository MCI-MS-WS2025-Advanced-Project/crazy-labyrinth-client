package mci.dibse.crazylabyrinth.client.view.controller.impl;

import lombok.extern.slf4j.Slf4j;
import mci.dibse.crazylabyrinth.client.view.viewmodel.navigation.NavigationStateViewModel;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class GameStateController extends BaseViewCleanup {

    public GameStateController(NavigationStateViewModel vm) {
        super(vm);
    }

    @Override
    protected boolean customCleanupCheckpoint(
            NavigationStateViewModel.ScreenId oldValue,
            NavigationStateViewModel.ScreenId newValue) {
        return newValue != null
                && NavigationStateViewModel.ScreenId.GAMEBOARD.equals(oldValue)
                && !NavigationStateViewModel.ScreenId.GAMEBOARD.equals(newValue);
    }
}
