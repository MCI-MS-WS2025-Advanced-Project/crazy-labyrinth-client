package mci.dibse.crazylabyrinth.client.ai;

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
import java.util.function.BiFunction;

/**
 * Selects an opportunistic bonus-related move target based on an already chosen push.
 *
 * <p>This component evaluates bonus tile positions after applying a specific (already selected) push and rotation,
 * and chooses a move target that is reachable on the virtually pushed board.
 *
 * <p>Purpose in the AI flow:
 * <ul>
 *   <li>Used after a push decision has been made, to optionally redirect the move towards a reachable bonus position.</li>
 *   <li>Does not search or optimize pushes; it only works with the push parameters passed in.</li>
 * </ul>
 */
@Component
public class BonusCalculator {

    private final BoardSimulationHelper boardSimulationHelper;
    private final AiPathFindingService AiPathFindingService;
    private final MoveTargetHelper moveTargetHelper;

    public BonusCalculator(BoardSimulationHelper boardSimulationHelper, MoveTargetHelper moveTargetHelper) {
        this.boardSimulationHelper = boardSimulationHelper;
        this.moveTargetHelper = moveTargetHelper;
        this.AiPathFindingService = new AiPathFindingService();
    }

    /**
     * Picks an opportunistic move target for collecting a bonus after the push decision is already fixed.
     *
     * <p>What this method does:
     * <ul>
     *   <li>Transforms each known bonus position using {@link BoardSimulationHelper#calculatePositionAfterPush(Coordinates, int, Direction, int, int)}
     *       to get its location after the chosen push.</li>
     *   <li>Uses {@code AiPathFindingService.findPathAfterVirtualPush(...)} to check whether the bonus is reachable
     *       for the AI on the virtually pushed board.</li>
     *   <li>Filters to bonuses that are considered "on the way" towards the already chosen goal position
     *       using Manhattan distance comparisons.</li>
     *   <li>Scores candidates primarily by path length, and penalizes targets that are occupied after the push.</li>
     *   <li>If the bonus tile is occupied, it can select an alternative unoccupied position along the computed path
     *       via {@link MoveTargetHelper#findBestUnoccupiedPositionAfterPush(GameStateEventPayload, List, Coordinates, String, int, Direction, Coordinates)}.</li>
     * </ul>
     *
     * <p>What this method does NOT do:
     * <ul>
     *   <li>It does not change or re-evaluate the push index/direction/rotation.</li>
     *   <li>It does not compute the main goal; the goal position is provided via {@code bestGoalPosAfterPush}.</li>
     * </ul>
     *
     * @param game the current game state payload, used for occupancy checks
     * @param board the current game board (used for dimensions and virtual pathfinding)
     * @param myPlayerId the AI player's id (used to ignore the AI itself in occupancy checks)
     * @param bonusPositions list of bonus positions on the current board (pre-push coordinates)
     * @param bestPushIndex the chosen push index
     * @param bestPushDirection the chosen push direction
     * @param bestRotatedEntrances the rotated entrances for the spare tile used in the chosen push
     * @param bestAiPosAfterPush the AI position after applying the chosen push
     * @param bestGoalPosAfterPush the already chosen goal position after applying the chosen push
     * @return the selected bonus move target after the push, or {@code null} if no suitable reachable candidate exists
     */
    public Coordinates chooseBonusMoveTargetAfterChosenPush(
            GameStateEventPayload game,
            GameBoard board,
            String myPlayerId,
            List<Coordinates> bonusPositions,
            int bestPushIndex,
            Direction bestPushDirection,
            Direction[] bestRotatedEntrances,
            Coordinates bestAiPosAfterPush,
            Coordinates bestGoalPosAfterPush
    ) {
        if (bonusPositions == null || bonusPositions.isEmpty()) return null;
        if (bestRotatedEntrances == null) return null;
        if (bestAiPosAfterPush == null || bestGoalPosAfterPush == null) return null;

        Coordinates bonusMoveTarget = null;
        int bestBonusScore = Integer.MIN_VALUE;

        int distNow = boardSimulationHelper.manhattanDistance(bestAiPosAfterPush, bestGoalPosAfterPush);

        for (Coordinates bonusPos : bonusPositions) {
            if (bonusPos == null) continue;

            Coordinates bonusPosAfterPush = boardSimulationHelper.calculatePositionAfterPush(
                    bonusPos,
                    bestPushIndex,
                    bestPushDirection,
                    board.getRows(),
                    board.getCols()
            );

            List<Coordinates> pathToBonus = AiPathFindingService.findPathAfterVirtualPush(
                    board,
                    bestPushIndex,
                    bestPushDirection,
                    bestRotatedEntrances,
                    bestAiPosAfterPush,
                    bonusPosAfterPush
            );

            boolean bonusReachable = pathToBonus != null && !pathToBonus.isEmpty()
                    && pathToBonus.contains(bonusPosAfterPush);

            if (!bonusReachable) continue;

            int distFromBonus = boardSimulationHelper.manhattanDistance(bonusPosAfterPush, bestGoalPosAfterPush);
            boolean bonusOnWay = distFromBonus <= distNow + 1;
            if (!bonusOnWay) continue;

            int pathLen = pathToBonus.size() - 1;
            int score = 100_000 - pathLen * 100;

            boolean occupied = moveTargetHelper.isPositionOccupiedAfterPush(
                    game, bonusPosAfterPush, myPlayerId, bestPushIndex, bestPushDirection
            );
            if (occupied) score -= 500;

            if (score > bestBonusScore) {
                bonusMoveTarget = !occupied
                        ? bonusPosAfterPush
                        : moveTargetHelper.findBestUnoccupiedPositionAfterPush(
                        game, pathToBonus, bonusPosAfterPush, myPlayerId,
                        bestPushIndex, bestPushDirection, bestAiPosAfterPush
                );
                bestBonusScore = score;
            }
        }

        return bonusMoveTarget;
    }

}
