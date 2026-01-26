package mci.dibse.crazylabyrinth.client.ai;

import labyrinth.contracts.models.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;


/**
 * Helper component providing utility methods for board simulation and lightweight geometry operations.
 *
 * <p>This class contains methods to:
 * <ul>
 *   <li>compute the resulting {@link Coordinates} after a push operation,</li>
 *   <li>rotate tile entrance directions by 90° clockwise steps,</li>
 *   <li>search for treasures/bonuses on a {@link GameBoard},</li>
 *   <li>derive valid push indices,</li>
 *   <li>determine the tile that would be pushed out,</li>
 *   <li>and detect reverse pushes based on {@link GameBoard#getLastPush()}.</li>
 * </ul>
 */
@Component
public class BoardSimulationHelper {

    /**
     * Calculates the new player position after a push operation on the board.
     *
     * <p>The position is shifted by one field if the player's current row/column matches the pushed axis,
     * and wraps around the board edges using modulo arithmetic.
     *
     * @param currentPos the current player position
     * @param pushIndex the row/column index at which the push is executed
     * @param pushDirection the push direction
     * @param rows the total number of rows on the board
     * @param cols the total number of columns on the board
     * @return the updated position after applying the push shift (with wrap-around)
     */
    public Coordinates calculatePositionAfterPush(
            Coordinates currentPos,
            int pushIndex,
            Direction pushDirection,
            int rows,
            int cols
    ) {
        int row = currentPos.getRow();
        int column = currentPos.getColumn();

        switch (pushDirection) {
            case RIGHT:
                if (row == pushIndex) {
                    column = (column + 1) % cols;
                }
                break;
            case LEFT:
                if (row == pushIndex) {
                    column = (column - 1 + cols) % cols;
                }
                break;
            case DOWN:
                if (column == pushIndex) {
                    row = (row + 1) % rows;
                }
                break;
            case UP:
                if (column == pushIndex) {
                    row = (row - 1 + rows) % rows;
                }
                break;
        }

        return AiContractUtil.coord(row, column);
    }

    /**
     * Computes the rotated entrances for a tile by applying a given number of clockwise 90° rotations.
     *
     * <p>If {@code originalEntrances} is {@code null}, an empty array is returned.
     *
     * @param originalEntrances the original entrances
     * @param rotations the number of clockwise 90° rotations to apply
     * @return the entrances after rotation as an array
     */
    public Direction[] calculateRotatedEntrances(
            Direction[] originalEntrances,
            int rotations
    ) {
        if (originalEntrances == null) {
            return new Direction[0];
        }

        List<Direction> entrances = new ArrayList<>(List.of(originalEntrances));

        for (int i = 0; i < rotations; i++) {
            entrances = rotateEntrancesClockwise(entrances);
        }

        return entrances.toArray(Direction[]::new);
    }


    /**
     * Rotates each direction in the given entrance list by 90° clockwise.
     *
     * @param entrances the entrance directions to rotate
     * @return a new list containing the rotated directions
     */
    private List<Direction> rotateEntrancesClockwise(List<Direction> entrances) {
        List<Direction> rotated = new ArrayList<>();
        for (Direction dir : entrances) {
            rotated.add(rotateDirectionClockwise(dir));
        }
        return rotated;
    }


    /**
     * Rotates a single {@link Direction} by 90° clockwise.
     *
     * @param dir the direction to rotate
     * @return the rotated direction
     */
    private Direction rotateDirectionClockwise(Direction dir) {
        return switch (dir) {
            case UP -> Direction.RIGHT;
            case RIGHT -> Direction.DOWN;
            case DOWN -> Direction.LEFT;
            case LEFT -> Direction.UP;
        };
    }

    /**
     * Finds the board position of the tile carrying the given treasure id.
     *
     * <p>If {@code treasure} is {@code null}, this method returns {@code null} without searching.
     *
     * @param board the current game board
     * @param treasure the treasure id to search for; may be {@code null}
     * @return the coordinates of the matching treasure tile, or {@code null} if not found or if {@code treasure} is {@code null}
     */
    public Coordinates findTreasurePosition(GameBoard board, Integer treasure) {
        if (treasure == null) {
            return null; // No treasure to find
        }

        Tile[][] tiles = board.getTiles();

        for (int row = 0; row < board.getRows(); row++) {
            for (int col = 0; col < board.getCols(); col++) {
                Tile tile = tiles[row][col];
                if (tile.getTreasure() != null && tile.getTreasure().getId() == treasure) {
                    return AiContractUtil.coord(row, col);
                }
            }
        }

        return null;
    }

    /**
     * Collects the coordinates of all tiles on the board that currently contain a bonus.
     *
     * @param board the current game board
     * @return a list of coordinates for all tiles with a non-null bonus (may be empty)
     */
    public List<Coordinates> findBonusPositions(GameBoard board) {
        List<Coordinates> result = new ArrayList<>();
        Tile[][] tiles = board.getTiles();

        for (int row = 0; row < board.getRows(); row++) {
            for (int col = 0; col < board.getCols(); col++) {
                Tile tile = tiles[row][col];
                if (tile != null && tile.getBonus() != null) {
                    result.add(AiContractUtil.coord(row, col));
                }
            }
        }
        return result;
    }



    /**
     * Computes the Manhattan distance between two coordinates.
     *
     * @param a first coordinate
     * @param b second coordinate
     * @return {@code |a.row - b.row| + |a.col - b.col|}
     */
    public int manhattanDistance(Coordinates a, Coordinates b) {
        return Math.abs(a.getRow() - b.getRow()) + Math.abs(a.getColumn() - b.getColumn());
    }


    /**
     * Returns the list of push indices considered valid by this helper.
     *
     * <p>The indices are generated as odd numbers starting at 1 up to (but excluding) the maximum of
     * {@link GameBoard#getRows()} and {@link GameBoard#getCols()}.
     *
     * @param board the game board
     * @return a list of valid push indices (may be empty)
     */
    public List<Integer> getValidPushIndices(GameBoard board) {
        List<Integer> indices = new ArrayList<>();
        int size = Math.max(board.getRows(), board.getCols());

        for (int i = 1; i < size; i += 2) {
            indices.add(i);
        }

        return indices;
    }


    /**
     * Returns the list of push indices considered valid for "fixed" pushes by this helper.
     *
     * <p>The indices are generated as even numbers starting at 2 up to {@code size - 1}, where
     * {@code size} is {@code max(rows, cols)}.
     *
     * @param board the game board
     * @return a list of valid indices for fixed pushes (may be empty)
     */
    public List<Integer> getValidPushFixedIndices(GameBoard board) {
        List<Integer> indices = new ArrayList<>();
        int size = Math.max(board.getRows(), board.getCols());

        for (int i = 2; i < size - 1; i += 2) {
            indices.add(i);
        }
        return indices;
    }


    /**
     * Determines which tile would be pushed out of the board for the given push action.
     *
     * <p>The returned tile corresponds to the edge tile on the pushed row/column that is shifted out
     * in the given {@link Direction}.
     *
     * @param board the current game board
     * @param pushIndex the row/column index at which the push is executed
     * @param pushDirection the push direction
     * @return the tile that would be pushed out, or {@code null} if {@code pushDirection} is not handled
     */
    public Tile getTilePushedOut(GameBoard board, int pushIndex, Direction pushDirection) {
        Tile[][] tiles = board.getTiles();
        int rows = board.getRows();
        int cols = board.getCols();

        switch (pushDirection) {
            case RIGHT:
                return tiles[pushIndex][cols - 1];

            case LEFT:
                return tiles[pushIndex][0];

            case DOWN:
                return tiles[rows - 1][pushIndex];

            case UP:
                return tiles[0][pushIndex];

            default:
                return null;
        }
    }


    /**
     * Determines which tile would be pushed out of the board for the given push action.
     *
     * <p>The returned tile corresponds to the edge tile on the pushed row/column that is shifted out
     * in the given {@link Direction}.
     *
     * @param board the current game board
     * @param pushIndex the row/column index at which the push is executed
     * @param pushDirection the push direction
     * @return the tile that would be pushed out, or {@code null} if {@code pushDirection} is not handled
     */
    public boolean isReversePush(GameBoard board, int pushIndex, Direction pushDirection) {
        if (board.getLastPush() == null) {
            return false; // First move in the game
        }

        int lastIndex = board.getLastPush().getRowOrColIndex();
        Direction lastDirection = board.getLastPush().getDirection();

        if (pushIndex != lastIndex) {
            return false;
        }

        // Check for opposite directions
        return (pushDirection == Direction.LEFT && lastDirection == Direction.RIGHT) ||
                (pushDirection == Direction.RIGHT && lastDirection == Direction.LEFT) ||
                (pushDirection == Direction.UP && lastDirection == Direction.DOWN) ||
                (pushDirection == Direction.DOWN && lastDirection == Direction.UP);
    }



}

