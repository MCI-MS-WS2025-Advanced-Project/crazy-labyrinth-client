package mci.dibse.crazylabyrinth.client.test.ai;

import labyrinth.contracts.models.*;
import mci.dibse.crazylabyrinth.client.ai.AiContractUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AiContractUtilTest {

    /**
     * UT-AICLIENT-01:
     * coord(row,col) is called
     * => returns Coordinates with matching row and column.
     */
    @Test
    @DisplayName("UT-AICLIENT-01: coord creates Coordinates with given row and column")
    void coordCreatesCoordinatesWithGivenRowAndColumn() {
        // given
        int row = 3;
        int col = 5;

        // when
        Coordinates c = AiContractUtil.coord(row, col);

        // then
        assertNotNull(c);
        assertEquals(row, c.getRow());
        assertEquals(col, c.getColumn());
    }

    /**
     * UT-AICLIENT-02:
     * hasEntrance is called with null tile or null entrances
     * => returns false.
     */
    @Test
    @DisplayName("UT-AICLIENT-02: hasEntrance returns false for null tile or null entrances")
    void hasEntranceNullSafetyReturnsFalse() {
        // given
        Tile tile = new Tile();
        tile.setEntrances(null);

        // when
        boolean nullTile = AiContractUtil.hasEntrance(null, Direction.UP);
        boolean nullEntrances = AiContractUtil.hasEntrance(tile, Direction.UP);

        // then
        assertFalse(nullTile);
        assertFalse(nullEntrances);
    }

    /**
     * UT-AICLIENT-03:
     * hasEntrance is called with a tile that has an entrance direction
     * => returns true for that direction and false for others.
     */
    @Test
    @DisplayName("UT-AICLIENT-03: hasEntrance detects contained entrance direction")
    void hasEntranceDetectsContainedDirection() {
        // given
        Tile tile = new Tile();
        tile.setEntrances(new Direction[]{Direction.UP, Direction.LEFT});

        // when
        boolean hasUp = AiContractUtil.hasEntrance(tile, Direction.UP);
        boolean hasRight = AiContractUtil.hasEntrance(tile, Direction.RIGHT);

        // then
        assertTrue(hasUp);
        assertFalse(hasRight);
    }

    /**
     * UT-AICLIENT-04:
     * tileAt is called with invalid board/indices or missing tiles array
     * => returns null.
     */
    @Test
    @DisplayName("UT-AICLIENT-04: tileAt returns null for null board, missing tiles or out-of-bounds indices")
    void tileAtNullAndBoundsReturnsNull() {
        // given
        GameBoard board = new GameBoard();
        board.setTiles(null);

        Tile[][] tiles = new Tile[2][2];
        GameBoard boardWithTiles = new GameBoard();
        boardWithTiles.setTiles(tiles);

        // when
        Tile nullBoard = AiContractUtil.tileAt(null, 0, 0);
        Tile nullTiles = AiContractUtil.tileAt(board, 0, 0);
        Tile negative = AiContractUtil.tileAt(boardWithTiles, -1, 0);
        Tile outRow = AiContractUtil.tileAt(boardWithTiles, 2, 0);
        Tile outCol = AiContractUtil.tileAt(boardWithTiles, 0, 2);

        // then
        assertNull(nullBoard);
        assertNull(nullTiles);
        assertNull(negative);
        assertNull(outRow);
        assertNull(outCol);
    }

    /**
     * UT-AICLIENT-05:
     * tileAt is called with valid board and indices
     * => returns the tile instance at that position.
     */
    @Test
    @DisplayName("UT-AICLIENT-05: tileAt returns the tile at given board position when accessible")
    void tileAtValidReturnsTile() {
        // given
        Tile t = new Tile();
        Tile[][] tiles = new Tile[2][2];
        tiles[1][0] = t;

        GameBoard board = new GameBoard();
        board.setTiles(tiles);

        // when
        Tile result = AiContractUtil.tileAt(board, 1, 0);

        // then
        assertSame(t, result);
    }

    /**
     * UT-AICLIENT-06:
     * rotateEntrances is called with rotations (including negative / >3)
     * => normalizes rotations and returns a NEW array, without modifying input.
     */
    @Test
    @DisplayName("UT-AICLIENT-06: rotateEntrances normalizes rotations and returns rotated copy")
    void rotateEntrancesNormalizesAndReturnsCopy() {
        // given
        Direction[] in = new Direction[]{Direction.UP, Direction.LEFT};

        // when
        Direction[] rot1 = AiContractUtil.rotateEntrances(in, 1);   // UP->RIGHT, LEFT->UP
        Direction[] rotNeg3 = AiContractUtil.rotateEntrances(in, -3); // same as +1
        Direction[] rot4 = AiContractUtil.rotateEntrances(in, 4);   // identity copy

        // then
        assertArrayEquals(new Direction[]{Direction.RIGHT, Direction.UP}, rot1);
        assertArrayEquals(new Direction[]{Direction.RIGHT, Direction.UP}, rotNeg3);

        assertArrayEquals(new Direction[]{Direction.UP, Direction.LEFT}, rot4);
        assertNotSame(in, rot4);

        // input must remain unchanged
        assertArrayEquals(new Direction[]{Direction.UP, Direction.LEFT}, in);
    }

    /**
     * UT-AICLIENT-07:
     * cloneTileWithEntrances is called
     * => returns a new Tile that copies treasure + bonus and sets entrances + isFixed flag.
     */
    @Test
    @DisplayName("UT-AICLIENT-07: cloneTileWithEntrances copies treasure and bonus and applies entrances and fixed flag")
    void cloneTileWithEntrancesCopiesTreasureBonusAndSetsFields() {
        // given
        Tile base = new Tile();

        Treasure treasure = new Treasure();
        treasure.setId(42);
        base.setTreasure(treasure);

        BonusType bonus = BonusType.BEAM;
        base.setBonus(bonus);

        Direction[] entrances = new Direction[]{Direction.UP, Direction.RIGHT};

        // when
        Tile cloned = AiContractUtil.cloneTileWithEntrances(base, entrances, true);

        // then
        assertNotNull(cloned);
        assertNotSame(base, cloned);

        assertSame(treasure, cloned.getTreasure());
        assertEquals(bonus, cloned.getBonus());
        assertArrayEquals(entrances, cloned.getEntrances());
        assertTrue(Boolean.TRUE.equals(cloned.getIsFixed()));
    }

    /**
     * UT-AICLIENT-08:
     * canShift is called
     * => returns true only for odd indices within bounds (rows for LEFT/RIGHT, cols for UP/DOWN).
     */
    @Test
    @DisplayName("UT-AICLIENT-08: canShift validates odd index and bounds depending on direction")
    void canShiftValidatesOddIndexAndBounds() {
        // given
        GameBoard board = new GameBoard();
        board.setRows(7);
        board.setCols(7);

        // when / then
        assertTrue(AiContractUtil.canShift(board, 1, Direction.RIGHT));
        assertTrue(AiContractUtil.canShift(board, 5, Direction.LEFT));
        assertTrue(AiContractUtil.canShift(board, 1, Direction.UP));
        assertTrue(AiContractUtil.canShift(board, 5, Direction.DOWN));

        assertFalse(AiContractUtil.canShift(board, 0, Direction.RIGHT)); // even
        assertFalse(AiContractUtil.canShift(board, 2, Direction.UP));    // even
        assertFalse(AiContractUtil.canShift(board, 7, Direction.LEFT));  // out of bounds
        assertFalse(AiContractUtil.canShift(board, -1, Direction.DOWN)); // out of bounds
        assertFalse(AiContractUtil.canShift(null, 1, Direction.RIGHT));  // null board
        assertFalse(AiContractUtil.canShift(board, 1, null));            // null direction
    }

    /**
     * UT-AICLIENT-09:
     * playerId is called
     * => returns id if PlayerInfo exists, otherwise null.
     */
    @Test
    @DisplayName("UT-AICLIENT-09: playerId returns id when PlayerInfo exists, otherwise null")
    void playerIdExtractsIdOrNull() {
        // given
        PlayerState p = new PlayerState();
        PlayerInfo info = new PlayerInfo();
        info.setId("P-1");
        p.setPlayerInfo(info);

        PlayerState noInfo = new PlayerState();
        noInfo.setPlayerInfo(null);

        // when
        String id = AiContractUtil.playerId(p);
        String idNull1 = AiContractUtil.playerId(noInfo);
        String idNull2 = AiContractUtil.playerId(null);

        // then
        assertEquals("P-1", id);
        assertNull(idNull1);
        assertNull(idNull2);
    }

    /**
     * UT-AICLIENT-10:
     * isHomeTarget is called with null, 0, and other values
     * => true for null/0, otherwise false.
     */
    @Test
    @DisplayName("UT-AICLIENT-10: isHomeTarget returns true for null or 0, otherwise false")
    void isHomeTargetNullOrZeroReturnsTrue() {
        // given / when / then
        assertTrue(AiContractUtil.isHomeTarget(null));
        assertTrue(AiContractUtil.isHomeTarget(0));
        assertFalse(AiContractUtil.isHomeTarget(1));
        assertFalse(AiContractUtil.isHomeTarget(99));
    }

    /**
     * UT-AICLIENT-11:
     * toBonusSet is called with null/empty and with values
     * => returns empty EnumSet or set with all values.
     */
    @Test
    @DisplayName("UT-AICLIENT-11: toBonusSet returns empty set for null/empty and contains all values otherwise")
    void toBonusSetConversion() {
        // given
        BonusType[] empty = new BonusType[0];
        BonusType[] bonuses = new BonusType[]{BonusType.BEAM, BonusType.SWAP};

        // when
        Set<BonusType> sNull = AiContractUtil.toBonusSet(null);
        Set<BonusType> sEmpty = AiContractUtil.toBonusSet(empty);
        Set<BonusType> s = AiContractUtil.toBonusSet(bonuses);

        // then
        assertNotNull(sNull);
        assertTrue(sNull.isEmpty());

        assertNotNull(sEmpty);
        assertTrue(sEmpty.isEmpty());

        assertEquals(2, s.size());
        assertTrue(s.contains(BonusType.BEAM));
        assertTrue(s.contains(BonusType.SWAP));
    }
}
