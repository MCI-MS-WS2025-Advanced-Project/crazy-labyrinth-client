package mci.dibse.crazylabyrinth.client.view.viewmodel.item;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;


//TODO needed?
public class PLayerTurnItem implements Disposable {


    private final java.util.concurrent.atomic.AtomicBoolean pendingUpdate =
            new java.util.concurrent.atomic.AtomicBoolean(false);
    private final StringProperty playerId = new SimpleStringProperty();
    private final SimpleObjectProperty<String> playerName = new SimpleObjectProperty<>();




    @Override
    public void dispose() {
        //TODO something to do here?
    }
}


