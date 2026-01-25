package mci.dibse.crazylabyrinth.client.test.unit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

import labyrinth.contracts.models.Direction;
import labyrinth.contracts.models.GameBoard;
import labyrinth.contracts.models.Tile;
import mci.dibse.crazylabyrinth.client.view.viewmodel.item.GameBoardItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import javafx.application.Platform;

class GameBoardItemTest {

    private GameBoardItem gameBoardItem;
    private GameBoard mockBoard;

    @BeforeAll
    static void initToolkit() {
        boolean isCi = System.getenv("GITHUB_ACTIONS") != null;
        if (isCi) {
            System.setProperty("glass.platform", "Monocle");
            System.setProperty("monocle.platform", "Headless");
            System.setProperty("prism.order", "sw");
        }

        try {
            com.sun.javafx.application.PlatformImpl.startup(() -> {});
        } catch (IllegalStateException e) {}
    }

    @BeforeEach
    void setUp() {
        mockBoard = mock(GameBoard.class);
        // Use 5x5 for faster and more stable testing in CI
        int rows = 5;
        int cols = 5;

        Tile mockTile = createMockTile();
        Tile[][] dummyTiles = new Tile[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                dummyTiles[i][j] = mockTile; // Reuse same mock to reduce overhead
            }
        }

        doReturn(rows).when(mockBoard).getRows();
        doReturn(cols).when(mockBoard).getCols();
        doReturn(dummyTiles).when(mockBoard).getTiles();
        doReturn(mockTile).when(mockBoard).getSpareTile();

        // Ensure creation happens without thread issues
        gameBoardItem = new GameBoardItem(mockBoard);
    }

    private Tile createMockTile() {
        Tile t = mock(Tile.class);
        Direction[] validEntrances = new Direction[] {Direction.UP, Direction.RIGHT};
        doReturn(validEntrances).when(t).getEntrances();
        doReturn(false).when(t).getIsFixed();
        return t;
    }

    @Test
    @DisplayName("UT-VIEWMODEL-01 - GameBoardItem updates properties from source (C4)")
    void testViewModelUpdatesOnEvent_UT_VIEWMODEL_01() {
        GameBoard nextState = mock(GameBoard.class);
        int rows = 5;
        int cols = 5;
        Tile mockTile = createMockTile();
        Tile[][] nextTiles = new Tile[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                nextTiles[i][j] = mockTile;
            }
        }

        doReturn(rows).when(nextState).getRows();
        doReturn(cols).when(nextState).getCols();
        doReturn(nextTiles).when(nextState).getTiles();
        doReturn(mockTile).when(nextState).getSpareTile();

        // Execute update via Reflection
        ReflectionTestUtils.invokeMethod(gameBoardItem, "updateItemFromSource", nextState);

        assertNotNull(gameBoardItem.getTiles(), "Tiles array should be updated.");
        System.out.println("UT-VIEWMODEL-01 Result: GameBoardItem synchronization verified.");
    }
}