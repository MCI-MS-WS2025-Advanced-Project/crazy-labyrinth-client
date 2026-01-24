package mci.dibse.crazylabyrinth.client.ai;

//import mci.dibse.crazylabyrinth.gameserver.domain.model.Game;
//import mci.dibse.crazylabyrinth.gameserver.domain.model.Player;


import labyrinth.contracts.models.GameStateEventPayload;
import labyrinth.contracts.models.PlayerState;
import labyrinth.contracts.models.PlayerInfo;
import labyrinth.contracts.models.TurnState;
import labyrinth.contracts.models.GameBoard;
import labyrinth.contracts.models.Coordinates;
import labyrinth.contracts.models.Direction;
import labyrinth.contracts.models.BonusType;
import labyrinth.contracts.models.Tile;

import org.springframework.stereotype.Component;

import java.util.List;

// TODO: Add Java Doc
@Component
public class MoveTargetHelper {

    private final BoardSimulationHelper boardSimulationHelper;

    public MoveTargetHelper(BoardSimulationHelper boardSimulationHelper) {
        this.boardSimulationHelper = boardSimulationHelper;
    }

    /**
     * Checks whether a given position will be occupied by any OTHER player after a virtual push.
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
     * Returns the best unoccupied target on the given path, preferring the farthest reachable
     * position towards the target, while avoiding collisions after the given virtual push.
     *
     * Expected input:
     * - path starts at aiPosAfterPush and ends at (or near) targetAfterPush
     * - aiPosAfterPush is the player's position after the push
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
        // but do not step onto occupied fields.
        for (int i = path.size() - 1; i >= 0; i--) {
            Coordinates candidate = path.get(i);

            // must be reachable (it is, since it's on the path)
            // skip positions occupied after push
            if (!isPositionOccupiedAfterPush(game, candidate, currentPlayerId, pushIndex, pushDirection)) {
                return candidate;
            }
        }

        // Fallback: stay at current position after push
        return aiPosAfterPush;
    }

    /**
     * Checks if a position is occupied by another player.
     *
     * @param game the game state
     * @param position the position to check
     * @param excludePlayerId the player ID to exclude (typically the AI itself)
     * @return true if position is occupied by another player
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
