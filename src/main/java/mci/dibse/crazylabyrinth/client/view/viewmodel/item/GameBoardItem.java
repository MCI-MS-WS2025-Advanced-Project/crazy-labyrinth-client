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

    public int getRows() {
        return rows.get();
    }

    public void setRows(int value) {
        rows.set(value);
    }

    public IntegerProperty rowsProperty() {
        return rows;
    }

    public int getCols() {
        return cols.get();
    }

    public void setCols(int value) {
        cols.set(value);
    }

    public IntegerProperty colsProperty() {
        return cols;
    }

    public TileItem[][] getTiles() {
        return tiles.get();
    }

    public void setTiles(TileItem[][] value) {
        tiles.set(value);
    }

    public ObjectProperty<TileItem[][]> tilesProperty() {
        return tiles;
    }

    public PushActionInfo getLastPush() {
        return lastPush.get();
    }

    public void setLastPush(PushActionInfo value) {
        lastPush.set(value);
    }

    public ObjectProperty<PushActionInfo> lastPushProperty() {
        return lastPush;
    }

    public TileItem getSpareTile() {
        return spareTile.get();
    }

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

    @Override
    protected void disposeItem() {
        rows.set(0);
        cols.set(0);
        tiles.set(null);
        lastPush.set(null);
        spareTile.set(null);
    }

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
