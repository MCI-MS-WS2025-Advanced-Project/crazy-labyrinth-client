package mci.dibse.crazylabyrinth.client.service.animation;

import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Performance-optimized cache for grid cell lookups.
 * Provides O(1) access to cells by their grid coordinates instead of linear search.
 */
@Slf4j
public class GridCellCache {

    private final GridPane boardGrid;
    private StackPane[][] cellCache;
    @Getter private int gridRows = -1;
    @Getter private int gridColumns = -1;
    @Getter private boolean cacheValid = false;

    /**
     * Constructor for GridCellCache.
     *
     * @param boardGrid the grid pane to cache cells from
     */
    public GridCellCache(GridPane boardGrid) {
        this.boardGrid = boardGrid;
    }

    /**
     * Find a cell at the specified grid position using cached lookup.
     * If cache is invalid, it will be rebuilt automatically.
     *
     * @param row row index
     * @param col column index
     * @return the cell at the specified position, or null if not found
     */
    public StackPane findCellAtGridPosition(int row, int col) {
        if (!cacheValid || hasGridChanged()) {
            rebuildCache();
        }

        if (row < 0 || row >= gridRows || col < 0 || col >= gridColumns) {
            return null;
        }

        return cellCache[row][col];
    }

    /**
     * Check if the grid structure has changed since the last cache build.
     *
     * @return true if grid has changed, false otherwise
     */
    private boolean hasGridChanged() {
        int currentRows = boardGrid.getRowConstraints().size();
        int currentCols = boardGrid.getColumnConstraints().size();

        return currentRows != gridRows || currentCols != gridColumns;
    }

    /**
     * Rebuild the cell cache by scanning the grid.
     * This is an O(n) operation but only needs to be done when the grid changes.
     */
    private synchronized void rebuildCache() {
        int currentRows = boardGrid.getRowConstraints().size();
        int currentCols = boardGrid.getColumnConstraints().size();

        StackPane[][] newCache = new StackPane[currentRows][currentCols];

        for (var node : boardGrid.getChildren()) {
            if (node instanceof StackPane cellContainer) {
                Integer cellRow = GridPane.getRowIndex(cellContainer);
                Integer cellCol = GridPane.getColumnIndex(cellContainer);

                if (cellRow != null
                        && cellCol != null
                        && cellRow >= 0
                        && cellRow < currentRows
                        && cellCol >= 0
                        && cellCol < currentCols) {
                    newCache[cellRow][cellCol] = cellContainer;
                }
            }
        }

        cellCache = newCache;
        gridRows = currentRows;
        gridColumns = currentCols;
        cacheValid = true;
    }

    /**
     * Invalidate the cache to force rebuild on next access.
     * Useful when grid structure changes dynamically.
     */
    public void invalidateCache() {
        cacheValid = false;
    }
}
