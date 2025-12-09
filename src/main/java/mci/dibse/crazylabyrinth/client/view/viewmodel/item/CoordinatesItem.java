package mci.dibse.crazylabyrinth.client.view.viewmodel.item;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import labyrinth.contracts.models.Coordinates;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class CoordinatesItem implements Disposable{

    private final java.util.concurrent.atomic.AtomicBoolean pendingUpdate =
            new java.util.concurrent.atomic.AtomicBoolean(false);


    private final SimpleIntegerProperty x = new SimpleIntegerProperty();
    private final SimpleIntegerProperty y = new SimpleIntegerProperty();
    private final ObjectProperty<Map<String, Object>> additionalProperties = new SimpleObjectProperty<>();

    public CoordinatesItem(Coordinates source) {
        Runnable apply = () -> {
            this.x.set(source.getX());
            this.y.set(source.getY());
            this.additionalProperties.set(source.getAdditionalProperties());
        };
        if (javafx.application.Platform.isFxApplicationThread()) apply.run();
        else javafx.application.Platform.runLater(apply);

    }


    public void updateFrom(Coordinates source) {
        Runnable apply = () -> {
            this.x.set(source.getX());
            this.y.set(source.getY());
            this.additionalProperties.set(source.getAdditionalProperties());
        };
        if (javafx.application.Platform.isFxApplicationThread()) apply.run();
        else javafx.application.Platform.runLater(apply);
    }

    public SimpleIntegerProperty xProperty() {
        return x;
    }

    public SimpleIntegerProperty yProperty() {
        return y;
    }

    public ObjectProperty<Map<String, Object>> additionalPropertiesProperty() {
        return additionalProperties;
    }

    @Override
    public void dispose() {
        //TODO something to do here?
    }
}
