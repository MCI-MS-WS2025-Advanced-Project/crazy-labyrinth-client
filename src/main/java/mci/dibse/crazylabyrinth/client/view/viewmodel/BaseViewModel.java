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

    /**
     * Clear the current error state and message.
     */
    public void clearError() {
        runOnFxThread(
                () -> {
                    errorMessage.set(null);
                    hasError.set(false);
                    errorCodeProperty.set(null);
                });
    }

    /**
     * Return the current error code.
     *
     * @return error code or null when no error is set
     */
    public ErrorCode getErrorCode() {
        return errorCodeProperty.get();
    }

    /**
     * Set an error message without an error code.
     *
     * @param message error message
     */
    protected void setError(String message) {
        runOnFxThread(
                () -> {
                    setBaseError(message);
                });
    }

    /**
     * Set an error message with an error code.
     *
     * @param message error message
     * @param code error code
     */
    protected void setError(String message, ErrorCode code) {
        runOnFxThread(
                () -> {
                    errorCodeProperty.set(code);
                    setBaseError(message);
                });
    }

    /**
     * Update error state and stop any refresh indicator.
     *
     * @param message error message
     */
    private void setBaseError(String message) {
        runOnFxThread(
                () -> {
                    errorMessage.set(message);
                    hasError.set(true);
                    isRefreshing.set(false);
                });
    }

    /**
     * Update the refresh state, clearing errors when refreshing begins.
     *
     * @param refreshing true when a refresh is in progress
     */
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

    /**
     * Reset view model bindings and error state.
     */
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

    /**
     * Handle action errors from the server and set them on the view model.
     *
     * @param actionError error payload
     */
    @EventListener
    private void handleError(ActionErrorEventPayload actionError) {
        if (isErrorRelevant()) {
            log.debug("setError {}: {}", actionError.getErrorCode(), actionError.getMessage());
            setError(actionError.getMessage(), actionError.getErrorCode());
        }
    }

    /**
     * Determine whether errors should be surfaced for this view model.
     *
     * @return true when the error is relevant
     */
    protected abstract boolean isErrorRelevant();
}
