package mci.dibse.crazylabyrinth.client.view.viewmodel;

import static mci.dibse.crazylabyrinth.client.infrastructure.util.JavaFXUtils.runOnFxThread;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import labyrinth.contracts.models.ActionErrorEventPayload;
import lombok.Getter;
import mci.dibse.crazylabyrinth.client.view.controller.impl.BaseViewCleanup;
import mci.dibse.crazylabyrinth.client.view.viewmodel.navigation.NavigationStateViewModel;
import org.springframework.context.event.EventListener;

public abstract class BaseViewModel extends BaseViewCleanup {

    @Getter protected final StringProperty errorMessage = new SimpleStringProperty();
    @Getter protected final BooleanProperty hasError = new SimpleBooleanProperty(false);
    @Getter protected final BooleanProperty isRefreshing = new SimpleBooleanProperty(false);

    public BaseViewModel(NavigationStateViewModel vm) {
        super(vm);
    }

    public void clearError() {
        runOnFxThread(
                () -> {
                    errorMessage.set(null);
                    hasError.set(false);
                });
    }

    protected void setError(String message) {
        runOnFxThread(
                () -> {
                    errorMessage.set(message);
                    hasError.set(true);
                    isRefreshing.set(false);
                });
    }

    protected void setRefreshing(boolean refreshing) {
        runOnFxThread(
                () -> {
                    isRefreshing.set(refreshing);
                    if (refreshing) {
                        hasError.set(false);
                        errorMessage.set(null);
                    }
                });
    }

    @Override
    public void baseViewModelCleanup() {
        runOnFxThread(
                () -> {
                    errorMessage.unbind();
                    hasError.unbind();
                    isRefreshing.unbind();
                    errorMessage.set(null);
                    hasError.set(false);
                    isRefreshing.set(false);
                });
    }

    @EventListener
    private void handleError(ActionErrorEventPayload actionError) {
        if (isErrorRelevant()) setError(actionError.getMessage());
    }

    protected abstract boolean isErrorRelevant();
}
