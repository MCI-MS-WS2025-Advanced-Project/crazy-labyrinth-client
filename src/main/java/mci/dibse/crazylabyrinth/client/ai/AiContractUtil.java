package mci.dibse.crazylabyrinth.client.ai;


import labyrinth.contracts.models.*;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;


/**
 * Utility helpers for working with contract-layer model objects in the AI client.
 * <p>
 * This class contains small, side-effect free convenience methods for:
 * <ul>
 *   <li>creating and accessing coordinates and tiles</li>
 *   <li>rotating tile entrances</li>
 *   <li>checking board push validity</li>
 *   <li>converting contract data structures into more convenient forms</li>
 * </ul>
 * </p>
 *
 * <p>
 * All methods are static and do not maintain any internal state.
 * </p>
 */
public class AiContractUtil {


    /**
     * Creates a {@link Coordinates} instance with the given row and column.
     *
     * @param row the row index
     * @param col the column index
     * @return a new {@link Coordinates} instance with the given values
     */
    public static Coordinates coord(int row, int col) {
        Coordinates c = new Coordinates();
        c.setRow(row);
        c.setColumn(col);
        return c;
    }

    /**
     * Checks whether the given tile has an entrance in the specified direction.
     *
     * @param tile the tile to check (may be {@code null})
     * @param dir the direction to look for
     * @return {@code true} if the tile has an entrance in this direction, otherwise {@code false}
     */
    public static boolean hasEntrance(Tile tile, Direction dir) {
        if (tile == null) return false;
        Direction[] e = tile.getEntrances();
        if (e == null) return false;

        for (Direction d : e) {
            if (d == dir) return true;
        }
        return false;
    }

    /**
     * Safely returns the tile at the given position of the board.
     * <p>
     * Performs full bounds and null checks and returns {@code null} if the position is invalid
     * or the board data is not available.
     * </p>
     *
     * @param board the game board
     * @param row the row index
     * @param col the column index
     * @return the tile at the given position, or {@code null} if not accessible
     */
    public static Tile tileAt(GameBoard board, int row, int col) {
        if (board == null) return null;
        Tile[][] tiles = board.getTiles();
        if (tiles == null) return null;
        if (row < 0 || col < 0) return null;
        if (row >= tiles.length) return null;
        if (tiles[row] == null || col >= tiles[row].length) return null;
        return tiles[row][col];
    }

    /**
     * Rotates the given entrance directions by the specified number of 90° clockwise rotations.
     * <p>
     * The input array is not modified. A new array instance is returned.
     * Negative and positive rotation counts are normalized to the range {@code 0..3}.
     * </p>
     *
     * @param entrances the original entrance directions
     * @param rotations number of clockwise 90° rotations (may be negative or greater than 3)
     * @return a new array containing the rotated entrance directions, or {@code null} if {@code entrances} is {@code null}
     */
    public static Direction[] rotateEntrances(Direction[] entrances, int rotations) {
        if (entrances == null) return null;

        int r = ((rotations % 4) + 4) % 4;
        if (r == 0) return entrances.clone();

        Direction[] out = new Direction[entrances.length];
        for (int i = 0; i < entrances.length; i++) {
            out[i] = rotateDirectionRight(entrances[i], r);
        }
        return out;
    }

    /**
     * Rotates a single direction by {@code r} times 90° clockwise.
     *
     * @param d the original direction
     * @param r number of clockwise 90° rotations (expected to be normalized to {@code 0..3})
     * @return the rotated direction
     */
    private static Direction rotateDirectionRight(Direction d, int r) {
        Direction res = d;
        for (int i = 0; i < r; i++) {
            res = switch (res) {
                case UP -> Direction.RIGHT;
                case RIGHT -> Direction.DOWN;
                case DOWN -> Direction.LEFT;
                case LEFT -> Direction.UP;
            };
        }
        return res;
    }


    /**
     * Creates a new {@link Tile} instance by copying treasure and bonus from the given base tile
     * and applying the provided entrances and fixed-flag.
     *
     * @param base the base tile to copy treasure and bonus from
     * @param entrances the entrances to set on the new tile
     * @param isFixed whether the new tile should be marked as fixed
     * @return a new tile instance, or {@code null} if {@code base} is {@code null}
     */
    public static Tile cloneTileWithEntrances(Tile base, Direction[] entrances, boolean isFixed) {
        if (base == null) return null;
        Tile t = new Tile();
        t.setEntrances(entrances);
        t.setTreasure(base.getTreasure());
        t.setBonus(base.getBonus());
        t.setIsFixed(isFixed);
        return t;
    }


    /**
     * Checks whether a push (shift) operation is valid at the given index and direction.
     * <p>
     * According to the game rules, only odd indices are allowed and the index must be within
     * the board bounds. LEFT/RIGHT operate on rows, UP/DOWN on columns.
     * </p>
     *
     * @param board the game board
     * @param index the row or column index to push
     * @param direction the push direction
     * @return {@code true} if the shift is allowed, otherwise {@code false}
     */
    public static boolean canShift(GameBoard board, int index, Direction direction) {
        if (board == null || direction == null) return false;

        int rows = board.getRows() != null ? board.getRows() : (board.getTiles() != null ? board.getTiles().length : 0);
        int cols = board.getCols() != null ? board.getCols() : (board.getTiles() != null && board.getTiles().length > 0 ? board.getTiles()[0].length : 0);

        boolean isRowMove = (direction == Direction.LEFT || direction == Direction.RIGHT);
        boolean isColMove = (direction == Direction.UP || direction == Direction.DOWN);

        if (isRowMove) return index >= 0 && index < rows && index % 2 != 0;
        if (isColMove) return index >= 0 && index < cols && index % 2 != 0;
        return false;
    }

    /**
     * Extracts the player id from a {@link PlayerState}.
     *
     * @param p the player state
     * @return the player id, or {@code null} if not available
     */
    public static String playerId(PlayerState p) {
        if (p == null || p.getPlayerInfo() == null) return null;
        return p.getPlayerInfo().getId();
    }


    /**
     * Checks whether the given target value represents the "home" target.
     * <p>
     * In the contract model, {@code null} or {@code 0} indicates that all treasures are collected
     * and the player should return home.
     * </p>
     *
     * @param t the target value
     * @return {@code true} if this represents the home target, otherwise {@code false}
     */
    public static boolean isHomeTarget(Integer t) {
        return t == null || t == 0;
    }

    /**
     * Converts an array of {@link BonusType} values into a {@link Set}.
     *
     * @param bonuses the bonus array (may be {@code null} or empty)
     * @return a set containing all bonus types, or an empty set if {@code bonuses} is {@code null} or empty
     */
    public static Set<BonusType> toBonusSet(BonusType[] bonuses) {
        if (bonuses == null || bonuses.length == 0) {
            return EnumSet.noneOf(BonusType.class);
        }
        return EnumSet.copyOf(Arrays.asList(bonuses));
    }
}
