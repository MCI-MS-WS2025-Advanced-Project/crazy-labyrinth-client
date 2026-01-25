package mci.dibse.crazylabyrinth.client.ai;

import java.util.*;

import labyrinth.contracts.models.GameBoard;
import labyrinth.contracts.models.Coordinates;
import labyrinth.contracts.models.Direction;

import labyrinth.contracts.models.Tile;

/**
 * Pathfinding service for the client-side AI on a {@link GameBoard}.
 * <p>
 * Provides A* based path search between two coordinates while respecting tile entrances.
 * In addition to normal pathfinding, the service supports path search on <em>virtual</em> board states
 * created by simulated pushes (including two consecutive pushes).
 * </p>
 *
 * <p>
 * The service is self-contained (no external dependencies) and does not modify the given board.
 * Virtual operations are implemented by resolving tiles through coordinate mapping rather than mutating data.
 * </p>
 */
public class AiPathFindingService {

    /**
     * Internal node representation used by the A* algorithm.
     * Stores the coordinate as well as the gScore (cost from start) and fScore (gScore + heuristic).
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
         * Creates a new A* node.
         *
         * @param coordinates board coordinate of this node
         * @param gScore movement cost from start to this node
         * @param fScore total estimated cost from start to target through this node
         */
        Node(Coordinates coordinates, int gScore, int fScore) {
            this.coordinates = coordinates;
            this.gScore = gScore;
            this.fScore = fScore;
        }
    }


    /**
     * Finds a shortest path between {@code start} and {@code target} on the current board.
     * <p>
     * Movement is only allowed if the current tile has an entrance towards the neighbor and the neighbor tile
     * has the reciprocal entrance back.
     * </p>
     *
     * <p>
     * Returns an empty list on invalid input or if no path exists. If {@code start.equals(target)},
     * a single-element list is returned.
     * </p>
     *
     * @param board the board to search on
     * @param start starting coordinate
     * @param target target coordinate
     * @return list of coordinates from start to target (inclusive), or an empty list if unreachable
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
     * A* search implementation for the current board state.
     * <p>
     * Uses Manhattan distance as heuristic and treats each move as unit cost.
     * Neighbor expansion respects tile entrances.
     * </p>
     *
     * @param tiles the board tile matrix
     * @param start starting coordinate
     * @param target target coordinate
     * @param rows number of board rows
     * @param cols number of board columns
     * @return path from start to target, or {@code null} if no path exists
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
     * Reconstructs the path by following the predecessor links stored in {@code cameFrom}.
     *
     * @param cameFrom mapping of each visited coordinate to its predecessor
     * @param target the goal coordinate
     * @return reconstructed path from start to target (inclusive)
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
     * Computes all reachable neighbor coordinates from {@code current} on the current board.
     * <p>
     * A neighbor is valid if entrances are reciprocal between the two adjacent tiles.
     * </p>
     *
     * @param tiles the board tile matrix
     * @param current current coordinate
     * @param rows number of board rows
     * @param cols number of board columns
     * @return list of valid neighbor coordinates (may be empty)
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
     * Checks whether the given coordinate is outside the board bounds.
     *
     * @param coordinates coordinate to check
     * @param rows number of board rows
     * @param cols number of board columns
     * @return {@code true} if the coordinate is out of bounds, otherwise {@code false}
     */
    private boolean isValidPosition(Coordinates coordinates, int rows, int cols) {
        return coordinates.getColumn() < 0
                || coordinates.getColumn() >= cols
                || coordinates.getRow() < 0
                || coordinates.getRow() >= rows;
    }

    /**
     * Computes the Manhattan distance between two coordinates.
     *
     * @param a first coordinate
     * @param b second coordinate
     * @return {@code |dx| + |dy|}
     */
    private int manhattanDistance(Coordinates a, Coordinates b) {
        return Math.abs(a.getColumn() - b.getColumn()) + Math.abs(a.getRow() - b.getRow());
    }


    /**
     * Finds a path after applying a <em>virtual</em> push, using a fully constructed virtual spare tile.
     * <p>
     * This method does not modify {@link GameBoard}. Instead, it resolves the tile layout on-the-fly by treating
     * {@code virtualSpareTile} as inserted at {@code pushIndex}/{@code pushDirection}.
     * </p>
     *
     * @param board the original board state (not modified)
     * @param pushIndex the push index (row for LEFT/RIGHT, column for UP/DOWN)
     * @param pushDirection the push direction
     * @param virtualSpareTile the spare tile instance used for the virtual insertion
     * @param start start coordinate in the virtual post-push state
     * @param target target coordinate in the virtual post-push state
     * @return path from {@code start} to {@code target} in the virtual post-push state, or empty list if unreachable
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
     * Finds a path after applying a <em>virtual</em> push, constructing the virtual spare tile from entrances.
     * <p>
     * The virtual spare tile copies treasure and bonus from the real spare tile, but uses the given entrances and
     * is marked as non-fixed. The actual board is not modified.
     * </p>
     *
     * @param board the original board state (not modified)
     * @param pushIndex the push index (row for LEFT/RIGHT, column for UP/DOWN)
     * @param pushDirection the push direction
     * @param spareTileEntrances entrances of the virtual spare tile (already rotated)
     * @param start start coordinate in the virtual post-push state
     * @param target target coordinate in the virtual post-push state
     * @return path from {@code start} to {@code target} in the virtual post-push state, or empty list if unreachable
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


    /**
     * A* search on a virtual board state created by a simulated push.
     * <p>
     * Tile access is performed via {@link #getVirtualTileAt(GameBoard, int, Direction, Tile, int, int)}
     * rather than modifying the board.
     * </p>
     *
     * @param board the original board state (not modified)
     * @param pushIndex the virtual push index
     * @param pushDirection the virtual push direction
     * @param rotatedSpareTile the spare tile inserted into the virtual state (already rotated)
     * @param start start coordinate after the virtual push
     * @param target target coordinate after the virtual push
     * @return path from start to target in the virtual state, or empty list if unreachable
     */
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


    /**
     * Computes valid neighbors from {@code current} in the virtual post-push state.
     * <p>
     * Neighbor validity requires reciprocal entrances between adjacent tiles.
     * All tile lookups are resolved via {@link #getVirtualTileAt(GameBoard, int, Direction, Tile, int, int)}.
     * </p>
     *
     * @param board the original board
     * @param pushIndex the virtual push index
     * @param pushDirection the virtual push direction
     * @param rotatedSpareTile the inserted spare tile (already rotated)
     * @param current current coordinate
     * @param rows number of board rows
     * @param cols number of board columns
     * @return list of reachable neighbor coordinates (may be empty)
     */
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


    /**
     * Resolves the tile at {@code (row, column)} as it would appear after a single virtual push.
     * <p>
     * On the affected row/column, positions are mapped to their source positions in the original board,
     * except for the insertion point which returns {@code rotatedSpareTile}.
     * Unaffected positions return the original tile at the same coordinate.
     * </p>
     *
     * @param board the original board
     * @param pushIndex the virtual push index
     * @param pushDirection the virtual push direction
     * @param rotatedSpareTile spare tile inserted into the virtual state (already rotated)
     * @param row requested row
     * @param column requested column
     * @return the tile at {@code (row, column)} in the virtual post-push state (may be {@code null})
     */
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
     * Finds a path after two consecutive <em>virtual</em> pushes.
     * <p>
     * Simulates push #1 (with rotations on the current spare tile), derives the new spare tile
     * (the tile pushed out), applies rotations for push #2, and then performs A* search on the resulting
     * virtual board state. The original board is not modified.
     * </p>
     *
     * @param board the original board state (not modified)
     * @param push1Index first push index
     * @param push1Direction first push direction
     * @param push1Rotations number of clockwise 90° rotations for the spare tile of push #1 (0..3)
     * @param push2Index second push index
     * @param push2Direction second push direction
     * @param push2Rotations number of clockwise 90° rotations for the spare tile of push #2 (0..3)
     * @param start start coordinate after both pushes
     * @param target target coordinate after both pushes
     * @return path from start to target after both pushes, or empty list if unreachable
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


    /**
     * Computes all coordinates reachable from {@code start} after a single <em>virtual</em> push.
     * <p>
     * This method performs a BFS on the virtual post-push board state and returns the visited set.
     * The virtual spare tile is constructed from the current spare tile while applying {@code spareTileEntrances}.
     * The original board is not modified.
     * </p>
     *
     * @param board the original board state (not modified)
     * @param pushIndex the virtual push index
     * @param pushDirection the virtual push direction
     * @param spareTileEntrances entrances of the virtual spare tile (already rotated)
     * @param start start coordinate in the virtual post-push state
     * @return set of all reachable coordinates (empty set on invalid input)
     */
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
     * Returns the tile that would be pushed out of the board by the given push operation.
     * <p>
     * This tile becomes the next spare tile after executing the push.
     * </p>
     *
     * @param board the board
     * @param pushIndex the push index (row for LEFT/RIGHT, column for UP/DOWN)
     * @param pushDirection the push direction
     * @return the tile that would be pushed out
     * @throws IllegalArgumentException if {@code pushDirection} is invalid
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
     * A* search on a virtual board state created by applying two simulated pushes.
     * <p>
     * Tile lookups are resolved via {@link #getVirtualTileAfterTwoPushes(GameBoard, int, Direction, Tile, int, Direction, Tile, int, int)}
     * which maps coordinates back to the original board or one of the virtual spare tiles.
     * </p>
     *
     * @param board the original board
     * @param push1Index first push index
     * @param push1Direction first push direction
     * @param virtualSpareTile1 virtual spare tile inserted by push #1
     * @param push2Index second push index
     * @param push2Direction second push direction
     * @param virtualSpareTile2 virtual spare tile inserted by push #2
     * @param start start coordinate after both pushes
     * @param target target coordinate after both pushes
     * @return path from start to target, or empty list if unreachable
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
     * Computes valid neighbors from {@code current} in the virtual state after two pushes.
     * <p>
     * Neighbor validity requires reciprocal entrances between adjacent tiles, resolved via
     * {@link #getVirtualTileAfterTwoPushes(GameBoard, int, Direction, Tile, int, Direction, Tile, int, int)}.
     * </p>
     *
     * @param board the original board
     * @param push1Index first push index
     * @param push1Direction first push direction
     * @param virtualSpareTile1 virtual spare tile inserted by push #1
     * @param push2Index second push index
     * @param push2Direction second push direction
     * @param virtualSpareTile2 virtual spare tile inserted by push #2
     * @param current current coordinate
     * @param rows number of board rows
     * @param cols number of board columns
     * @return list of reachable neighbor coordinates (may be empty)
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


    /**
     * Resolves the tile at {@code (row, column)} as it would appear after two consecutive virtual pushes.
     * <p>
     * The method applies inverse coordinate mapping:
     * <ol>
     *   <li>handle insertion point of push #2 (returns {@code virtualSpareTile2})</li>
     *   <li>map {@code (row, column)} back to the board state before push #2 if affected</li>
     *   <li>handle insertion point of push #1 on the mapped coordinate (returns {@code virtualSpareTile1})</li>
     *   <li>map the coordinate back to the original board state before push #1 if affected</li>
     * </ol>
     * Finally, it returns the tile from the original board at the computed source coordinate.
     * </p>
     *
     * @param board the original board
     * @param push1Index first push index
     * @param push1Direction first push direction
     * @param virtualSpareTile1 virtual spare tile inserted by push #1
     * @param push2Index second push index
     * @param push2Direction second push direction
     * @param virtualSpareTile2 virtual spare tile inserted by push #2
     * @param row requested row after both pushes
     * @param column requested column after both pushes
     * @return tile at the given coordinate after both pushes (may be {@code null})
     */
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
