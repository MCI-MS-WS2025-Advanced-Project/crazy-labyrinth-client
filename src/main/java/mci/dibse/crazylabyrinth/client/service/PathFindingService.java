package mci.dibse.crazylabyrinth.client.service;

import java.util.*;
import labyrinth.contracts.models.Direction;
import lombok.extern.slf4j.Slf4j;
import mci.dibse.crazylabyrinth.client.view.viewmodel.item.CoordinatesItem;
import mci.dibse.crazylabyrinth.client.view.viewmodel.item.GameBoardItem;
import mci.dibse.crazylabyrinth.client.view.viewmodel.item.TileItem;

@Slf4j
public class PathFindingService {

    // Cache for recently computed paths
    private final Map<String, List<CoordinatesItem>> pathCache =
            new LinkedHashMap<>(100, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(
                        Map.Entry<String, List<CoordinatesItem>> eldest) {
                    return size() > 50; // Keep only 50 most recent paths
                }
            };

    private static class Node {
        final int row;
        final int col;
        final int gScore;
        final int fScore;

        Node(int row, int col, int gScore, int fScore) {
            this.row = row;
            this.col = col;
            this.gScore = gScore;
            this.fScore = fScore;
        }
    }

    // Coordinate encoding for primitive maps (row * 1000 + col, assuming max 999 cols)
    /**
     * Encode a row/column pair into a single integer.
     *
     * @param row row index
     * @param col column index
     * @return encoded coordinate value
     */
    private static int encodeCoord(int row, int col) {
        return row * 1000 + col;
    }

    /**
     * Decode the row from an encoded coordinate.
     *
     * @param encoded encoded coordinate value
     * @return decoded row
     */
    private static int getRow(int encoded) {
        return encoded / 1000;
    }

    /**
     * Decode the column from an encoded coordinate.
     *
     * @param encoded encoded coordinate value
     * @return decoded column
     */
    private static int getCol(int encoded) {
        return encoded % 1000;
    }

    /**
     * Find a path between two coordinates on the board using A*.
     *
     * @param board board to search
     * @param start start coordinate
     * @param target target coordinate
     * @return list of coordinates representing the path, or empty if none
     */
    public List<CoordinatesItem> findPath(
            GameBoardItem board, CoordinatesItem start, CoordinatesItem target) {
        if (start.equals(target)) {
            return Collections.singletonList(start);
        }

        TileItem[][] tiles = board.getTiles();
        int rows = board.getRows();
        int cols = board.getCols();

        if (isValidPosition(start, rows, cols) || isValidPosition(target, rows, cols)) {
            return Collections.emptyList();
        }

        // Check cache first
        String cacheKey =
                start.getRowAsInt()
                        + ","
                        + start.getColumnAsInt()
                        + "->"
                        + target.getRowAsInt()
                        + ","
                        + target.getColumnAsInt()
                        + ":"
                        + System.identityHashCode(tiles); // Board state identifier
        List<CoordinatesItem> cached = pathCache.get(cacheKey);
        if (cached != null) {
            return new ArrayList<>(cached); // Return copy to avoid modification
        }

        List<CoordinatesItem> path = aStar(tiles, start, target, rows, cols);

        if (path != null && !path.isEmpty()) {
            pathCache.put(cacheKey, new ArrayList<>(path)); // Cache a copy
            return path;
        }

        return Collections.emptyList();
    }

    /**
     * Clear the internal path cache.
     */
    public void clearCache() {
        pathCache.clear();
    }

    /**
     * Run the A* search on the given board tiles.
     *
     * @param tiles board tiles
     * @param start start coordinate
     * @param target target coordinate
     * @param rows row count
     * @param cols column count
     * @return path coordinates or null if not found
     */
    private List<CoordinatesItem> aStar(
            TileItem[][] tiles, CoordinatesItem start, CoordinatesItem target, int rows, int cols) {

        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingInt(n -> n.fScore));
        Map<Integer, Integer> cameFrom = new HashMap<>(); // encoded coord -> encoded coord
        Map<Integer, Integer> gScore = new HashMap<>();
        Set<Integer> closedSet = new HashSet<>();

        int startEncoded = encodeCoord(start.getRowAsInt(), start.getColumnAsInt());
        int targetEncoded = encodeCoord(target.getRowAsInt(), target.getColumnAsInt());

        int h =
                manhattanDistance(
                        start.getRowAsInt(),
                        start.getColumnAsInt(),
                        target.getRowAsInt(),
                        target.getColumnAsInt());
        openSet.offer(new Node(start.getRowAsInt(), start.getColumnAsInt(), 0, h));
        gScore.put(startEncoded, 0);

        int maxIterations = rows * cols * 2; // Prevent infinite loops
        int iterations = 0;

        while (!openSet.isEmpty() && iterations < maxIterations) {
            iterations++;
            Node current = openSet.poll();
            int currentEncoded = encodeCoord(current.row, current.col);

            if (currentEncoded == targetEncoded) {
                return reconstructPath(cameFrom, startEncoded, targetEncoded);
            }

            if (closedSet.contains(currentEncoded)) {
                continue;
            }
            closedSet.add(currentEncoded);

            int currentGScore = gScore.get(currentEncoded);
            int[] neighbors = getValidNeighborCoords(tiles, current.row, current.col, rows, cols);

            for (int i = 0; i < neighbors.length; i += 2) {
                int neighborRow = neighbors[i];
                int neighborCol = neighbors[i + 1];
                int neighborEncoded = encodeCoord(neighborRow, neighborCol);

                if (closedSet.contains(neighborEncoded)) {
                    continue;
                }

                int tentativeGScore = currentGScore + 1;
                int neighborGScore = gScore.getOrDefault(neighborEncoded, Integer.MAX_VALUE);

                if (tentativeGScore < neighborGScore) {
                    cameFrom.put(neighborEncoded, currentEncoded);
                    gScore.put(neighborEncoded, tentativeGScore);
                    int fScore =
                            tentativeGScore
                                    + manhattanDistance(
                                            neighborRow,
                                            neighborCol,
                                            target.getRowAsInt(),
                                            target.getColumnAsInt());
                    openSet.offer(new Node(neighborRow, neighborCol, tentativeGScore, fScore));
                }
            }
        }
        return null;
    }

    /**
     * Reconstruct a path from a map of predecessors.
     *
     * @param cameFrom map of encoded coordinate to its predecessor
     * @param start encoded start coordinate
     * @param target encoded target coordinate
     * @return ordered list of coordinates from start to target
     */
    private List<CoordinatesItem> reconstructPath(
            Map<Integer, Integer> cameFrom, int start, int target) {
        List<CoordinatesItem> path = new ArrayList<>();
        Integer current = target;

        while (current != null) {
            int row = getRow(current);
            int col = getCol(current);
            path.add(new CoordinatesItem(row, col));
            current = cameFrom.get(current);

            // Safety check to prevent infinite loops
            if (current != null && current.equals(start)) {
                path.add(new CoordinatesItem(getRow(start), getCol(start)));
                break;
            }
        }

        Collections.reverse(path);
        return path;
    }

    // Returns flattened array of [row1, col1, row2, col2, ...] for better performance
    /**
     * Collect valid neighboring coordinates that can be traversed.
     *
     * @param tiles board tiles
     * @param row current row
     * @param col current column
     * @param rows total rows
     * @param cols total columns
     * @return flattened array of neighboring row/col pairs
     */
    private int[] getValidNeighborCoords(TileItem[][] tiles, int row, int col, int rows, int cols) {
        List<Integer> neighbors = new ArrayList<>();
        TileItem currentTile = tiles[row][col];

        if (currentTile == null || currentTile.getDirections() == null) {
            return new int[0];
        }

        List<Direction> currentDirections = currentTile.getDirections();

        // Check UP
        if (currentDirections.contains(Direction.UP) && row > 0) {
            TileItem neighborTile = tiles[row - 1][col];
            if (neighborTile != null
                    && neighborTile.getDirections() != null
                    && neighborTile.getDirections().contains(Direction.DOWN)) {
                neighbors.add(row - 1);
                neighbors.add(col);
            }
        }

        // Check DOWN
        if (currentDirections.contains(Direction.DOWN) && row < rows - 1) {
            TileItem neighborTile = tiles[row + 1][col];
            if (neighborTile != null
                    && neighborTile.getDirections() != null
                    && neighborTile.getDirections().contains(Direction.UP)) {
                neighbors.add(row + 1);
                neighbors.add(col);
            }
        }

        // Check LEFT
        if (currentDirections.contains(Direction.LEFT) && col > 0) {
            TileItem neighborTile = tiles[row][col - 1];
            if (neighborTile != null
                    && neighborTile.getDirections() != null
                    && neighborTile.getDirections().contains(Direction.RIGHT)) {
                neighbors.add(row);
                neighbors.add(col - 1);
            }
        }

        // Check RIGHT
        if (currentDirections.contains(Direction.RIGHT) && col < cols - 1) {
            TileItem neighborTile = tiles[row][col + 1];
            if (neighborTile != null
                    && neighborTile.getDirections() != null
                    && neighborTile.getDirections().contains(Direction.LEFT)) {
                neighbors.add(row);
                neighbors.add(col + 1);
            }
        }

        return neighbors.stream().mapToInt(Integer::intValue).toArray();
    }

    /**
     * Check whether a coordinate lies outside the board bounds.
     *
     * @param coordinates coordinate to check
     * @param rows total rows
     * @param cols total columns
     * @return true if outside bounds
     */
    private boolean isValidPosition(CoordinatesItem coordinates, int rows, int cols) {
        return coordinates.getColumnAsInt() < 0
                || coordinates.getColumnAsInt() >= cols
                || coordinates.getRowAsInt() < 0
                || coordinates.getRowAsInt() >= rows;
    }

    /**
     * Compute Manhattan distance between two grid coordinates.
     *
     * @param row1 first row
     * @param col1 first column
     * @param row2 second row
     * @param col2 second column
     * @return Manhattan distance
     */
    private int manhattanDistance(int row1, int col1, int row2, int col2) {
        return Math.abs(col1 - col2) + Math.abs(row1 - row2);
    }
}
