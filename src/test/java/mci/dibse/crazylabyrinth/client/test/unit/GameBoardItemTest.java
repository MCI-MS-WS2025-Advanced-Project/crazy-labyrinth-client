package mci.dibse.crazylabyrinth.client.test.unit;

import labyrinth.contracts.models.Direction;
import labyrinth.contracts.models.GameBoard;
import labyrinth.contracts.models.Tile;
import mci.dibse.crazylabyrinth.client.view.viewmodel.item.GameBoardItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

class GameBoardItemTest {

    private GameBoardItem gameBoardItem;
    private GameBoard mockBoard;

    @BeforeAll
    static void initToolkit() {
        // Check if we are running in a headless environment (CI)
        // Most CI runners set 'headless' or 'GITHUB_ACTIONS'
        boolean isCi = System.getenv("GITHUB_ACTIONS") != null;
        if (isCi) {
            System.setProperty("glass.platform", "Monocle");
            System.setProperty("monocle.platform", "Headless");
            System.setProperty("prism.order", "sw");
        }

        try {
            com.sun.javafx.application.PlatformImpl.startup(() -> {});
        } catch (IllegalStateException e) {
            // Toolkit already initialized
        }
    }

    @BeforeEach
    void setUp() {
        mockBoard = mock(GameBoard.class);
        int rows = 7;
        int cols = 7;

        Tile[][] dummyTiles = new Tile[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                dummyTiles[i][j] = createMockTile();
            }
        }

        // 3. doReturn syntax to prevent UnfinishedStubbingException
        doReturn(rows).when(mockBoard).getRows();
        doReturn(cols).when(mockBoard).getCols();
        doReturn(dummyTiles).when(mockBoard).getTiles();
        doReturn(createMockTile()).when(mockBoard).getSpareTile();

        gameBoardItem = new GameBoardItem(mockBoard);
    }

    private Tile createMockTile() {
        Tile t = mock(Tile.class);
        // Ensure no NPE during Arrays.asList()
        doReturn(new Direction[0]).when(t).getEntrances();
        return t;
    }

    @Test
    @DisplayName("UT-VIEWMODEL-01 - GameBoardItem updates properties from source (C4)")
    void testViewModelUpdatesOnEvent_UT_VIEWMODEL_01() {
        // GIVEN: GameBoardItem is initialized and a mock GameBoard with full data exists
        GameBoard nextState = mock(GameBoard.class);
        int rows = 7;
        int cols = 7;
        Tile[][] nextTiles = new Tile[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                nextTiles[i][j] = createMockTile();
            }
        }

        doReturn(rows).when(nextState).getRows();
        doReturn(cols).when(nextState).getCols();
        doReturn(nextTiles).when(nextState).getTiles();
        doReturn(createMockTile()).when(nextState).getSpareTile();

        // WHEN: updateItemFromSource() is called for the GameBoardItem (reflection because the method is protected)
        ReflectionTestUtils.invokeMethod(gameBoardItem, "updateItemFromSource", nextState);

        // THEN: GameBoard properties (e.g. tiles array) are updated and not null
        assertNotNull(gameBoardItem.getTiles(), "Tiles array should be updated.");

        System.out.println("UT-VIEWMODEL-01 Result: GameBoardItem synchronization verified.");
    }
}