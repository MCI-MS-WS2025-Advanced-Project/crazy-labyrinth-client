package mci.dibse.crazylabyrinth.client.ai;

import labyrinth.contracts.models.*;

import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


/**
 * Helper class for calculating defensive moves when opponents threaten to win.
 * Finds pushes that block opponent winning paths while validating safety.
 */
@Component
public class DefensiveMoveCalculator {

    private final AiPathFindingService AiPathFindingService;
    private final BoardSimulationHelper boardSimulationHelper;
    private final OpponentThreatDetector opponentThreatDetector;
    private final MoveTargetHelper moveTargetHelper;

    private static final Logger log = LoggerFactory.getLogger(DefensiveMoveCalculator.class);

    public DefensiveMoveCalculator(
            BoardSimulationHelper boardSimulationHelper,
            OpponentThreatDetector opponentThreatDetector,
            MoveTargetHelper moveTargetHelper) {
        this.AiPathFindingService = new AiPathFindingService();
        this.boardSimulationHelper = boardSimulationHelper;
        this.opponentThreatDetector = opponentThreatDetector;
        this.moveTargetHelper = moveTargetHelper;
    }

    /**
     * Calculates a defensive move that prevents opponents from winning.
     * Evaluates all possible pushes and ensures they don't open new winning paths.
     *
     * @param game the current game state
     * @param board the game board
     * @return the best defensive decision
     */

    public AiDecision calculateDefensiveMove(GameStateEventPayload game, GameBoard board, String myPlayerId, Set<BonusType> availableBonuses, Coordinates currentPos, Coordinates myHomePosition, Treasure myTargetTreasure) {


        // Find ALL opponents that could win soon (Home phase)
        List<PlayerState> dangerousOpponents = new ArrayList<>();
        for (PlayerState opponent : game.getPlayers()) {
            if (opponent.getPlayerInfo().getId().equals(myPlayerId)) continue;
            if (opponent.getRemainingTreasureCount() == null) { // All treasures collected -> heading home
                dangerousOpponents.add(opponent);
            }
        }

        // If no dangerous opponents, you can still play "safe offense" or just fallback to offense.
        // But selector probably calls this only when needed.
        if (dangerousOpponents.isEmpty()) {
            // Keep current behavior: just do a normal "progress" move (still push & move).
            // Easiest: no extra defense penalty, let scoring do its job.
        }


        DefenseBest normalBest = findBestDefensiveTurn(game, board, myPlayerId, dangerousOpponents, boardSimulationHelper.getValidPushIndices(board), myTargetTreasure, myHomePosition, currentPos, false);

        // If normal defense is not safe AND we have PUSH_FIXED -> try PUSH_FIXED defense
        if ((normalBest == null || !normalBest.safe) && availableBonuses.contains(BonusType.PUSH_FIXED)) {
            DefenseBest pushFixedBest = findBestDefensiveTurn(
                    game, board, myPlayerId, dangerousOpponents,
                    boardSimulationHelper.getValidPushFixedIndices(board),
                    myTargetTreasure,
                    myHomePosition,
                    currentPos,
                    true
            );

            // Prefer PUSH_FIXED if it yields a safe solution, or if normalBest is null
            if (pushFixedBest != null && (pushFixedBest.safe || normalBest == null)) {
                // deprecated
                return AiDecision.pushFixedThenPushAndMove(
                        new AiDecision.PushDecision(pushFixedBest.pushIndex,
                                pushFixedBest.pushDirection, pushFixedBest.rotations),
                        new AiDecision.MoveDecision(pushFixedBest.moveTarget));
            }
        }

        // Fallback to best normal defense (even if not safe, it's our best shot)
        if (normalBest == null) {
            // absolute fallback: do nothing harmful (still must push & move) -> keep defaults
            // TODO: Hier noch auf Reverse überprüfen
            return  AiDecision.pushAndMove(
                    new AiDecision.PushDecision(1, Direction.DOWN, 0),
                    new AiDecision.MoveDecision(currentPos));
        }

        return AiDecision.pushAndMove(new AiDecision.PushDecision(normalBest.pushIndex, normalBest.pushDirection, normalBest.rotations),
                new AiDecision.MoveDecision(normalBest.moveTarget));
    }

    private static class DefenseBest {
        final int score;
        final int pushIndex;
        final Direction pushDirection;
        final int rotations;
        final Coordinates moveTarget;
        final boolean safe; // true = does NOT allow opponent win next turn (after our push)

        private DefenseBest(int score, int pushIndex, Direction pushDirection, int rotations, Coordinates moveTarget, boolean safe) {
            this.score = score;
            this.pushIndex = pushIndex;
            this.pushDirection = pushDirection;
            this.rotations = rotations;
            this.moveTarget = moveTarget;
            this.safe = safe;
        }
    }

    private DefenseBest findBestDefensiveTurn(
            GameStateEventPayload game,
            GameBoard board,
            String myPlayerId,
            List<PlayerState> dangerousOpponents,
            List<Integer> pushIndices,
            Treasure myTargetTreasure,
            Coordinates myHomePosition,
            Coordinates currentPos,
            boolean isPushFixedMode
    ) {

        int bestScore = Integer.MIN_VALUE;
        int bestPushIndex = -1;
        Direction bestPushDirection = null;
        int bestRotations = 0;
        Coordinates bestMoveTarget = currentPos;
        boolean bestSafe = false;

        Direction[] directions = {Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT};
        Tile spare = board.getSpareTile();

        for (int pushIndex : pushIndices) {
            for (Direction pushDirection : directions) {

                if (!AiContractUtil.canShift(board, pushIndex, pushDirection)) continue;
                if (boardSimulationHelper.isReversePush(board, pushIndex, pushDirection)) continue;

                for (int rotations = 0; rotations < 4; rotations++) {

                    // Build rotated spare tile (like in your current code)
                    Direction[] rotatedEntrances =
                            boardSimulationHelper.calculateRotatedEntrances(spare.getEntrances(), rotations);

                    Tile rotatedSpareTile = new Tile();
                    rotatedSpareTile.setEntrances(rotatedEntrances);
                    rotatedSpareTile.setTreasure(spare.getTreasure());
                    rotatedSpareTile.setBonus(spare.getBonus());
                    rotatedSpareTile.setIsFixed(false); // oder spare.getIsFixed() wenn du willst

                    // AI position after this push
                    Coordinates aiPosAfterPush = boardSimulationHelper.calculatePositionAfterPush(
                            currentPos, pushIndex, pushDirection, board.getRows(), board.getCols()
                    );

                    // ----- Threat evaluation (Worst-case over all dangerous opponents) -----
                    boolean opensDirectWin = false;
                    boolean allowsWinInOnePush = false;

                    for (PlayerState opponent : dangerousOpponents) {
                        Coordinates oppPos = opponent.getCurrentPosition();
                        Coordinates oppHome = opponent.getHomePosition();

                        Coordinates oppAfterPush = boardSimulationHelper.calculatePositionAfterPush(
                                oppPos, pushIndex, pushDirection, board.getRows(), board.getCols()
                        );

                        // CHECK 1: "Direct" path exists after our push (opponent can keep it with a harmless push)
                        List<Coordinates> directPath = AiPathFindingService.findPathAfterVirtualPushWithTile(
                                board, pushIndex, pushDirection, rotatedSpareTile,
                                oppAfterPush, oppHome
                        );

                        if (directPath != null && !directPath.isEmpty() && directPath.contains(oppHome)) {
                            opensDirectWin = true;
                            break; // this candidate is dead anyway
                        }

                        // CHECK 2: opponent can win next turn after our push (opponent push + move)
                        boolean canWinNextTurn = opponentThreatDetector.canOpponentWinAfterThisPush(
                                board, pushIndex, pushDirection, rotations,
                                opponent, oppAfterPush
                        );

                        if (canWinNextTurn) {
                            allowsWinInOnePush = true;
                            // do NOT break; check other opponents too (worst-case)
                        }
                    }

                    if (opensDirectWin) {
                        // NEVER choose a move that opens direct win
                        continue;
                    }

                    // ----- Base scoring -----
                    int defenseScore = 0;

                    // Huge penalty if any opponent can win next turn
                    if (allowsWinInOnePush) {
                        defenseScore -= 50_000;
                    } else {
                        defenseScore += 10_000; // reward "safe" candidates
                    }

                    // ----- AI progress scoring (only secondary) -----
                    Coordinates targetPosAfterPush = null;

                    if (myTargetTreasure != null) {
                        // Treasure position BEFORE push
                        Coordinates treasurePos = boardSimulationHelper.findTreasurePosition(board, myTargetTreasure);
                        if (treasurePos != null) {
                            // Treasure shifts with the board -> compute AFTER push
                            targetPosAfterPush = boardSimulationHelper.calculatePositionAfterPush(
                                    treasurePos, pushIndex, pushDirection, board.getRows(), board.getCols()
                            );
                        }
                    } else {
                        // All treasures collected -> go home (fixed edge)
                        targetPosAfterPush = myHomePosition;
                    }


                    Coordinates moveTarget = aiPosAfterPush;

                    if (targetPosAfterPush != null) {
                        int distBefore = boardSimulationHelper.manhattanDistance(currentPos, targetPosAfterPush);
                        int distAfter = boardSimulationHelper.manhattanDistance(aiPosAfterPush, targetPosAfterPush);
                        if (distAfter < distBefore) defenseScore += 200; // small tie-breaker

                        List<Coordinates> pathToTarget = AiPathFindingService.findPathAfterVirtualPushWithTile(
                                board, pushIndex, pushDirection, rotatedSpareTile,
                                aiPosAfterPush, targetPosAfterPush
                        );

                        if (pathToTarget != null && !pathToTarget.isEmpty()) {
                            moveTarget = moveTargetHelper.findBestUnoccupiedPositionAfterPush(
                                    game, pathToTarget, targetPosAfterPush,
                                    myPlayerId, pushIndex, pushDirection, aiPosAfterPush
                            );

                            if (moveTarget.equals(targetPosAfterPush)) {
                                defenseScore += 500; // reaching own target is nice, but not dominant
                            }
                        }
                    }

                    boolean safe = !allowsWinInOnePush;

                    // Deterministic selection:
                    // 1) higher score wins
                    // 2) if equal: prefer safe
                    boolean shouldUpdate = false;
                    if (defenseScore > bestScore) {
                        shouldUpdate = true;
                    } else if (defenseScore == bestScore) {
                        if (safe && !bestSafe) shouldUpdate = true;
                    }

                    if (shouldUpdate) {
                        bestScore = defenseScore;
                        bestPushIndex = pushIndex;
                        bestPushDirection = pushDirection;
                        bestRotations = rotations;
                        bestMoveTarget = moveTarget;
                        bestSafe = safe;
                    }
                }
            }
        }

        if (bestPushIndex == -1) return null;

        // Final guard: move target must not be occupied AFTER the chosen push
        if (moveTargetHelper.isPositionOccupiedAfterPush(game, bestMoveTarget, myPlayerId, bestPushIndex, bestPushDirection)) {
            log.warn("Defensive move target occupied after push -> fallback to aiPosAfterPush");
            bestMoveTarget = boardSimulationHelper.calculatePositionAfterPush(
                    currentPos, bestPushIndex, bestPushDirection, board.getRows(), board.getCols()
            );
        }

        return new DefenseBest(bestScore, bestPushIndex, bestPushDirection, bestRotations, bestMoveTarget, bestSafe);
    }

}