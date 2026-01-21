package mci.dibse.crazylabyrinth.client.service.animation;

import javafx.scene.Node;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import lombok.NonNull;

public class CellFinder {
    public StackPane findCellAtGridPosition(int row, int col, @NonNull GridPane boardGrid) {
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
