package mci.dibse.crazylabyrinth.client.ai;

import labyrinth.contracts.models.*;

import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


/**
 * Computes defensive {@link AiDecision}s when at least one opponent is considered an immediate threat.
 *
 * <p><b>What it is for:</b> This calculator searches push (and rotation) candidates that prevent opponents from winning
 * on their next turn. It evaluates each candidate by simulating the effects of the push and then checking opponent
 * reachability towards their home positions.</p>
 *
 * <p><b>How it works (high level):</b>
 * <ul>
 *   <li>Identifies "dangerous" opponents (those with {@code remainingTreasureCount == null}).</li>
 *   <li>Evaluates all legal pushes (normal indices), excluding reverse pushes.</li>
 *   <li>Optionally evaluates "push fixed" candidates (fixed indices) if {@link BonusType#PUSH_FIXED} is available.</li>
 *   <li>For each candidate, applies a threat check via {@link OpponentThreatDetector} and path checks via {@link AiPathFindingService}.</li>
 *   <li>Uses a score with strong penalties for enabling opponent wins, and small secondary incentives for the AI's own progress.</li>
 * </ul>
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
     * Calculates a defensive decision consisting of a push (with rotations) and a move target.
     *
     * <p><b>What it is for:</b> Attempts to select a push that reduces opponent winning chances while still producing a valid
     * push-and-move turn for the AI.</p>
     *
     * <p><b>Behavior details:</b>
     * <ul>
     *   <li>Builds a list of "dangerous" opponents as players whose {@code remainingTreasureCount} is {@code null}.</li>
     *   <li>Searches for the best defensive turn among normal push indices using findBestDefensiveTurn(...)}.</li>
     *   <li>If the best normal result is missing or not safe and {@link BonusType#PUSH_FIXED} is available,
     *       it additionally searches among fixed indices and may return a {@code pushFixedThenPushAndMove} decision.</li>
     *   <li>If no normal candidate exists, returns a hard fallback decision using a fixed push and staying at the current position.</li>
     * </ul>
     *
     * @param game the current game state payload
     * @param board the current game board
     * @param myPlayerId the AI player's id
     * @param availableBonuses the set of bonuses currently available to the AI
     * @param currentPos the AI player's current position (pre-push)
     * @param myHomePosition the AI player's home position
     * @param myTargetTreasure the current target treasure id; may be {@code null}
     * @return a defensive {@link AiDecision} (push + move), potentially using {@link BonusType#PUSH_FIXED} when selected
     */
    public AiDecision calculateDefensiveMove(GameStateEventPayload game, GameBoard board, String myPlayerId, Set<BonusType> availableBonuses, Coordinates currentPos, Coordinates myHomePosition, Integer myTargetTreasure) {


        // Find ALL opponents that could win soon (Home phase)
        List<PlayerState> dangerousOpponents = new ArrayList<>();
        for (PlayerState opponent : game.getPlayers()) {
            if (opponent.getPlayerInfo().getId().equals(myPlayerId)) continue;
            if (opponent.getRemainingTreasureCount() == null) { // All treasures collected -> heading home
                dangerousOpponents.add(opponent);
            }
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
            if (!boardSimulationHelper.isReversePush(board, 1, Direction.DOWN)) {
                return AiDecision.pushAndMove(
                        new AiDecision.PushDecision(1, Direction.DOWN, 0),
                        new AiDecision.MoveDecision(currentPos)
                );
            } else {
                return AiDecision.pushAndMove(
                        new AiDecision.PushDecision(1, Direction.UP, 0),
                        new AiDecision.MoveDecision(currentPos)
                );
            }
        }

        return AiDecision.pushAndMove(new AiDecision.PushDecision(normalBest.pushIndex, normalBest.pushDirection, normalBest.rotations),
                new AiDecision.MoveDecision(normalBest.moveTarget));
    }


    /**
     * Internal value holder for the best defensive candidate found during search.
     *
     * <p><b>What it is for:</b> Stores the chosen push parameters, the selected move target and the computed score.
     * The {@code safe} flag indicates whether the candidate avoids allowing an opponent win on the next turn
     * according to the threat checks performed during evaluation.</p>
     */
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

    /**
     * Searches the best defensive candidate among a set of push indices.
     *
     * <p><b>What it is for:</b> Core search routine used by calculateDefensiveMove(...) for:
     * <ul>
     *   <li>normal defense search (valid push indices), and</li>
     *   <li>optional push-fixed defense search (valid fixed indices) when {@code isPushFixedMode} is {@code true}.</li>
     * </ul>
     *
     * <p><b>What it does per candidate (push index, direction, rotation):</b>
     * <ul>
     *   <li>Skips illegal pushes via {@link AiContractUtil#canShift(GameBoard, int, Direction)} and reverse pushes via
     *       {@link BoardSimulationHelper#isReversePush(GameBoard, int, Direction)}.</li>
     *   <li>Builds a rotated spare {@link Tile} by rotating entrances with
     *       {@link BoardSimulationHelper#calculateRotatedEntrances(Direction[], int)} and copying treasure/bonus flags.</li>
     *   <li>Computes positions after the push for AI and opponents with
     *       {@link BoardSimulationHelper#calculatePositionAfterPush(Coordinates, int, Direction, int, int)}.</li>
     *   <li>Per dangerous opponent:
     *     <ul>
     *       <li>Checks whether a direct path to home exists after the candidate push using
     *           {@code AiPathFindingService.findPathAfterVirtualPushWithTile(...)}; such candidates are discarded.</li>
     *       <li>Checks whether the opponent can win next turn via
     *           {@link OpponentThreatDetector#canOpponentWinAfterThisPush(GameBoard, int, Direction, int, PlayerState, Coordinates)}.</li>
     *     </ul>
     *   </li>
     *   <li>Computes a defense score with a strong penalty if any opponent can win next turn; otherwise rewards safety.</li>
     *   <li>Optionally adds small secondary scoring for AI progress towards its own target (treasure or home) and selects a
     *       move target using {@link MoveTargetHelper#findBestUnoccupiedPositionAfterPush(GameStateEventPayload, List, Coordinates, String, int, Direction, Coordinates)}.</li>
     * </ul>
     *
     * <p><b>Selection:</b> Chooses the candidate with the highest score; ties prefer {@code safe} candidates.</p>
     *
     * <p><b>Final guard:</b> After selecting the best candidate, ensures the chosen move target is not occupied after the push
     * using {@link MoveTargetHelper#isPositionOccupiedAfterPush(GameStateEventPayload, Coordinates, String, int, Direction)}.
     * If occupied, it falls back to the AI position after the selected push.</p>
     *
     * @param game the current game state payload
     * @param board the current game board
     * @param myPlayerId the AI player's id
     * @param dangerousOpponents list of opponents considered threatening (evaluated for defensive checks)
     * @param pushIndices push indices to evaluate (normal or fixed indices)
     * @param myTargetTreasure the AI's current target treasure id; may be {@code null}
     * @param myHomePosition the AI's home position
     * @param currentPos the AI's current position
     * @param isPushFixedMode whether this search is performed for the PUSH_FIXED variant (parameter is informational)
     * @return the best {@link DefenseBest} found, or {@code null} if no legal candidate exists
     */
    private DefenseBest findBestDefensiveTurn(
            GameStateEventPayload game,
            GameBoard board,
            String myPlayerId,
            List<PlayerState> dangerousOpponents,
            List<Integer> pushIndices,
            Integer myTargetTreasure,
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

                    // Build rotated spare tile
                    Direction[] rotatedEntrances =
                            boardSimulationHelper.calculateRotatedEntrances(spare.getEntrances(), rotations);

                    Tile rotatedSpareTile = new Tile();
                    rotatedSpareTile.setEntrances(rotatedEntrances);
                    rotatedSpareTile.setTreasure(spare.getTreasure());
                    rotatedSpareTile.setBonus(spare.getBonus());
                    rotatedSpareTile.setIsFixed(false);

                    // AI position after this push
                    Coordinates aiPosAfterPush = boardSimulationHelper.calculatePositionAfterPush(
                            currentPos, pushIndex, pushDirection, board.getRows(), board.getCols()
                    );


                    boolean opensDirectWin = false;
                    boolean allowsWinInOnePush = false;

                    for (PlayerState opponent : dangerousOpponents) {
                        Coordinates oppPos = opponent.getCurrentPosition();
                        Coordinates oppHome = opponent.getHomePosition();

                        Coordinates oppAfterPush = boardSimulationHelper.calculatePositionAfterPush(
                                oppPos, pushIndex, pushDirection, board.getRows(), board.getCols()
                        );

                        // CHECK 1: "Direct" path exists after our push
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
                        }
                    }

                    if (opensDirectWin) {
                        // NEVER choose a move that opens direct win
                        continue;
                    }


                    int defenseScore = 0;

                    // Huge penalty if any opponent can win next turn
                    if (allowsWinInOnePush) {
                        defenseScore -= 50_000;
                    } else {
                        defenseScore += 10_000; // reward "safe" candidates
                    }

                    // AI progress scoring (only secondary)
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
                        // All treasures collected -> go home
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