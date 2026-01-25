package mci.dibse.crazylabyrinth.client.ai;



import labyrinth.contracts.models.GameStateEventPayload;
import labyrinth.contracts.models.PlayerState;
import labyrinth.contracts.models.Coordinates;
import labyrinth.contracts.models.Direction;


import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Utility component for selecting safe move targets and performing occupancy checks.
 *
 * <p><b>What it is for:</b> Centralizes logic for:
 * <ul>
 *   <li>checking whether positions are (or will be) occupied by other players, and</li>
 *   <li>selecting a suitable move target along a path while avoiding collisions.</li>
 * </ul>
 *
 * <p>The methods in this class are used by offensive, defensive, and bonus-related calculators to ensure that
 * chosen move targets do not result in moving onto a tile occupied by another player.</p>
 */
@Component
public class MoveTargetHelper {

    private final BoardSimulationHelper boardSimulationHelper;

    public MoveTargetHelper(BoardSimulationHelper boardSimulationHelper) {
        this.boardSimulationHelper = boardSimulationHelper;
    }

    /**
     * Checks whether a given position would be occupied by any other player after applying a virtual push.
     *
     * <p><b>What it is for:</b> Used during push simulation to avoid selecting move targets that will be occupied
     * after the board shift is applied.</p>
     *
     * @param game the current game state payload
     * @param position the position to check (interpreted as post-push position)
     * @param excludePlayerId the player id to exclude from the check (typically the AI itself)
     * @param pushIndex the push index used for the virtual push
     * @param pushDirection the push direction used for the virtual push
     * @return {@code true} if any other player ends up on {@code position} after the push; {@code false} otherwise
     */
    public boolean isPositionOccupiedAfterPush(
            GameStateEventPayload game,
            Coordinates position,
            String excludePlayerId,
            int pushIndex,
            Direction pushDirection
    ) {
        int rows = game.getBoard().getRows();
        int cols = game.getBoard().getCols();

        for (PlayerState p : game.getPlayers()) {
            if (p.getPlayerInfo().getId().equals(excludePlayerId)) continue;

            Coordinates otherAfterPush = boardSimulationHelper.calculatePositionAfterPush(
                    p.getCurrentPosition(), pushIndex, pushDirection, rows, cols
            );

            if (position.equals(otherAfterPush)) return true;
        }
        return false;
    }


    /**
     * Selects the best unoccupied coordinate on a given path after a virtual push.
     *
     * <p><b>What it is for:</b> Given a path computed on a virtually pushed board, this method chooses the farthest
     * reachable coordinate towards the target that is not occupied by another player after the same push.</p>
     *
     * <p><b>Selection strategy:</b>
     * <ul>
     *   <li>Iterates the path from the end (closest to the target) backwards.</li>
     *   <li>Returns the first coordinate that is not occupied according to
     *       {@link #isPositionOccupiedAfterPush(GameStateEventPayload, Coordinates, String, int, Direction)}.</li>
     *   <li>If all path positions are occupied, returns {@code aiPosAfterPush} as a fallback (i.e., "stay").</li>
     * </ul>
     *
     * @param game the current game state payload
     * @param path a path starting at {@code aiPosAfterPush} and leading towards {@code targetAfterPush}
     * @param targetAfterPush the intended target after the push (not necessarily used directly, but documents intent)
     * @param currentPlayerId the AI player's id (excluded from occupancy checks)
     * @param pushIndex the push index used for the virtual push
     * @param pushDirection the push direction used for the virtual push
     * @param aiPosAfterPush the AI's position after the push
     * @return the selected unoccupied coordinate along the path, or {@code aiPosAfterPush} if none is free
     */
    public Coordinates findBestUnoccupiedPositionAfterPush(
            GameStateEventPayload game,
            List<Coordinates> path,
            Coordinates targetAfterPush,
            String currentPlayerId,
            int pushIndex,
            Direction pushDirection,
            Coordinates aiPosAfterPush
    ) {
        if (path == null || path.isEmpty()) return aiPosAfterPush;

        // Prefer to move as far as possible along the path toward the target,
        for (int i = path.size() - 1; i >= 0; i--) {
            Coordinates candidate = path.get(i);

            if (!isPositionOccupiedAfterPush(game, candidate, currentPlayerId, pushIndex, pushDirection)) {
                return candidate;
            }
        }

        // Fallback: stay at current position after push
        return aiPosAfterPush;
    }

    /**
     * Checks whether a given position is currently occupied by another player.
     *
     * <p><b>What it is for:</b> Used in situations without push simulation (e.g., direct checks for BEAM/SWAP targets)
     * to avoid moving onto a tile already occupied by a different player.</p>
     *
     * @param game the current game state payload
     * @param position the position to check
     * @param excludePlayerId the player id to exclude from the check (typically the AI itself)
     * @return {@code true} if {@code position} is occupied by a different player; {@code false} otherwise
     */
    public boolean isPositionOccupied(GameStateEventPayload game, Coordinates position, String excludePlayerId) {
        for (PlayerState player : game.getPlayers()) {
            if (player.getPlayerInfo().getId().equals(excludePlayerId)) {
                continue; // Skip the AI itself
            }
            if (player.getCurrentPosition().equals(position)) {
                return true; // Position is occupied by another player
            }
        }
        return false;
    }

}
