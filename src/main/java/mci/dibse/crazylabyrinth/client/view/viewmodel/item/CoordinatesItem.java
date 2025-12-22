package mci.dibse.crazylabyrinth.client.view.viewmodel.item;

import javafx.beans.property.SimpleIntegerProperty;
import labyrinth.contracts.models.Coordinates;
import mci.dibse.crazylabyrinth.client.infrastructure.util.JavaFXUtils;

public class CoordinatesItem extends BaseItem<Coordinates> {

    private final SimpleIntegerProperty row = new SimpleIntegerProperty();
    private final SimpleIntegerProperty column = new SimpleIntegerProperty();

    public CoordinatesItem(Coordinates source) {
        Runnable apply =
                () -> {
                    this.row.set(source.getRow());
                    this.column.set(source.getColumn());
                };
        JavaFXUtils.runOnFxThread(apply);
    }

    public SimpleIntegerProperty rowProperty() {
        return row;
    }

    public int getRowAsInt() {
        return row.intValue();
    }

    public SimpleIntegerProperty columnProperty() {
        return column;
    }

    public int getColumnAsInt() {
        return column.intValue();
    }

    @Override
    protected void updateItemFromSource(Coordinates source) {
        this.row.set(source.getRow());
        this.column.set(source.getColumn());
    }

    @Override
    protected void disposeItem() {}
}
