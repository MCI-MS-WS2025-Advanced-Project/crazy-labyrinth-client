package mci.dibse.crazylabyrinth.client.view.viewmodel.item;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import labyrinth.contracts.models.GameBoard;
import labyrinth.contracts.models.PushActionInfo;
import labyrinth.contracts.models.Tile;
import lombok.Getter;
import mci.dibse.crazylabyrinth.client.infrastructure.util.JavaFXUtils;

public class GameBoardItem extends BaseItem<GameBoard> {

    private final IntegerProperty rows = new SimpleIntegerProperty();
    private final IntegerProperty cols = new SimpleIntegerProperty();
    @Getter private final ObjectProperty<TileItem[][]> tiles = new SimpleObjectProperty<>();
    private final ObjectProperty<PushActionInfo> lastPush = new SimpleObjectProperty<>();
    private final ObjectProperty<TileItem> spareTile = new SimpleObjectProperty<>();

    /**
     * Return the number of board rows.
     *
     * @return row count
     */
    public int getRows() {
        return rows.get();
    }

    /**
     * Set the number of board rows.
     *
     * @param value row count
     */
    public void setRows(int value) {
        rows.set(value);
    }

    /**
     * Return the rows property.
     *
     * @return rows property
     */
    public IntegerProperty rowsProperty() {
        return rows;
    }

    /**
     * Return the number of board columns.
     *
     * @return column count
     */
    public int getCols() {
        return cols.get();
    }

    /**
     * Set the number of board columns.
     *
     * @param value column count
     */
    public void setCols(int value) {
        cols.set(value);
    }

    /**
     * Return the columns property.
     *
     * @return columns property
     */
    public IntegerProperty colsProperty() {
        return cols;
    }

    /**
     * Return the tile matrix.
     *
     * @return tile grid
     */
    public TileItem[][] getTiles() {
        return tiles.get();
    }

    /**
     * Set the tile matrix.
     *
     * @param value tile grid
     */
    public void setTiles(TileItem[][] value) {
        tiles.set(value);
    }

    /**
     * Return the tiles property.
     *
     * @return tiles property
     */
    public ObjectProperty<TileItem[][]> tilesProperty() {
        return tiles;
    }

    /**
     * Return the last push action info.
     *
     * @return last push payload
     */
    public PushActionInfo getLastPush() {
        return lastPush.get();
    }

    /**
     * Set the last push action info.
     *
     * @param value last push payload
     */
    public void setLastPush(PushActionInfo value) {
        lastPush.set(value);
    }

    /**
     * Return the last push property.
     *
     * @return last push property
     */
    public ObjectProperty<PushActionInfo> lastPushProperty() {
        return lastPush;
    }

    /**
     * Return the spare tile item.
     *
     * @return spare tile item
     */
    public TileItem getSpareTile() {
        return spareTile.get();
    }

    /**
     * Return the spare tile property.
     *
     * @return spare tile property
     */
    public ObjectProperty<TileItem> getSpareTileProperty() {
        return spareTile;
    }

    public GameBoardItem(GameBoard source) {
        super();
        Runnable apply =
                () -> {
                    this.rows.set(source.getRows());
                    this.cols.set(source.getCols());
                    TileItem[][] tileItems = new TileItem[source.getRows()][source.getCols()];
                    Tile[][] sourceTiles = source.getTiles();
                    for (int row = 0; row < source.getRows(); row++) {
                        for (int col = 0; col < source.getCols(); col++) {
                            tileItems[row][col] = new TileItem(sourceTiles[row][col]);
                        }
                    }
                    this.tiles.set(tileItems);
                    this.lastPush.set(source.getLastPush());
                    TileItem spareTile = new TileItem(source.getSpareTile());
                    this.spareTile.set(spareTile);
                };
        JavaFXUtils.runOnFxThread(apply);
    }

    /**
     * Reset item state and release references.
     */
    @Override
    protected void disposeItem() {
        rows.set(0);
        cols.set(0);
        tiles.set(null);
        lastPush.set(null);
        spareTile.set(null);
    }

    /**
     * Update item state from the latest source board.
     *
     * @param source source game board
     */
    @Override
    protected void updateItemFromSource(GameBoard source) {
        TileItem[][] tileItems = new TileItem[source.getRows()][source.getCols()];
        Tile[][] sourceTiles = source.getTiles();
        for (int row = 0; row < source.getRows(); row++) {
            for (int col = 0; col < source.getCols(); col++) {
                tileItems[row][col] = new TileItem(sourceTiles[row][col]);
            }
        }
        this.tiles.set(tileItems);
        setLastPush(source.getLastPush());
        TileItem spareTile = new TileItem(source.getSpareTile());
        this.spareTile.set(spareTile);
    }
}
