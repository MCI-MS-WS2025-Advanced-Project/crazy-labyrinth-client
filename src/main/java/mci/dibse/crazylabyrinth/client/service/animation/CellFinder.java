package mci.dibse.crazylabyrinth.client.service.animation;

import javafx.scene.Node;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import lombok.NonNull;

/**
 * Utility class for finding cells in a grid.
 * Can use either direct search or cached lookup for better performance.
 */
public class CellFinder {

    private GridCellCache cellCache = null;

    /**
     * Set a grid cell cache to use for optimized lookups.
     * If no cache is set, will fall back to linear search.
     *
     * @param cellCache the cache to use, or null to disable caching
     */
    public void setCellCache(GridCellCache cellCache) {
        this.cellCache = cellCache;
    }

    /**
     * Locate the stack pane cell at the given grid position.
     * Uses cached lookup if available, otherwise falls back to linear search.
     *
     * @param row row index
     * @param col column index
     * @param boardGrid grid containing cells
     * @return matching cell container or null when not found
     */
    public StackPane findCellAtGridPosition(int row, int col, @NonNull GridPane boardGrid) {
        // Try cached lookup first if available
        if (cellCache != null) {
            return cellCache.findCellAtGridPosition(row, col);
        }

        // Fall back to linear search
        return findCellAtGridPositionLinear(row, col, boardGrid);
    }

    /**
     * Locate the stack pane cell at the given grid position using linear search.
     * This is the fallback method when no cache is available.
     *
     * @param row row index
     * @param col column index
     * @param boardGrid grid containing cells
     * @return matching cell container or null when not found
     */
    private StackPane findCellAtGridPositionLinear(int row, int col, @NonNull GridPane boardGrid) {
        for (Node node : boardGrid.getChildren()) {
            if (node instanceof StackPane cellContainer) {
                Integer cellRow = GridPane.getRowIndex(cellContainer);
                Integer cellCol = GridPane.getColumnIndex(cellContainer);

                if (cellRow != null && cellCol != null && cellRow == row && cellCol == col) {
                    return cellContainer;
                }
            }
        }
        return null;
    }
}
