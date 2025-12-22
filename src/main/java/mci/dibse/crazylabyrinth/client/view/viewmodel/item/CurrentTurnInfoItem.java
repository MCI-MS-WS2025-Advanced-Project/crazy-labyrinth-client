package mci.dibse.crazylabyrinth.client.view.viewmodel.item;

import java.time.OffsetDateTime;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import labyrinth.contracts.models.CurrentTurnInfo;
import labyrinth.contracts.models.TurnState;
import lombok.Getter;

public class CurrentTurnInfoItem extends BaseItem<CurrentTurnInfo> {

    @Getter private SimpleStringProperty currentPlayerId = new SimpleStringProperty();
    @Getter private SimpleObjectProperty<OffsetDateTime> turnEndTime = new SimpleObjectProperty<>();
    @Getter private SimpleObjectProperty<TurnState> turnState = new SimpleObjectProperty<>();

    public CurrentTurnInfoItem(CurrentTurnInfo currentTurnInfo) {
        currentPlayerId.set(currentTurnInfo.getCurrentPlayerId());
        turnEndTime.set(currentTurnInfo.getTurnEndTime());
        turnState.set(currentTurnInfo.getState());
    }

    public String getCurrentPlayerIdAsString() {
        return currentPlayerId.get();
    }

    @Override
    protected void updateItemFromSource(CurrentTurnInfo source) {
        currentPlayerId.setValue(source.getCurrentPlayerId());
        turnEndTime.setValue(source.getTurnEndTime());
        turnState.set(source.getState());
    }

    @Override
    protected void disposeItem() {
        currentPlayerId.unbind();
        currentPlayerId = null;
        turnEndTime.unbind();
        turnEndTime = null;
        turnState.unbind();
        turnState = null;
    }
}
