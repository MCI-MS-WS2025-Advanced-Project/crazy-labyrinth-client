package mci.dibse.crazylabyrinth.client.view.viewmodel.item;

import java.time.OffsetDateTime;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import labyrinth.contracts.models.CurrentTurnInfo;
import labyrinth.contracts.models.TurnState;
import lombok.Getter;
import mci.dibse.crazylabyrinth.client.infrastructure.util.JavaFXUtils;

public class
CurrentTurnInfoItem extends BaseItem<CurrentTurnInfo> {

    @Getter private SimpleStringProperty currentPlayerIdProperty = new SimpleStringProperty();

    @Getter
    private SimpleObjectProperty<OffsetDateTime> turnEndTimeProperty = new SimpleObjectProperty<>();

    @Getter
    private SimpleObjectProperty<TurnState> turnStateProperty = new SimpleObjectProperty<>();

    @Getter
    private final BooleanProperty aiTakeoverEnabledProperty = new SimpleBooleanProperty(false);

    public boolean isAiTakeoverEnabled() {
        return aiTakeoverEnabledProperty.get();
    }

    public void toggleAiTakeover() {
        aiTakeoverEnabledProperty.set(!aiTakeoverEnabledProperty.get());
    }

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

    public String getCurrentPlayerIdAsString() {
        return currentPlayerIdProperty.get();
    }

    public OffsetDateTime getTurnEndTime() {
        return turnEndTimeProperty.get();
    }

    public void addListenerToTurnEndTimeProperty(ChangeListener<OffsetDateTime> listener) {
        turnTimeChangeListener = listener;
        turnEndTimeProperty.addListener(listener);
    }

    @Override
    protected void updateItemFromSource(CurrentTurnInfo source) {
        currentPlayerIdProperty.setValue(source.getCurrentPlayerId());
        turnEndTimeProperty.setValue(source.getTurnEndTime());
        turnStateProperty.set(source.getState());
    }

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
