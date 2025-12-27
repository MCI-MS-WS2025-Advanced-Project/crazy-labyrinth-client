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

    public CoordinatesItem(int row, int column) {
        Runnable apply =
                () -> {
                    this.row.set(row);
                    this.column.set(column);
                };
        JavaFXUtils.runOnFxThread(apply);
    }

    public Coordinates toCoordinates() {
        Coordinates coordinates = new Coordinates();
        coordinates.setRow(this.getRowAsInt());
        coordinates.setColumn(this.getColumnAsInt());
        return coordinates;
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

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CoordinatesItem other) {
            return this.getRowAsInt() == other.getRowAsInt()
                    && this.getColumnAsInt() == other.getColumnAsInt();
        }

        return false;
    }

    @Override
    public int hashCode() {
        return getRowAsInt() * 31 + getColumnAsInt();
    }
}
