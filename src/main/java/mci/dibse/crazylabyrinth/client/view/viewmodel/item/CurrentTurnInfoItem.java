package mci.dibse.crazylabyrinth.client.view.viewmodel.item;

import java.time.OffsetDateTime;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import labyrinth.contracts.models.CurrentTurnInfo;
import labyrinth.contracts.models.TurnState;
import lombok.Getter;
import mci.dibse.crazylabyrinth.client.infrastructure.util.JavaFXUtils;

public class CurrentTurnInfoItem extends BaseItem<CurrentTurnInfo> {

    @Getter private SimpleStringProperty currentPlayerIdProperty = new SimpleStringProperty();

    @Getter
    private SimpleObjectProperty<OffsetDateTime> turnEndTimeProperty = new SimpleObjectProperty<>();

    @Getter
    private SimpleObjectProperty<TurnState> turnStateProperty = new SimpleObjectProperty<>();

    private ChangeListener<OffsetDateTime> turnTimeChangeListener;

    public CurrentTurnInfoItem(CurrentTurnInfo currentTurnInfo) {
        super();
        Runnable apply = () -> {
            currentPlayerIdProperty.set(currentTurnInfo.getCurrentPlayerId());
            turnEndTimeProperty.set(currentTurnInfo.getTurnEndTime());
            turnStateProperty.set(currentTurnInfo.getState());
        };
        JavaFXUtils.runOnFxThread(apply);

    }

    /**
     * Return the current player id as a string.
     *
     * @return current player id
     */
    public String getCurrentPlayerIdAsString() {
        return currentPlayerIdProperty.get();
    }

    /**
     * Return the current turn end time.
     *
     * @return turn end time
     */
    public OffsetDateTime getTurnEndTime() {
        return turnEndTimeProperty.get();
    }

    /**
     * Add a listener for turn end time updates.
     *
     * @param listener listener to add
     */
    public void addListenerToTurnEndTimeProperty(ChangeListener<OffsetDateTime> listener) {
        turnTimeChangeListener = listener;
        turnEndTimeProperty.addListener(listener);
    }

    /**
     * Update item state from the latest turn info.
     *
     * @param source current turn info payload
     */
    @Override
    protected void updateItemFromSource(CurrentTurnInfo source) {
        currentPlayerIdProperty.setValue(source.getCurrentPlayerId());
        turnEndTimeProperty.setValue(source.getTurnEndTime());
        turnStateProperty.set(source.getState());
    }

    /**
     * Unbind and clear properties.
     */
    @Override
    protected void disposeItem() {
        currentPlayerIdProperty.unbind();
        currentPlayerIdProperty = null;
        turnEndTimeProperty.removeListener(turnTimeChangeListener);
        turnEndTimeProperty.unbind();
        turnEndTimeProperty = null;
        turnStateProperty.unbind();
        turnStateProperty = null;
    }
}
