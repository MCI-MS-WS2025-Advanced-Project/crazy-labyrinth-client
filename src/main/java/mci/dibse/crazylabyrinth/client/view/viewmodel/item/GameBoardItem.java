package mci.dibse.crazylabyrinth.client.view.viewmodel.item;

import javafx.beans.property.*;
import labyrinth.contracts.models.GameBoard;
import labyrinth.contracts.models.PushActionInfo;
import labyrinth.contracts.models.Tile;
import lombok.Getter;

import java.util.Map;

public class GameBoardItem implements Disposable {


    private final java.util.concurrent.atomic.AtomicBoolean pendingUpdate =
            new java.util.concurrent.atomic.AtomicBoolean(false);

    private final IntegerProperty rows = new SimpleIntegerProperty();
    private final IntegerProperty cols = new SimpleIntegerProperty();
    @Getter  private final ObjectProperty<TileItem[][]> tiles = new SimpleObjectProperty<>();
    private final ObjectProperty<PushActionInfo> lastPush = new SimpleObjectProperty<>();
    private final ObjectProperty<Map<String, Object>> additionalProperties = new SimpleObjectProperty<>();

    public GameBoardItem(GameBoard source) {
        Runnable apply = () -> {

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
            this.additionalProperties.set(source.getAdditionalProperties());
        };
        if (javafx.application.Platform.isFxApplicationThread()) apply.run();
        else javafx.application.Platform.runLater(apply);
    }


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


    public Map<String, Object> getAdditionalProperties() {
        return additionalProperties.get();
    }

    public void setAdditionalProperties(Map<String, Object> value) {
        additionalProperties.set(value);
    }

    public ObjectProperty<Map<String, Object>> additionalPropertiesProperty() {
        return additionalProperties;
    }


    public void dispose(){
        // Remove listener to prevent memory leaks
        // Clear collections and remove listeners
        // Clear properties
        // Clear references
        rows.set(0);
        cols.set(0);
        tiles.set(null);
        lastPush.set(null);
        additionalProperties.set(null);
    }

    public void updateFrom(GameBoard source) {
        Runnable apply = () -> {

            TileItem[][] tileItems = new TileItem[source.getRows()][source.getCols()];
            Tile[][] sourceTiles = source.getTiles();
            for (int row = 0; row < source.getRows(); row++) {
                for (int col = 0; col < source.getCols(); col++) {
                        tileItems[row][col] = new TileItem(sourceTiles[row][col]);
                }
            }
            this.tiles.set(tileItems);

            setLastPush(source.getLastPush());
            setAdditionalProperties(source.getAdditionalProperties());
        };
        if (javafx.application.Platform.isFxApplicationThread()) apply.run();
        else javafx.application.Platform.runLater(apply);
    }
}
