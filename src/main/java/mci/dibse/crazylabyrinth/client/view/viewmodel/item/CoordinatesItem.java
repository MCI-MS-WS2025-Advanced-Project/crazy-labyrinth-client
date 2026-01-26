package mci.dibse.crazylabyrinth.client.view.viewmodel.item;

import javafx.beans.property.SimpleIntegerProperty;
import labyrinth.contracts.models.Coordinates;
import mci.dibse.crazylabyrinth.client.infrastructure.util.JavaFXUtils;

public class CoordinatesItem extends BaseItem<Coordinates> {

    private final SimpleIntegerProperty row = new SimpleIntegerProperty();
    private final SimpleIntegerProperty column = new SimpleIntegerProperty();

    public CoordinatesItem(Coordinates source) {
        super();
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

    /**
     * Convert this item to a contracts Coordinates object.
     *
     * @return coordinates payload
     */
    public Coordinates toCoordinates() {
        Coordinates coordinates = new Coordinates();
        coordinates.setRow(this.getRowAsInt());
        coordinates.setColumn(this.getColumnAsInt());
        return coordinates;
    }

    /**
     * Return the row property.
     *
     * @return row property
     */
    public SimpleIntegerProperty rowProperty() {
        return row;
    }

    /**
     * Return the row value.
     *
     * @return row index
     */
    public int getRowAsInt() {
        return row.intValue();
    }

    /**
     * Return the column property.
     *
     * @return column property
     */
    public SimpleIntegerProperty columnProperty() {
        return column;
    }

    /**
     * Return the column value.
     *
     * @return column index
     */
    public int getColumnAsInt() {
        return column.intValue();
    }

    /**
     * Update item state from source coordinates.
     *
     * @param source source coordinates
     */
    @Override
    protected void updateItemFromSource(Coordinates source) {
        this.row.set(source.getRow());
        this.column.set(source.getColumn());
    }

    /**
     * Dispose item state; no-op for coordinates.
     */
    @Override
    protected void disposeItem() {}

    /**
     * Compare coordinates for equality.
     *
     * @param obj object to compare
     * @return true when row and column match
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CoordinatesItem other) {
            return this.getRowAsInt() == other.getRowAsInt()
                    && this.getColumnAsInt() == other.getColumnAsInt();
        }

        return false;
    }

    /**
     * Compute a hash code for the coordinate.
     *
     * @return hash code
     */
    @Override
    public int hashCode() {
        return getRowAsInt() * 31 + getColumnAsInt();
    }
}
