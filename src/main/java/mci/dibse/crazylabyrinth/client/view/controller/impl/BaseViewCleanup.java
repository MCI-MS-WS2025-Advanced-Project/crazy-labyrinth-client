package mci.dibse.crazylabyrinth.client.view.controller.impl;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import mci.dibse.crazylabyrinth.client.view.viewmodel.ViewCleanup;
import mci.dibse.crazylabyrinth.client.view.viewmodel.navigation.NavigationStateViewModel;

@Slf4j
public abstract class BaseViewCleanup implements ViewCleanup {

    protected NavigationStateViewModel navigationStateViewModel;

    private final javafx.beans.value.ChangeListener<? super NavigationStateViewModel.ScreenId>
            cleanupListener =
                    (observable, oldValue, newValue) -> {
                        log.debug("Screen changed from {} to {}", oldValue, newValue);
                        // JavaFX props should not fire if values are equal
                        if (oldValue != null && oldValue.equals(newValue)) return;
                        if (!customCleanupCheckpoint(oldValue, newValue)) return;
                        baseViewModelCleanup();
                        cleanup();
                    };

    public BaseViewCleanup(NavigationStateViewModel navigationStateViewModel) {
        this.navigationStateViewModel = navigationStateViewModel;
        init();
    }

    /**
     * Remove the cleanup listener before bean destruction.
     */
    @PreDestroy
    public void destroy() {
        navigationStateViewModel.getCurrentScreenProperty().removeListener(cleanupListener);
    }

    // override to impolement class specific cleanup
    /**
     * Perform subclass-specific cleanup when navigation changes.
     */
    public void cleanup() {
        log.debug("Cleaning up view...nothing to do here.");
    }

    // used for viewModel base class
    /**
     * Run base cleanup logic for view models.
     */
    protected void baseViewModelCleanup() {}

    /**
     * Register the cleanup listener on the navigation state.
     */
    private void init() {
        navigationStateViewModel.getCurrentScreenProperty().addListener(cleanupListener);
    }

    /**
     * Decide whether cleanup should run for the given navigation change.
     *
     * @param oldValue previous screen
     * @param newValue new screen
     * @return true when cleanup should run
     */
    protected boolean customCleanupCheckpoint(
            NavigationStateViewModel.ScreenId oldValue,
            NavigationStateViewModel.ScreenId newValue) {
        return false;
    }
}
