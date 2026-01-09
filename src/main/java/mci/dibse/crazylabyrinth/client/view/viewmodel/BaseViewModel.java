package mci.dibse.crazylabyrinth.client.view.viewmodel;

import static mci.dibse.crazylabyrinth.client.infrastructure.util.JavaFXUtils.runOnFxThread;

import javafx.beans.property.*;
import labyrinth.contracts.models.ActionErrorEventPayload;
import labyrinth.contracts.models.ErrorCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import mci.dibse.crazylabyrinth.client.view.controller.impl.BaseViewCleanup;
import mci.dibse.crazylabyrinth.client.view.viewmodel.navigation.NavigationStateViewModel;
import org.springframework.context.event.EventListener;

@Slf4j
public abstract class BaseViewModel extends BaseViewCleanup {

    @Getter protected final StringProperty errorMessage = new SimpleStringProperty();

    @Getter
    protected final SimpleObjectProperty<ErrorCode> errorCodeProperty =
            new SimpleObjectProperty<>();

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
                    errorCodeProperty.set(null);
                });
    }

    public ErrorCode getErrorCode() {
        return errorCodeProperty.get();
    }

    protected void setError(String message) {
        runOnFxThread(
                () -> {
                    setBaseError(message);
                });
    }

    protected void setError(String message, ErrorCode code) {
        runOnFxThread(
                () -> {
                    errorCodeProperty.set(code);
                    setBaseError(message);
                });
    }

    private void setBaseError(String message) {
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
        if (isErrorRelevant()) {
            log.debug("setError {}: {}", actionError.getErrorCode(), actionError.getMessage());
            setError(actionError.getMessage(), actionError.getErrorCode());
        }
    }

    protected abstract boolean isErrorRelevant();
}
