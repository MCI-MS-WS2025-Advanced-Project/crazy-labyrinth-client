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

// TODO: JAVA DOC überarbeiten

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
     * Picks an opportunistic bonus move target AFTER the already chosen push.
     * Does NOT re-evaluate pushes. Uses the same virtual push inputs as the treasure decision.
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
