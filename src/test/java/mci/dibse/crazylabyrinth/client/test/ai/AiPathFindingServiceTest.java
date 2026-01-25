package mci.dibse.crazylabyrinth.client.test.ai;

import labyrinth.contracts.models.Coordinates;
import labyrinth.contracts.models.Direction;
import labyrinth.contracts.models.GameBoard;
import labyrinth.contracts.models.Tile;
import mci.dibse.crazylabyrinth.client.ai.AiContractUtil;
import mci.dibse.crazylabyrinth.client.ai.AiPathFindingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link AiPathFindingService}.
 * <p>
 * Focus: correctness of entrance reciprocity, and correctness of virtual push / two-push simulation.
 * </p>
 */
class AiPathFindingServiceTest {

    private final AiPathFindingService sut = new AiPathFindingService();

    /**
     * Creates a tile with the given entrances (no treasure/bonus semantics needed for pathfinding).
     *
     * @param entrances entrances the tile exposes
     * @return tile instance
     */
    private static Tile tile(Direction... entrances) {
        Tile t = new Tile();
        t.setEntrances(entrances);
        t.setIsFixed(Boolean.FALSE);
        return t;
    }

    /**
     * Creates a {@link GameBoard} with explicit rows/cols, tile matrix and spare tile.
     *
     * @param rows row count
     * @param cols col count
     * @param tiles tile matrix (row-major)
     * @param spare spare tile
     * @return board instance
     */
    private static GameBoard board(int rows, int cols, Tile[][] tiles, Tile spare) {
        GameBoard b = new GameBoard();
        b.setRows(rows);
        b.setCols(cols);
        b.setTiles(tiles);
        b.setSpareTile(spare);
        return b;
    }

    /**
     * Asserts that a path is non-empty and starts/ends at the expected coordinates.
     *
     * @param path returned path
     * @param sr start row
     * @param sc start col
     * @param tr target row
     * @param tc target col
     */
    private static void assertPathStartsEnds(List<Coordinates> path, int sr, int sc, int tr, int tc) {
        assertNotNull(path);
        assertFalse(path.isEmpty(), "path must not be empty");
        Coordinates first = path.get(0);
        Coordinates last = path.get(path.size() - 1);
        assertEquals(sr, first.getRow());
        assertEquals(sc, first.getColumn());
        assertEquals(tr, last.getRow());
        assertEquals(tc, last.getColumn());
    }

    /**
     * UT-AICLIENT-12
     * <p>
     * Verifies that {@link AiPathFindingService#findPath(GameBoard, Coordinates, Coordinates)} enforces
     * the reciprocal-entrance rule: moving from A to B requires A has entrance towards B AND B has entrance back.
     * </p>
     */
    @Test
    @DisplayName("UT-AICLIENT-12: findPath enforces reciprocal entrances (one-way corridor is NOT traversable)")
    void findPathReciprocalEntrancesRequired() {
        Tile[][] tiles = new Tile[][]{
                new Tile[]{
                        tile(Direction.RIGHT),
                        tile() // no LEFT -> blocked from the other side
                }
        };

        GameBoard b = board(1, 2, tiles, tile(Direction.LEFT, Direction.RIGHT));

        Coordinates start = AiContractUtil.coord(0, 0);
        Coordinates target = AiContractUtil.coord(0, 1);

        List<Coordinates> path = sut.findPath(b, start, target);
        assertTrue(path.isEmpty(), "should be unreachable because entrances are not reciprocal");

        // Make entrances reciprocal -> now reachable
        tiles[0][1] = tile(Direction.LEFT);

        List<Coordinates> path2 = sut.findPath(b, start, target);
        assertPathStartsEnds(path2, 0, 0, 0, 1);
        assertEquals(2, path2.size(), "direct neighbor -> shortest path length must be 2");
    }

    /**
     * UT-AICLIENT-13
     * <p>
     * Verifies that {@link AiPathFindingService#findPathAfterVirtualPush(GameBoard, int, Direction, Direction[], Coordinates, Coordinates)}
     * actually changes the effective tile layout via virtual mapping (without mutating the board),
     * enabling a path that is impossible on the real board.
     * </p>
     *
     * <p>
     * Important: We choose a DOWN push on column 1 to replace the blocking middle tile at (1,1) with the tile from (0,1).
     * That is exactly what {@code getVirtualTileAt(...)} implements for DOWN pushes.
     * </p>
     */
    @Test
    @DisplayName("UT-AICLIENT-13: findPathAfterVirtualPush can open a corridor that does NOT exist on the real board")
    void virtualPushOpensCorridor() {
        Tile H = tile(Direction.LEFT, Direction.RIGHT);
        Tile V = tile(Direction.UP, Direction.DOWN);

        // Real board is blocked on row 1 because middle tile is vertical:
        // row 1: [H, V, H]
        //
        // We set (0,1)=H so a virtual DOWN push on column 1 will move (0,1) into (1,1),
        // turning row 1 into [H, H, H] in the virtual post-push view.
        Tile[][] tiles = new Tile[][]{
                new Tile[]{ H, H, H }, // (0,1)=H is the key
                new Tile[]{ H, V, H }, // (1,1)=V blocks before push
                new Tile[]{ H, H, H }
        };

        Tile spare = tile(Direction.UP, Direction.DOWN); // spare entrances don't matter for opening row 1 corridor here
        GameBoard b = board(3, 3, tiles, spare);

        Coordinates start = AiContractUtil.coord(1, 0);
        Coordinates target = AiContractUtil.coord(1, 2);

        List<Coordinates> before = sut.findPath(b, start, target);
        assertTrue(before.isEmpty(), "without push, row 1 is blocked by the vertical tile in the middle");

        Direction[] rotatedSpareEntrances = AiContractUtil.rotateEntrances(spare.getEntrances(), 0);

        List<Coordinates> after = sut.findPathAfterVirtualPush(
                b,
                1, Direction.DOWN, // push column 1 down
                rotatedSpareEntrances,
                start, target
        );

        assertPathStartsEnds(after, 1, 0, 1, 2);
        assertEquals(3, after.size(), "corridor across 3 tiles -> expected 3 coordinates");
    }

    /**
     * UT-AICLIENT-14
     * <p>
     * Verifies that {@link AiPathFindingService#findPathAfterTwoPushes(GameBoard, int, Direction, int, int, Direction, int, Coordinates, Coordinates)}
     * uses the tile pushed out by push #1 as the spare tile for push #2.
     * If push #2 incorrectly reuses the original spare, this test will fail.
     * </p>
     */
    @Test
    @DisplayName("UT-AICLIENT-14: findPathAfterTwoPushes uses tile pushed out by push #1 as spare for push #2")
    void twoPushesUsesPushedOutTileAsSecondSpare() {
        Tile H = tile(Direction.LEFT, Direction.RIGHT);
        Tile BLOCKED = tile(); // no entrances

        Tile[][] tiles = new Tile[][]{
                new Tile[]{ H, H, H },
                new Tile[]{ H, H, H },
                new Tile[]{ H, H, H }
        };

        // Original spare is BLOCKED, push1 will insert BLOCKED and push out an H tile.
        GameBoard b = board(3, 3, tiles, BLOCKED);

        Coordinates startAfterBothPushes = AiContractUtil.coord(1, 0);
        Coordinates target = AiContractUtil.coord(1, 2);

        List<Coordinates> sanity = sut.findPath(b, startAfterBothPushes, target);
        assertPathStartsEnds(sanity, 1, 0, 1, 2);

        // After ONE virtual push with BLOCKED inserted at (row1,col0), corridor breaks.
        List<Coordinates> afterPush1 = sut.findPathAfterVirtualPushWithTile(
                b,
                1, Direction.RIGHT,
                BLOCKED,
                startAfterBothPushes,
                target
        );
        assertTrue(afterPush1.isEmpty(), "after push1, insertion tile is blocked -> corridor breaks");

        // After TWO pushes, the pushed-out H from push1 must be used as spare for push2,
        // restoring the horizontal corridor.
        List<Coordinates> afterTwo = sut.findPathAfterTwoPushes(
                b,
                1, Direction.RIGHT, 0,
                1, Direction.LEFT, 0,
                startAfterBothPushes,
                target
        );

        assertPathStartsEnds(afterTwo, 1, 0, 1, 2);
        assertEquals(3, afterTwo.size(), "corridor restored after 2nd push -> expected 3 coordinates");
    }
}
