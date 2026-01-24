package mci.dibse.crazylabyrinth.client.ai;

import labyrinth.contracts.models.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

// TODO: JAVA Doc überarbeiten
/**
 * Helper class for board simulation and geometry calculations.
 * Contains pure functions for coordinate transformations, rotations, and board state analysis.
 */
@Component
public class BoardSimulationHelper {

    /**
     * Calculates the new position of a player after a tile push operation.
     *
     * @param currentPos the current position of the player
     * @param pushIndex the index where the push occurs (row or column)
     * @param pushDirection the direction of the push
     * @param rows total number of rows on the board
     * @param cols total number of columns on the board
     * @return the new position after the push, potentially wrapped around
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
     * Calculates the rotated entrances of a tile after applying a number of clockwise rotations.
     *
     * @param originalEntrances the original set of entrances
     * @param rotations the number of 90-degree clockwise rotations to apply
     * @return list of rotated entrances
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
     * Rotates a list of entrances by 90 degrees clockwise.
     *
     * @param entrances the list of entrances to rotate
     * @return the rotated list of entrances
     */
    private List<Direction> rotateEntrancesClockwise(List<Direction> entrances) {
        List<Direction> rotated = new ArrayList<>();
        for (Direction dir : entrances) {
            rotated.add(rotateDirectionClockwise(dir));
        }
        return rotated;
    }

    /**
     * Rotates a single direction by 90 degrees clockwise.
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
     * Finds the position of a specific treasure on the board.
     *
     * @param board the game board
     * @param treasure the treasure to find (can be null if all treasures collected)
     * @return the coordinates of the treasure, or null if not found
     */
    public Coordinates findTreasurePosition(GameBoard board, Treasure treasure) {
        if (treasure == null) {
            return null; // No treasure to find
        }

        Tile[][] tiles = board.getTiles();

        for (int row = 0; row < board.getRows(); row++) {
            for (int col = 0; col < board.getCols(); col++) {
                Tile tile = tiles[row][col];
                if (tile.getTreasure() != null && tile.getTreasure().equals(treasure)) {
                    return AiContractUtil.coord(row, col);
                }
            }
        }

        return null;
    }

    /**
     * Finds the position of Bonus on the board.
     *
     * @param board the game board
     * @return the coordinates of the treasure, or null if not found
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



    // TODO JAVADOC
    public int manhattanDistance(Coordinates a, Coordinates b) {
        return Math.abs(a.getRow() - b.getRow()) + Math.abs(a.getColumn() - b.getColumn());
    }

    /**
     * Returns all valid push indices for the board (odd indices: 1, 3, 5, ...).
     *
     * @param board the game board
     * @return list of valid push indices
     */
    public List<Integer> getValidPushIndices(GameBoard board) {
        List<Integer> indices = new ArrayList<>();
        int size = Math.max(board.getRows(), board.getCols());

        for (int i = 1; i < size; i += 2) {
            indices.add(i);
        }

        return indices;
    }

    public List<Integer> getValidPushFixedIndices(GameBoard board) {
        List<Integer> indices = new ArrayList<>();
        int size = Math.max(board.getRows(), board.getCols());

        for (int i = 2; i < size - 1; i += 2) {
            indices.add(i);
        }
        return indices;
    }


    /**
     * Calculates which tile will be pushed out when a push is executed.
     * This tile becomes the new spare tile that the opponent can use.
     *
     * @param board the game board
     * @param pushIndex the index where the push occurs (row or column number)
     * @param pushDirection the direction of the push
     * @return the tile that will be pushed out (becomes new spare tile)
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
     * Checks if a push would reverse the last push operation.
     * A reverse push is not allowed in Crazy Labyrinth rules.
     *
     * @param board the game board
     * @param pushIndex the index to push at
     * @param pushDirection the direction to push
     * @return true if this push would reverse the last push, false otherwise
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

