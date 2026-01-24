package mci.dibse.crazylabyrinth.client.ai;

import java.util.*;
//import mci.dibse.crazylabyrinth.gameserver.domain.model.Board;

import labyrinth.contracts.models.GameStateEventPayload;
import labyrinth.contracts.models.PlayerState;
import labyrinth.contracts.models.PlayerInfo;
import labyrinth.contracts.models.TurnState;
import labyrinth.contracts.models.GameBoard;
import labyrinth.contracts.models.Coordinates;
import labyrinth.contracts.models.Direction;
import labyrinth.contracts.models.BonusType;
import labyrinth.contracts.models.Tile;

// TODO: JAVA DOC überarbeiten
/**
 * Domain service for pathfinding operations on the game board.
 * <p>
 * Uses the A* algorithm to determine if a valid path exists between two coordinates,
 * considering tile entrances as constraints. The Manhattan distance heuristic provides
 * efficient pathfinding on typical game board sizes.
 * </p>
 * <p>
 * This is a pure domain service with no external dependencies, instantiated directly by
 * the GameFactory to keep all game logic concerns within the domain layer.
 * </p>
 */
public class AiPathFindingService {

    /**
     * Internal node class for the A* pathfinding algorithm.
     * Represents a board position with associated cost values used by the algorithm.
     */
    private static class Node {
        /**
         * The board coordinates of this node.
         */
        Coordinates coordinates;

        /**
         * The actual movement cost from the start position to this node.
         */
        int gScore;

        /**
         * The estimated total cost (gScore + heuristic) from start to goal through this node.
         */
        int fScore;

        /**
         * Constructs a Node with the specified coordinates and cost values.
         * @param coordinates the board position
         * @param gScore the actual cost from start
         * @param fScore the estimated total cost
         */
        Node(Coordinates coordinates, int gScore, int fScore) {
            this.coordinates = coordinates;
            this.gScore = gScore;
            this.fScore = fScore;
        }
    }

    /**
     * Checks if a valid path exists between two board coordinates.
     * <p>
     * Returns immediately if the start and target are the same position.
     * Validates that both positions are within board bounds.
     * </p>
     * @param board the game board to search
     * @param start the starting coordinates
     * @param target the target coordinates
     * @return true if a valid path exists from start to target, false otherwise
     */
    public boolean isPathValid(GameBoard board, Coordinates start, Coordinates target) {
        if (board == null || start == null || target == null) {
            return false;
        }
        if (start.equals(target)) {
            return true;
        }
        Tile[][] tiles = board.getTiles();
        int rows = board.getRows();
        int cols = board.getCols();
        if (isValidPosition(start, rows, cols) || isValidPosition(target, rows, cols)) {
            return false;
        }
        return aStar(tiles, start, target, rows, cols) != null;
    }

    /**
     * Finds the shortest path between two board coordinates.
     * <p>
     * Returns a list of coordinates representing the path from start to target,
     * or an empty list if no path exists. Returns a single-element list if start equals target.
     * </p>
     * @param board the game board to search
     * @param start the starting coordinates
     * @param target the target coordinates
     * @return a list of coordinates representing the path, or an empty list if no path exists
     */
    public List<Coordinates> findPath(GameBoard board, Coordinates start, Coordinates target) {
        if (board == null || start == null || target == null) {
            return Collections.emptyList();
        }
        if (start.equals(target)) {
            return Collections.singletonList(start);
        }
        Tile[][] tiles = board.getTiles();
        int rows = board.getRows();
        int cols = board.getCols();
        if (isValidPosition(start, rows, cols) || isValidPosition(target, rows, cols)) {
            return Collections.emptyList();
        }
        List<Coordinates> path = aStar(tiles, start, target, rows, cols);
        return path != null ? path : Collections.emptyList();
    }

    /**
     * Implements the A* pathfinding algorithm for finding the shortest path on the board.
     * <p>
     * Uses Manhattan distance as the heuristic function for efficiency.
     * Considers tile entrances when determining valid moves between adjacent tiles.
     * </p>
     * @param tiles the board tiles
     * @param start the starting coordinates
     * @param target the goal coordinates
     * @param rows the number of rows on the board
     * @param cols the number of columns on the board
     * @return a list of coordinates representing the path, or null if no path exists
     */
    private List<Coordinates> aStar(
            Tile[][] tiles, Coordinates start, Coordinates target, int rows, int cols) {
        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingInt(n -> n.fScore));
        Map<Coordinates, Coordinates> cameFrom = new HashMap<>();
        Map<Coordinates, Integer> gScore = new HashMap<>();
        Set<Coordinates> closedSet = new HashSet<>();

        int h = manhattanDistance(start, target);
        openSet.offer(new Node(start, 0, h));
        gScore.put(start, 0);
        while (!openSet.isEmpty()) {
            Node current = openSet.poll();
            Coordinates currentcoordinates = current.coordinates;
            if (currentcoordinates.equals(target)) {
                return reconstructPath(cameFrom, target);
            }
            if (closedSet.contains(currentcoordinates)) {
                continue;
            }
            closedSet.add(currentcoordinates);
            int currentGScore = gScore.get(currentcoordinates);
            for (Coordinates neighbor : getValidNeighbors(tiles, currentcoordinates, rows, cols)) {
                if (closedSet.contains(neighbor)) {
                    continue;
                }
                int tentativeGScore = currentGScore + 1;
                int neighborGScore = gScore.getOrDefault(neighbor, Integer.MAX_VALUE);
                if (tentativeGScore < neighborGScore) {
                    cameFrom.put(neighbor, currentcoordinates);
                    gScore.put(neighbor, tentativeGScore);
                    int fScore = tentativeGScore + manhattanDistance(neighbor, target);
                    openSet.offer(new Node(neighbor, tentativeGScore, fScore));
                }
            }
        }
        return null;
    }

    /**
     * Reconstructs the path from the cameFrom map.
     * @param cameFrom map of coordinates and their predecessors
     * @param target the target coordinates
     * @return list of coordinates representing the path
     */
    private List<Coordinates> reconstructPath(
            Map<Coordinates, Coordinates> cameFrom, Coordinates target) {
        List<Coordinates> path = new ArrayList<>();
        Coordinates current = target;
        while (current != null) {
            path.add(current);
            current = cameFrom.get(current);
        }
        Collections.reverse(path);
        return path;
    }

    /**
     * Gets all valid neighboring coordinates that can be reached from the current position.
     * Considers tile entrances to determine if movement is possible.
     * @param tiles the board tiles
     * @param current the current coordinates
     * @param rows number of rows in the board
     * @param cols number of columns in the board
     * @return list of valid neighboring coordinates
     */
    private List<Coordinates> getValidNeighbors(
            Tile[][] tiles, Coordinates current, int rows, int cols) {
        List<Coordinates> neighbors = new ArrayList<>();
        int column = current.getColumn();
        int row = current.getRow();
        Tile currentTile = tiles[row][column];
        if (currentTile == null || currentTile.getEntrances() == null) {
            return neighbors;
        }

        if (AiContractUtil.hasEntrance(currentTile, Direction.UP) && row > 0) {
            Tile neighborTile = tiles[row - 1][column];
            if (AiContractUtil.hasEntrance(neighborTile, Direction.DOWN)) {
                neighbors.add(AiContractUtil.coord(row - 1, column));
            }
        }

        if (AiContractUtil.hasEntrance(currentTile, Direction.DOWN) && row < rows - 1) {
            Tile neighborTile = tiles[row + 1][column];
            if (AiContractUtil.hasEntrance(neighborTile, Direction.UP)) {
                neighbors.add(AiContractUtil.coord(row + 1, column));
            }
        }

        if (AiContractUtil.hasEntrance(currentTile, Direction.LEFT) && column > 0) {
            Tile neighborTile = tiles[row][column - 1];
            if (AiContractUtil.hasEntrance(neighborTile, Direction.RIGHT)) {
                neighbors.add(AiContractUtil.coord(row, column - 1));
            }
        }

        if (AiContractUtil.hasEntrance(currentTile, Direction.RIGHT) && column < cols - 1) {
            Tile neighborTile = tiles[row][column + 1];
            if (AiContractUtil.hasEntrance(neighborTile, Direction.LEFT)) {
                neighbors.add(AiContractUtil.coord(row, column + 1));
            }
        }
        return neighbors;
    }

    /**
     * Checks if coordinates are within board bounds.
     * @param coordinates the coordinates to check
     * @param rows number of rows in the board
     * @param cols number of columns in the board
     * @return true if out of bounds, false otherwise
     */
    private boolean isValidPosition(Coordinates coordinates, int rows, int cols) {
        return coordinates.getColumn() < 0
                || coordinates.getColumn() >= cols
                || coordinates.getRow() < 0
                || coordinates.getRow() >= rows;
    }

    /**
     * Calculates the Manhattan distance between two coordinates.
     * @param a first coordinate
     * @param b second coordinate
     * @return Manhattan distance (sum of absolute differences in x and y)
     */
    private int manhattanDistance(Coordinates a, Coordinates b) {
        return Math.abs(a.getColumn() - b.getColumn()) + Math.abs(a.getRow() - b.getRow());
    }


    /**
     * Finds path after virtual push with a complete virtual spare tile.
     * Use this when you already have the rotated spare tile constructed.
     */
    public List<Coordinates> findPathAfterVirtualPushWithTile(
            GameBoard board,
            int pushIndex,
            Direction pushDirection,
            Tile virtualSpareTile,
            Coordinates start,
            Coordinates target
    ) {
        if (board == null || start == null || target == null) {
            return Collections.emptyList();
        }
        if (start.equals(target)) {
            return Collections.singletonList(start);
        }

        return aStarVirtual(board, pushIndex, pushDirection, virtualSpareTile, start, target);
    }


    /**
     * Finds path on a virtual board state after a simulated push.
     * Does not modify the actual board.
     */
    public List<Coordinates> findPathAfterVirtualPush(
            GameBoard board,
            int pushIndex,
            Direction pushDirection,
            Direction[] spareTileEntrances,
            Coordinates start,
            Coordinates target
    ) {
        if (board == null || start == null || target == null) {
            return Collections.emptyList();
        }
        if (start.equals(target)) {
            return Collections.singletonList(start);
        }

        Tile realSpare = board.getSpareTile();
        if(realSpare == null) return Collections.emptyList();

        // create virtual Tile
        Tile virtualSpareTile = new Tile();
        virtualSpareTile.setEntrances(spareTileEntrances);
        virtualSpareTile.setTreasure(realSpare.getTreasure());
        virtualSpareTile.setBonus(realSpare.getBonus());
        virtualSpareTile.setIsFixed(Boolean.FALSE);

        return aStarVirtual(board, pushIndex, pushDirection, virtualSpareTile, start, target);
    }

    private List<Coordinates> aStarVirtual(
            GameBoard board,
            int pushIndex,
            Direction pushDirection,
            Tile rotatedSpareTile,
            Coordinates start,
            Coordinates target
    ) {
        int rows = board.getRows();
        int cols = board.getCols();

        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingInt(n -> n.fScore));
        Map<Coordinates, Coordinates> cameFrom = new HashMap<>();
        Map<Coordinates, Integer> gScore = new HashMap<>();
        Set<Coordinates> closedSet = new HashSet<>();

        int h = manhattanDistance(start, target);
        openSet.offer(new Node(start, 0, h));
        gScore.put(start, 0);

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();
            Coordinates currentCoords = current.coordinates;

            if (currentCoords.equals(target)) {
                return reconstructPath(cameFrom, target);
            }

            if (closedSet.contains(currentCoords)) {
                continue;
            }
            closedSet.add(currentCoords);

            int currentGScore = gScore.get(currentCoords);

            for (Coordinates neighbor : getValidNeighborsVirtual(
                    board, pushIndex, pushDirection, rotatedSpareTile, currentCoords, rows, cols)) {

                if (closedSet.contains(neighbor)) {
                    continue;
                }

                int tentativeGScore = currentGScore + 1;
                int neighborGScore = gScore.getOrDefault(neighbor, Integer.MAX_VALUE);

                if (tentativeGScore < neighborGScore) {
                    cameFrom.put(neighbor, currentCoords);
                    gScore.put(neighbor, tentativeGScore);
                    int fScore = tentativeGScore + manhattanDistance(neighbor, target);
                    openSet.offer(new Node(neighbor, tentativeGScore, fScore));
                }
            }
        }

        return Collections.emptyList();
    }

    private List<Coordinates> getValidNeighborsVirtual(
            GameBoard board,
            int pushIndex,
            Direction pushDirection,
            Tile rotatedSpareTile,
            Coordinates current,
            int rows,
            int cols
    ) {
        List<Coordinates> neighbors = new ArrayList<>();
        int column = current.getColumn();
        int row = current.getRow();

        Tile currentTile = getVirtualTileAt(board, pushIndex, pushDirection, rotatedSpareTile, row, column);
        if (currentTile == null || currentTile.getEntrances() == null) {
            return neighbors;
        }

        // UP
        if (AiContractUtil.hasEntrance(currentTile, Direction.UP) && row > 0) {
            Tile neighborTile = getVirtualTileAt(board, pushIndex, pushDirection, rotatedSpareTile, row - 1, column);
            if (AiContractUtil.hasEntrance(neighborTile, Direction.DOWN)) {
                neighbors.add(AiContractUtil.coord(row - 1, column));
            }
        }

        // DOWN
        if (AiContractUtil.hasEntrance(currentTile, Direction.DOWN) && row < rows - 1) {
            Tile neighborTile = getVirtualTileAt(board, pushIndex, pushDirection, rotatedSpareTile, row + 1, column);
            if (AiContractUtil.hasEntrance(neighborTile, Direction.UP)) {
                neighbors.add(AiContractUtil.coord(row + 1, column));
            }
        }

        // LEFT
        if (AiContractUtil.hasEntrance(currentTile, Direction.LEFT) && column > 0) {
            Tile neighborTile = getVirtualTileAt(board, pushIndex, pushDirection, rotatedSpareTile, row, column - 1);
            if (AiContractUtil.hasEntrance(neighborTile, Direction.RIGHT)) {
                neighbors.add(AiContractUtil.coord(row, column - 1));
            }
        }

        // RIGHT
        if (AiContractUtil.hasEntrance(currentTile, Direction.RIGHT) && column < cols - 1) {
            Tile neighborTile = getVirtualTileAt(board, pushIndex, pushDirection, rotatedSpareTile, row, column + 1);
            if (AiContractUtil.hasEntrance(neighborTile, Direction.LEFT)) {
                neighbors.add(AiContractUtil.coord(row, column + 1));
            }
        }

        return neighbors;
    }


    private Tile getVirtualTileAt(
            GameBoard board,
            int pushIndex,
            Direction pushDirection,
            Tile rotatedSpareTile,
            int row,
            int column
    ) {
        int rows = board.getRows();
        int cols = board.getCols();

        switch (pushDirection) {
            case RIGHT:
                if (row == pushIndex) {
                    if (column == 0) return rotatedSpareTile;
                    return AiContractUtil.tileAt(board, row, column - 1);
                }
                break;
            case LEFT:
                if (row == pushIndex) {
                    if (column == cols - 1) return rotatedSpareTile;
                    return AiContractUtil.tileAt(board, row, column + 1);
                }
                break;
            case DOWN:
                if (column == pushIndex) {
                    if (row == 0) return rotatedSpareTile;
                    return AiContractUtil.tileAt(board, row - 1, column);
                }
                break;
            case UP:
                if (column == pushIndex) {
                    if (row == rows - 1) return rotatedSpareTile;
                    return AiContractUtil.tileAt(board,row + 1, column);
                }
                break;
        }

        return AiContractUtil.tileAt(board, row, column);
    }

    /**
     * Finds path after TWO consecutive virtual pushes.
     * Simulates: Push1 → Board state changes → Push2 → Board state changes → Find path
     *
     * @param board the original board state
     * @param push1Index first push index
     * @param push1Direction first push direction
     * @param push1Rotations number of 90-degree clockwise rotations for first push (0-3)
     * @param push2Index second push index
     * @param push2Direction second push direction
     * @param push2Rotations number of 90-degree clockwise rotations for second push (0-3)
     * @param start starting position for pathfinding (after both pushes)
     * @param target goal position
     * @return path from start to target after both pushes, or empty list if no path exists
     */
    public List<Coordinates> findPathAfterTwoPushes(
            GameBoard board,
            int push1Index,
            Direction push1Direction,
            int push1Rotations,
            int push2Index,
            Direction push2Direction,
            int push2Rotations,
            Coordinates start,
            Coordinates target) {


        if (board == null || start == null || target == null) {
            return Collections.emptyList();
        }
        if (start.equals(target)) {

            return Collections.singletonList(start);
        }

        Tile spare1 = board.getSpareTile();
        if (spare1 == null) return Collections.emptyList();

        Direction[] rotatedEntrances1 = AiContractUtil.rotateEntrances(spare1.getEntrances(), push1Rotations);
        Tile virtualSpareTile1 = AiContractUtil.cloneTileWithEntrances(spare1, rotatedEntrances1, false);

        // tile that gets pushed out after push1
        Tile spareTileAfterPush1 = getTilePushedOut(board, push1Index, push1Direction);
        if (spareTileAfterPush1 == null) return Collections.emptyList();

        Direction[] rotatedEntrances2 = AiContractUtil.rotateEntrances(spareTileAfterPush1.getEntrances(), push2Rotations);
        Tile virtualSpareTile2 = AiContractUtil.cloneTileWithEntrances(spareTileAfterPush1, rotatedEntrances2, false);

        return aStarAfterTwoPushes(
                board,
                push1Index, push1Direction, virtualSpareTile1,
                push2Index, push2Direction, virtualSpareTile2,
                start, target
        );
    }

    // TODO: JAVADOC
    public Set<Coordinates> getReachablePositionsAfterVirtualPush(
            GameBoard board,
            int pushIndex,
            Direction pushDirection,
            Direction[] spareTileEntrances,
            Coordinates start
    ) {
        if (board == null || start == null) {
            return Collections.emptySet();
        }

        Tile spare = board.getSpareTile();
        if (spare == null) return Collections.emptySet();

        Tile virtualSpareTile = AiContractUtil.cloneTileWithEntrances(spare, spareTileEntrances, false);

        int rows = board.getRows();
        int cols = board.getCols();

        Set<Coordinates> visited = new HashSet<>();
        ArrayDeque<Coordinates> q = new ArrayDeque<>();

        visited.add(start);
        q.add(start);

        while (!q.isEmpty()) {
            Coordinates cur = q.poll();

            for (Coordinates n : getValidNeighborsVirtual(
                    board, pushIndex, pushDirection, virtualSpareTile, cur, rows, cols
            )) {
                if (visited.add(n)) {
                    q.add(n);
                }
            }
        }

        return visited;
    }


    /**
     * Rotates a set of entrances by a given number of 90-degree clockwise rotations.
     *
     * @param originalEntrances the original set of entrances
     * @param rotations number of 90-degree clockwise rotations (0-3)
     * @return list of rotated entrances
     */
    private List<Direction> rotateEntrances(Set<Direction> originalEntrances, int rotations) {
        List<Direction> result = new ArrayList<>(originalEntrances);

        // Normalize rotations to 0-3 range
        rotations = rotations % 4;
        if (rotations < 0) {
            rotations += 4;
        }

        // Apply rotations
        for (int i = 0; i < rotations; i++) {
            List<Direction> temp = new ArrayList<>();
            for (Direction dir : result) {
                temp.add(rotateDirectionClockwise(dir));
            }
            result = temp;
        }

        return result;
    }

    /**
     * Rotates a direction 90 degrees clockwise.
     *
     * @param dir the direction to rotate
     * @return the rotated direction
     */
    private Direction rotateDirectionClockwise(Direction dir) {
        switch (dir) {
            case UP:
                return Direction.RIGHT;
            case RIGHT:
                return Direction.DOWN;
            case DOWN:
                return Direction.LEFT;
            case LEFT:
                return Direction.UP;
            default:
                throw new IllegalArgumentException("Invalid direction: " + dir);
        }
    }

    /**
     * Gets the tile that will be pushed out of the board during a push operation.
     * This tile becomes the new spare tile.
     */
    private Tile getTilePushedOut(GameBoard board, int pushIndex, Direction pushDirection) {
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
                throw new IllegalArgumentException("Invalid push direction: " + pushDirection);
        }
    }

    /**
     * A* algorithm that considers TWO virtual pushes.
     */
    private List<Coordinates> aStarAfterTwoPushes(
            GameBoard board,
            int push1Index,
            Direction push1Direction,
            Tile virtualSpareTile1,
            int push2Index,
            Direction push2Direction,
            Tile virtualSpareTile2,
            Coordinates start,
            Coordinates target) {

        int rows = board.getRows();
        int cols = board.getCols();

        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingInt(n -> n.fScore));
        Map<Coordinates, Coordinates> cameFrom = new HashMap<>();
        Map<Coordinates, Integer> gScore = new HashMap<>();
        Set<Coordinates> closedSet = new HashSet<>();

        int h = manhattanDistance(start, target);
        openSet.offer(new Node(start, 0, h));
        gScore.put(start, 0);

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();
            Coordinates currentCoords = current.coordinates;

            if (currentCoords.equals(target)) {
                return reconstructPath(cameFrom, target);
            }

            if (closedSet.contains(currentCoords)) {
                continue;
            }
            closedSet.add(currentCoords);

            int currentGScore = gScore.get(currentCoords);

            // Get neighbors considering both virtual pushes
            for (Coordinates neighbor : getValidNeighborsAfterTwoPushes(
                    board,
                    push1Index, push1Direction, virtualSpareTile1,
                    push2Index, push2Direction, virtualSpareTile2,
                    currentCoords, rows, cols)) {

                if (closedSet.contains(neighbor)) {
                    continue;
                }

                int tentativeGScore = currentGScore + 1;
                int neighborGScore = gScore.getOrDefault(neighbor, Integer.MAX_VALUE);

                if (tentativeGScore < neighborGScore) {
                    cameFrom.put(neighbor, currentCoords);
                    gScore.put(neighbor, tentativeGScore);
                    int fScore = tentativeGScore + manhattanDistance(neighbor, target);
                    openSet.offer(new Node(neighbor, tentativeGScore, fScore));
                }
            }
        }

        return Collections.emptyList();
    }

    /**
     * Gets valid neighbors considering TWO virtual pushes.
     */
    private List<Coordinates> getValidNeighborsAfterTwoPushes(
            GameBoard board,
            int push1Index,
            Direction push1Direction,
            Tile virtualSpareTile1,
            int push2Index,
            Direction push2Direction,
            Tile virtualSpareTile2,
            Coordinates current,
            int rows,
            int cols) {

        List<Coordinates> neighbors = new ArrayList<>();
        int column = current.getColumn();
        int row = current.getRow();

        // Get the tile at current position after both pushes
        Tile currentTile = getVirtualTileAfterTwoPushes(
                board,
                push1Index, push1Direction, virtualSpareTile1,
                push2Index, push2Direction, virtualSpareTile2,
                row, column
        );

        if (currentTile == null || currentTile.getEntrances() == null) {
            return neighbors;
        }

        // UP
        if (AiContractUtil.hasEntrance(currentTile, Direction.UP) && row > 0) {
            Tile neighborTile = getVirtualTileAfterTwoPushes(
                    board,
                    push1Index, push1Direction, virtualSpareTile1,
                    push2Index, push2Direction, virtualSpareTile2,
                    row - 1, column
            );
            if (AiContractUtil.hasEntrance(neighborTile, Direction.DOWN)) {
                neighbors.add(AiContractUtil.coord(row - 1, column));
            }
        }

        // DOWN
        if (AiContractUtil.hasEntrance(currentTile, Direction.DOWN) && row < rows - 1) {
            Tile neighborTile = getVirtualTileAfterTwoPushes(
                    board,
                    push1Index, push1Direction, virtualSpareTile1,
                    push2Index, push2Direction, virtualSpareTile2,
                    row + 1, column
            );
            if (AiContractUtil.hasEntrance(neighborTile, Direction.UP)) {
                neighbors.add(AiContractUtil.coord(row + 1, column));
            }
        }

        // LEFT
        if (AiContractUtil.hasEntrance(currentTile, Direction.LEFT) && column > 0) {
            Tile neighborTile = getVirtualTileAfterTwoPushes(
                    board,
                    push1Index, push1Direction, virtualSpareTile1,
                    push2Index, push2Direction, virtualSpareTile2,
                    row, column - 1
            );
            if (AiContractUtil.hasEntrance(neighborTile, Direction.RIGHT)) {
                neighbors.add(AiContractUtil.coord(row, column - 1));
            }
        }

        // RIGHT
        if (AiContractUtil.hasEntrance(currentTile, Direction.RIGHT) && column < cols - 1) {
            Tile neighborTile = getVirtualTileAfterTwoPushes(
                    board,
                    push1Index, push1Direction, virtualSpareTile1,
                    push2Index, push2Direction, virtualSpareTile2,
                    row, column + 1
            );
            if (AiContractUtil.hasEntrance(neighborTile, Direction.LEFT)) {
                neighbors.add(AiContractUtil.coord(row, column + 1));
            }
        }

        return neighbors;
    }


    //TODO: JAVA DOC
    private Tile getVirtualTileAfterTwoPushes(
            GameBoard board,
            int push1Index,
            Direction push1Direction,
            Tile virtualSpareTile1,
            int push2Index,
            Direction push2Direction,
            Tile virtualSpareTile2,
            int row,
            int column
    ) {
        int rows = board.getRows();
        int cols = board.getCols();

        if (isInsertionPoint(row, column, push2Index, push2Direction, rows, cols)) {
            return virtualSpareTile2;
        }

        Coordinates beforePush2 = AiContractUtil.coord(row, column);

        if (isAffectedByPush(row, column, push2Index, push2Direction)) {
            beforePush2 = getSourceBeforePush(row, column, push2Index, push2Direction, rows, cols);
        }


        if (isInsertionPoint(beforePush2.getRow(), beforePush2.getColumn(), push1Index, push1Direction, rows, cols)) {
            return virtualSpareTile1;
        }

        Coordinates beforePush1 = beforePush2;

        if (isAffectedByPush(beforePush2.getRow(), beforePush2.getColumn(), push1Index, push1Direction)) {
            beforePush1 = getSourceBeforePush(
                    beforePush2.getRow(), beforePush2.getColumn(),
                    push1Index, push1Direction, rows, cols
            );
        }

        return AiContractUtil.tileAt(board, beforePush1.getRow(), beforePush1.getColumn());
    }


    /**
     * Checks if a position is affected by a given push.
     */
    private boolean isAffectedByPush(int row, int column, int pushIndex, Direction pushDirection) {
        switch (pushDirection) {
            case RIGHT:
            case LEFT:
                return row == pushIndex;
            case DOWN:
            case UP:
                return column == pushIndex;
        }
        return false;
    }

    /**
     * Checks if a position is where the spare tile gets inserted.
     */
    private boolean isInsertionPoint(
            int row, int column,
            int pushIndex,
            Direction pushDirection,
            int rows, int cols) {

        switch (pushDirection) {
            case RIGHT:
                return row == pushIndex && column == 0;
            case LEFT:
                return row == pushIndex && column == cols - 1;
            case DOWN:
                return column == pushIndex && row == 0;
            case UP:
                return column == pushIndex && row == rows - 1;
        }
        return false;
    }

    /**
     * Gets the source coordinates before a push (inverse of push operation).
     */
    private Coordinates getSourceBeforePush(
            int row, int column,
            int pushIndex,
            Direction pushDirection,
            int rows, int cols) {

        switch (pushDirection) {
            case RIGHT:
                if (row == pushIndex) {
                    return AiContractUtil.coord(row,(column - 1 + cols) % cols);
                }
                break;
            case LEFT:
                if (row == pushIndex) {
                    return AiContractUtil.coord(row,(column + 1) % cols);
                }
                break;
            case DOWN:
                if (column == pushIndex) {
                    return AiContractUtil.coord( (row - 1 + rows) % rows, column);
                }
                break;
            case UP:
                if (column == pushIndex) {
                    return AiContractUtil.coord((row + 1) % rows, column);
                }
                break;
        }
        return AiContractUtil.coord(row, column);
    }


}
