package mci.dibse.crazylabyrinth.client.ai;


import labyrinth.contracts.models.*;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

public class AiContractUtil {

    public static Coordinates coord(int row, int col) {
        Coordinates c = new Coordinates();
        c.setRow(row);
        c.setColumn(col);
        return c;
    }

    public static boolean hasEntrance(Tile tile, Direction dir) {
        if (tile == null) return false;
        Direction[] e = tile.getEntrances();
        if (e == null) return false;

        for (Direction d : e) {
            if (d == dir) return true;
        }
        return false;
    }

    public static Tile tileAt(GameBoard board, int row, int col) {
        if (board == null) return null;
        Tile[][] tiles = board.getTiles();
        if (tiles == null) return null;
        if (row < 0 || col < 0) return null;
        if (row >= tiles.length) return null;
        if (tiles[row] == null || col >= tiles[row].length) return null;
        return tiles[row][col];
    }

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

    public static Tile cloneTileWithEntrances(Tile base, Direction[] entrances, boolean isFixed) {
        if (base == null) return null;
        Tile t = new Tile();
        t.setEntrances(entrances);
        t.setTreasure(base.getTreasure());
        t.setBonus(base.getBonus());
        t.setIsFixed(isFixed);
        return t;
    }

    // TODO: eventuell in BoardSimulationHelper exportieren?
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

    public static String playerId(PlayerState p) {
        if (p == null || p.getPlayerInfo() == null) return null;
        return p.getPlayerInfo().getId();
    }

    public static boolean isHomeTarget(Treasure t) {
        return t == null || t.getId() == 0;
    }

    public static Set<BonusType> toBonusSet(BonusType[] bonuses) {
        if (bonuses == null || bonuses.length == 0) {
            return EnumSet.noneOf(BonusType.class);
        }
        return EnumSet.copyOf(Arrays.asList(bonuses));
    }


}
