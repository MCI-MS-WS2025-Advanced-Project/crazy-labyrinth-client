package mci.dibse.crazylabyrinth.client.ai;

import labyrinth.contracts.models.GameStateEventPayload;
import labyrinth.contracts.models.GameBoard;
import labyrinth.contracts.models.Coordinates;
import labyrinth.contracts.models.Direction;
import labyrinth.contracts.models.BonusType;
import labyrinth.contracts.models.Tile;


import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


/**
 * Computes offensive {@link AiDecision}s for progressing towards the AI's current objective.
 *
 * <p><b>What it is for:</b> Produces push + move decisions that either:
 * <ul>
 *   <li>bring the AI to its home position when the "home" objective is active, or</li>
 *   <li>move the AI towards (or directly onto) the current treasure target.</li>
 * </ul>
 *
 * <p><b>How it works (as implemented):</b>
 * <ul>
 *   <li>Enumerates legal pushes (index + {@link Direction}) and rotations (0..3).</li>
 *   <li>Simulates the resulting player/goal positions after a push via {@link BoardSimulationHelper}.</li>
 *   <li>Evaluates reachability and paths on the virtually pushed board via {@link AiPathFindingService}.</li>
 *   <li>Optionally attempts bonus-based shortcuts via {@link BonusUsageDecider} and opportunistic bonus collection via {@link BonusCalculator}.</li>
 *   <li>Chooses a move target with collision avoidance using {@link MoveTargetHelper}.</li>
 * </ul>
 */
@Component
public class OffensiveMoveCalculator {

    private final AiPathFindingService AiPathFindingService;
    private final BoardSimulationHelper boardSimulationHelper;
    private final BonusCalculator bonusCalculator;
    private final BonusUsageDecider bonusUsageDecider;
    private final MoveTargetHelper moveTargetHelper;

    private static final Logger log = LoggerFactory.getLogger(OffensiveMoveCalculator.class);

    private static final int PUSH_TWICE_TOP_K = 5;

    private record PushCandidate(int score, int pushIndex, Direction pushDirection, int rotations) {}


    public OffensiveMoveCalculator(BoardSimulationHelper boardSimulationHelper, BonusCalculator bonusCalculator, BonusUsageDecider bonusUsageDecider, MoveTargetHelper moveTargetHelper) {
        this.AiPathFindingService = new AiPathFindingService();
        this.boardSimulationHelper = boardSimulationHelper;
        this.bonusCalculator = bonusCalculator;
        this.bonusUsageDecider = bonusUsageDecider;
        this.moveTargetHelper = moveTargetHelper;

    }

    /**
     * Calculates an offensive decision for the "go home" objective (no treasure target).
     *
     * <p><b>What it is for:</b> Attempts to reach {@code homePosition} as efficiently as possible.</p>
     *
     * <p><b>Bonus handling (as implemented):</b>
     * <ul>
     *   <li>If {@link BonusType#BEAM} is available, immediately returns a {@code beamAndMove} decision to {@code homePosition}.</li>
     *   <li>If {@link BonusType#PUSH_FIXED} is available, tries BonusUsageDecider#tryUsePushFixedForDirectGoal(...).</li>
     *   <li>If {@link BonusType#SWAP} is available, tries BonusUsageDecider#tryUseSwapIfItEnablesGoal(...).</li>
     * </ul>
     *
     * <p><b>Push evaluation (as implemented):</b>
     * <ul>
     *   <li>Evaluates all legal pushes from {@link BoardSimulationHelper#getValidPushIndices(GameBoard)} excluding reverse pushes.</li>
     *   <li>Scores candidates primarily by reachability of {@code homePosition} after the virtual push and secondarily by
     *       Manhattan distance when not reachable.</li>
     *   <li>Tracks top candidates for a later PUSH_TWICE attempt when {@link BonusType#PUSH_TWICE} is available and home is not reachable.</li>
     * </ul>
     *
     * <p><b>Move target selection (as implemented):</b>
     * <ul>
     *   <li>If a path to home exists after the chosen push, chooses the farthest unoccupied position on the path via
     *       MoveTargetHelper#findBestUnoccupiedPositionAfterPush(...).</li>
     *   <li>If no path exists, optionally tries PUSH_TWICE (top-K push1 candidates), otherwise optionally targets a bonus
     *       that is "on the way" via BonusCalculator#chooseBonusMoveTargetAfterChosenPush(...).</li>
     * </ul>
     *
     * @param game the current game state payload
     * @param myPlayerId the AI player's id
     * @param availableBonuses the set of bonuses available to the AI
     * @param currentPos the AI's current position (pre-push)
     * @param homePosition the AI's home position
     * @return an {@link AiDecision} containing a push and a move (or a bonus-based decision when applied)
     */
    public AiDecision calculateMoveToHome(GameStateEventPayload game, String myPlayerId, Set<BonusType> availableBonuses, Coordinates currentPos, Coordinates homePosition) {
        GameBoard board = game.getBoard();

        // use beam bonus if possible and win
        if (availableBonuses.contains(BonusType.BEAM)) {
            log.info("AI USED BEAM to get home");

            return AiDecision.beamAndMove( homePosition,
                    new AiDecision.MoveDecision(homePosition));
        }

        // use push fixed bonus if direct win is possible
        if (availableBonuses.contains(BonusType.PUSH_FIXED)) {
            AiDecision pushFixedTurn = bonusUsageDecider.tryUsePushFixedForDirectGoal(
                    game, myPlayerId, currentPos, availableBonuses, homePosition
            );
            if (pushFixedTurn != null) {
                log.info("USED PUSH_FIXED TO GET HOME!");
                return pushFixedTurn;
            }
        }

        // use swap bonus if direct win is possible
        if(availableBonuses.contains(BonusType.SWAP)) {
            AiDecision swapTurn = bonusUsageDecider.tryUseSwapIfItEnablesGoal(game, myPlayerId, availableBonuses, homePosition);
            if (swapTurn != null) {
                log.info("USED SWAP TO GET HOME!");
                return swapTurn;
            }
        }

        int bestScore = Integer.MIN_VALUE;
        int bestPushIndex = 1;
        Direction bestPushDirection = Direction.DOWN;
        int bestRotations = 0;

        Coordinates bestPosAfterPush = currentPos;
        Coordinates bestHomeAfterPush = homePosition;
        List<Coordinates> bestPath = Collections.emptyList();

        List<Integer> validIndices = boardSimulationHelper.getValidPushIndices(board);
        Direction[] directions = {Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT};

        boolean bestPushMovesAI = false;

        List<PushCandidate> push1CandidatesForPushTwice = new ArrayList<>();
        boolean canConsiderPushTwice = availableBonuses.contains(BonusType.PUSH_TWICE);

        for (int pushIndex : validIndices) {
            for (Direction pushDirection : directions) {

                if (!AiContractUtil.canShift(board, pushIndex, pushDirection)) continue;
                if (boardSimulationHelper.isReversePush(board, pushIndex, pushDirection)) continue;

                for (int rotations = 0; rotations < 4; rotations++) {

                    Direction[] rotatedEntrances = boardSimulationHelper.calculateRotatedEntrances(
                            board.getSpareTile().getEntrances(),
                            rotations
                    );

                    Coordinates newPlayerPos = boardSimulationHelper.calculatePositionAfterPush(
                            currentPos, pushIndex, pushDirection, board.getRows(), board.getCols()
                    );

                    boolean pushMovesAI = !newPlayerPos.equals(currentPos);

                    // Path to home after this virtual push
                    List<Coordinates> path = AiPathFindingService.findPathAfterVirtualPush(
                            board,
                            pushIndex, pushDirection, rotatedEntrances,
                            newPlayerPos, homePosition
                    );

                    boolean homeReachable = path != null && !path.isEmpty() && path.contains(homePosition);

                    int score;
                    if (homeReachable) {
                        int len = path.size() - 1;                 // steps
                        score = 1_000_000 - len * 100;             // dominates everything
                    } else {
                        int dist = boardSimulationHelper.manhattanDistance(newPlayerPos, homePosition);
                        score = 10_000 - dist * 100;
                        if (pushMovesAI) score += 50;              // break ties, avoid “stuck”
                    }

                    if (canConsiderPushTwice && !homeReachable) {
                        push1CandidatesForPushTwice.add(new PushCandidate(score, pushIndex, pushDirection, rotations));
                    }

                    boolean shouldUpdate = false;

                    if (score > bestScore) {
                        shouldUpdate = true;
                    } else if (score == bestScore) {
                        // Tie-breaker: prefer pushes that move the AI
                        if (pushMovesAI && !bestPushMovesAI) {
                            shouldUpdate = true;
                        }
                    }

                    if (shouldUpdate) {
                        bestScore = score;
                        bestPushIndex = pushIndex;
                        bestPushDirection = pushDirection;
                        bestRotations = rotations;
                        bestPushMovesAI = pushMovesAI;

                        bestPosAfterPush = newPlayerPos;
                        bestPath = (path != null) ? path : Collections.emptyList();
                    }
                }
            }
        }

        // Decide move target:
        Coordinates bestMoveTarget = currentPos;

        boolean havePath = bestPath != null && !bestPath.isEmpty() && bestPath.contains(homePosition);
        if (havePath) {
            Coordinates aiPosAfterPush = boardSimulationHelper.calculatePositionAfterPush(
                    currentPos,
                    bestPushIndex,
                    bestPushDirection,
                    board.getRows(),
                    board.getCols()
            );

            bestMoveTarget = moveTargetHelper.findBestUnoccupiedPositionAfterPush(
                    game,
                    bestPath,
                    homePosition,
                    myPlayerId,
                    bestPushIndex,
                    bestPushDirection,
                    aiPosAfterPush
            );
        } else {

            if (canConsiderPushTwice && !push1CandidatesForPushTwice.isEmpty()) {

                push1CandidatesForPushTwice.sort((a, b) -> {
                    int c = Integer.compare(b.score(), a.score());
                    if (c != 0) return c;
                    c = Integer.compare(a.rotations(), b.rotations());
                    if (c != 0) return c;
                    return Integer.compare(a.pushIndex(), b.pushIndex());
                });

                int k = Math.min(PUSH_TWICE_TOP_K, push1CandidatesForPushTwice.size());

                for (int i = 0; i < k; i++) {
                    PushCandidate cand = push1CandidatesForPushTwice.get(i);

                    AiDecision pushTwiceTurn = bonusUsageDecider.tryUsePushTwiceForDirectGoalAfterPlannedPush(
                            game,
                            myPlayerId,
                            currentPos,
                            homePosition,
                            availableBonuses,
                            cand.pushIndex(),
                            cand.pushDirection(),
                            cand.rotations(),
                            /*goalReachableAfterPlannedPush1*/ false
                    );

                    if (pushTwiceTurn != null) return pushTwiceTurn;
                }
            }


            // Try to collect bonus, if home is not possible and bonus is on path
            List<Coordinates> bonusPositions = boardSimulationHelper.findBonusPositions(board);

            Direction[] bestRotatedEntrances = boardSimulationHelper.calculateRotatedEntrances(
                    board.getSpareTile().getEntrances(),
                    bestRotations
            );

            Coordinates bonusMoveTarget = bonusCalculator.chooseBonusMoveTargetAfterChosenPush(
                    game, board, myPlayerId, bonusPositions,
                    bestPushIndex, bestPushDirection, bestRotatedEntrances,
                    bestPosAfterPush, homePosition
            );

            if (bonusMoveTarget != null) {
                log.info("Home not possible: Bot collects bonus which is on the way!");
                bestMoveTarget = bonusMoveTarget;
            } else {
                bestMoveTarget = bestPosAfterPush;
            }
        }

        return AiDecision.pushAndMove(new AiDecision.PushDecision(bestPushIndex, bestPushDirection, bestRotations),
                new AiDecision.MoveDecision(bestMoveTarget));
    }


    /**
     * Calculates an offensive decision for the special case where the target treasure is currently on the spare tile.
     *
     * <p><b>What it is for:</b> Since the treasure enters the board through the insertion point of the performed push,
     * this method evaluates pushes/rotations and targets the insertion coordinate returned by {@link #insertionPointCoordinates(GameBoard, int, Direction)}.</p>
     *
     * <p><b>How it works (as implemented):</b>
     * <ul>
     *   <li>Iterates over legal push indices and directions (excluding reverse pushes) and all 4 rotations.</li>
     *   <li>Computes the treasure position after the push as the insertion point coordinate for that push.</li>
     *   <li>Computes a path to that position using {@code AiPathFindingService.findPathAfterVirtualPush(...)}.</li>
     *   <li>Scores candidates by reachability and path length, otherwise by Manhattan distance.</li>
     *   <li>Selects a move target with occupancy checks after the push.</li>
     * </ul>
     *
     * @param game the current game state payload
     * @param board the current game board
     * @param myPlayerId the AI player's id
     * @param playerPosition the AI's current position (pre-push)
     * @return an {@link AiDecision} containing the selected push and move target
     */
    public AiDecision calculateMoveTreasureOnSpareTile(
            GameStateEventPayload game,
            GameBoard board,
            String myPlayerId,
            Coordinates playerPosition) {

        List<Integer> validIndices = boardSimulationHelper.getValidPushIndices(board);
        Direction[] directions = {Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT};

        int bestScore = Integer.MIN_VALUE;
        int bestPushIndex = 1;
        Direction bestPushDirection = Direction.DOWN;
        int bestRotations = 0;

        Coordinates bestTreasurePosAfterPush = null;
        List<Coordinates> bestPath = null;

        Coordinates bestAiPosAfterPush = playerPosition;

        for (int pushIndex : validIndices) {
            for (Direction pushDirection : directions) {

                if (!AiContractUtil.canShift(board, pushIndex, pushDirection)) continue;
                if (boardSimulationHelper.isReversePush(board, pushIndex, pushDirection)) continue;

                for (int rotations = 0; rotations < 4; rotations++) {

                    Direction[] rotatedEntrances = boardSimulationHelper.calculateRotatedEntrances(
                            board.getSpareTile().getEntrances(),
                            rotations
                    );

                    Coordinates treasurePosAfterPush = insertionPointCoordinates(
                            board, pushIndex, pushDirection
                    );

                    Coordinates newPlayerPos = boardSimulationHelper.calculatePositionAfterPush(
                            playerPosition, pushIndex, pushDirection, board.getRows(), board.getCols()
                    );

                    List<Coordinates> path = AiPathFindingService.findPathAfterVirtualPush(
                            board,
                            pushIndex,
                            pushDirection,
                            rotatedEntrances,
                            newPlayerPos,
                            treasurePosAfterPush
                    );

                    boolean treasureReachable = path != null && !path.isEmpty() && path.contains(treasurePosAfterPush);
                    int pathLen = treasureReachable ? (path.size() - 1) : Integer.MAX_VALUE;

                    int score;
                    if (treasureReachable) {
                        score = 1_000_000 - pathLen * 100;
                    } else {
                        int dist = boardSimulationHelper.manhattanDistance(newPlayerPos, treasurePosAfterPush);
                        score = 10_000 - dist * 100;
                        if (!newPlayerPos.equals(playerPosition)) score += 50;
                    }

                    if (score > bestScore) {
                        bestScore = score;
                        bestPushIndex = pushIndex;
                        bestPushDirection = pushDirection;
                        bestRotations = rotations;
                        bestTreasurePosAfterPush = treasurePosAfterPush;
                        bestPath = (path != null) ? path : Collections.emptyList();
                        bestAiPosAfterPush = newPlayerPos;
                    }
                }
            }
        }

        Coordinates moveTarget = playerPosition;
        if (bestTreasurePosAfterPush != null && bestPath != null && !bestPath.isEmpty()
                && bestPath.contains(bestTreasurePosAfterPush)) {

            if (!moveTargetHelper.isPositionOccupiedAfterPush(game, bestTreasurePosAfterPush, myPlayerId,
                    bestPushIndex, bestPushDirection)) {
                moveTarget = bestTreasurePosAfterPush;
            } else {
                moveTarget = moveTargetHelper.findBestUnoccupiedPositionAfterPush(
                        game, bestPath, bestTreasurePosAfterPush, myPlayerId,
                        bestPushIndex, bestPushDirection, bestAiPosAfterPush
                );
            }
        }

        return  AiDecision.pushAndMove(new AiDecision.PushDecision(bestPushIndex, bestPushDirection, bestRotations),
                new AiDecision.MoveDecision(moveTarget));
    }


    /**
     * Calculates an offensive decision for normal treasure hunting (target treasure is on the board).
     *
     * <p><b>What it is for:</b> Chooses a push (including spare tile rotation) and a move target to reach the target treasure
     * after the push, or to get closer when direct reachability is not possible.</p>
     *
     * <p><b>Push evaluation (as implemented):</b>
     * <ul>
     *   <li>Enumerates all legal pushes (valid indices, four directions) and rotations 0..3, excluding reverse pushes.</li>
     *   <li>Simulates the AI position and treasure position after the push.</li>
     *   <li>Computes a path after the virtual push and scores candidates primarily by (a) reachability and (b) path length.</li>
     *   <li>When the treasure is not reachable, uses Manhattan distance as a secondary score and applies deterministic tie-breakers
     *       (reachability, path length, whether the push moves the AI, proximity of push index to player axis, rotations, index).</li>
     * </ul>
     *
     * <p><b>Bonus attempts when treasure is not reachable (as implemented):</b>
     * <ul>
     *   <li>Tries BonusUsageDecider#tryUsePushFixedForDirectGoal(...).</li>
     *   <li>If BonusType#PUSH_TWICE} is available, tries PUSH_TWICE using the top-K push1 candidates.</li>
     *   <li>Tries BonusUsageDecider#tryUseSwapIfItEnablesGoal(...).</li>
     *   <li>Tries BonusUsageDecider#tryUseBeamForDirectTreasure(...).</li>
     * </ul>
     *
     * <p><b>Move target selection (as implemented):</b>
     * <ul>
     *   <li>If treasure is reachable: move directly to it if unoccupied after the push, otherwise choose the best unoccupied
     *       position along the path.</li>
     *   <li>If treasure is not reachable: choose the reachable position closest to the treasure using
     *       findClosestReachableToTreasureAfterVirtualPush(...) (with occupancy handling), and optionally override
     *       the move to an opportunistic bonus target via BonusCalculator#chooseBonusMoveTargetAfterChosenPush(...).</li>
     * </ul>
     *
     * @param game the current game state payload
     * @param board the current game board
     * @param myPlayerId the AI player's id
     * @param availableBonuses the set of bonuses available to the AI
     * @param playerPosition the AI's current position (pre-push)
     * @param treasurePosition the target treasure position (pre-push)
     * @return an {@link AiDecision} describing the chosen push and move (or a bonus-based decision when applicable)
     */
    public AiDecision calculateOffensiveMove(
            GameStateEventPayload game,
            GameBoard board,
            String myPlayerId,
            Set<BonusType> availableBonuses,
            Coordinates playerPosition,
            Coordinates treasurePosition) {

        int bestScore = Integer.MIN_VALUE;
        int bestPushIndex = 1;
        Direction bestPushDirection = Direction.DOWN;
        int bestRotations = 0;

        Coordinates bestMoveTarget = playerPosition;
        List<Coordinates> bestPath = Collections.emptyList();
        Coordinates bestTreasurePosAfterPush = treasurePosition;


        Direction[] bestRotatedEntrances = null;
        Coordinates bestAiPosAfterPush = playerPosition;

        boolean bestTreasureReachable = false;
        int bestPathLen = Integer.MAX_VALUE;
        boolean bestPushMovesAI = false;

        List<Coordinates> bonusPositions = boardSimulationHelper.findBonusPositions(board);

        List<Integer> validIndices = boardSimulationHelper.getValidPushIndices(board);
        Direction[] directions = {Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT};

        List<PushCandidate> push1CandidatesForPushTwice = new ArrayList<>();
        boolean canConsiderPushTwice = availableBonuses.contains(BonusType.PUSH_TWICE);

        for (int pushIndex : validIndices) {
            for (Direction pushDirection : directions) {

                if (!AiContractUtil.canShift(board, pushIndex, pushDirection)) {
                    continue;
                }

                if (boardSimulationHelper.isReversePush(board, pushIndex, pushDirection)) {
                    continue;
                }

                for (int rotations = 0; rotations < 4; rotations++) {

                    Direction[] rotatedEntrances = boardSimulationHelper.calculateRotatedEntrances(
                            board.getSpareTile().getEntrances(),
                            rotations
                    );

                    Coordinates newPlayerPos = boardSimulationHelper.calculatePositionAfterPush(
                            playerPosition,
                            pushIndex,
                            pushDirection,
                            board.getRows(),
                            board.getCols()
                    );

                    boolean pushMovesAI = !newPlayerPos.equals(playerPosition);

                    Coordinates treasurePosAfterPush = boardSimulationHelper.calculatePositionAfterPush(
                            treasurePosition,
                            pushIndex,
                            pushDirection,
                            board.getRows(),
                            board.getCols()
                    );

                    List<Coordinates> path = AiPathFindingService.findPathAfterVirtualPush(
                            board,
                            pushIndex,
                            pushDirection,
                            rotatedEntrances,
                            newPlayerPos,
                            treasurePosAfterPush
                    );

                    boolean treasureReachable =
                            path != null && !path.isEmpty() && path.contains(treasurePosAfterPush);

                    int pathLen = treasureReachable ? (path.size() - 1) : Integer.MAX_VALUE;


                    int score;
                    if (treasureReachable) {
                        score = 1_000_000 - pathLen * 100; // reach treasure most important, shorter path better
                    } else {
                        int dist = boardSimulationHelper.manhattanDistance(newPlayerPos, treasurePosAfterPush);
                        score = 10_000 - dist * 100; // treasure not in reach, try get closer

                        if (pushMovesAI) score += 50; // small bonus if push at least moves AI

                        if (canConsiderPushTwice) push1CandidatesForPushTwice.add(new PushCandidate(score, pushIndex, pushDirection, rotations));
                    }


                    boolean shouldUpdate = false;

                    if (score > bestScore) {
                        shouldUpdate = true;
                    }
                    else if (score == bestScore) {

                        // 1) safety: reachable better than not reachable
                        if (treasureReachable && !bestTreasureReachable) {
                            shouldUpdate = true;
                        }
                        else if (treasureReachable == bestTreasureReachable) {

                            // 2) if reachable: shortest path
                            if (treasureReachable && pathLen < bestPathLen) {
                                shouldUpdate = true;
                            }
                            // 3) not reachable: AI is moved is better
                            else if (!treasureReachable && pushMovesAI && !bestPushMovesAI) {
                                shouldUpdate = true;
                            }
                            // 4-6 not reachable, AI not moves
                            else if (!treasureReachable && !pushMovesAI && !bestPushMovesAI) {

                                int axisThis = (pushDirection == Direction.LEFT || pushDirection == Direction.RIGHT)
                                        ? playerPosition.getRow()
                                        : playerPosition.getColumn();
                                int distThis = Math.abs(pushIndex - axisThis);

                                int axisBest = (bestPushDirection == Direction.LEFT || bestPushDirection == Direction.RIGHT)
                                        ? playerPosition.getRow()
                                        : playerPosition.getColumn();
                                int distBest = Math.abs(bestPushIndex - axisBest);

                                // 4) closer index to player
                                if (distThis < distBest) {
                                    shouldUpdate = true;
                                }
                                // 5) fewer rotations
                                else if (distThis == distBest && rotations < bestRotations) {
                                    shouldUpdate = true;
                                }
                                // 6) smaller index
                                else if (distThis == distBest && rotations == bestRotations && pushIndex < bestPushIndex) {
                                    shouldUpdate = true;
                                }
                            }
                        }
                    }

                    if (shouldUpdate) {
                        bestScore = score;
                        bestPushIndex = pushIndex;
                        bestPushDirection = pushDirection;
                        bestRotations = rotations;
                        bestRotatedEntrances = rotatedEntrances;

                        bestPath = (path != null) ? path : Collections.emptyList();
                        bestTreasurePosAfterPush = treasurePosAfterPush;

                        bestTreasureReachable = treasureReachable;
                        bestPathLen = pathLen;
                        bestPushMovesAI = pushMovesAI;
                        bestAiPosAfterPush = newPlayerPos;
                    }
                }
            }
        }

        // Treasure not reachable, try bonus
        if (!bestTreasureReachable) {

            // 1) use push fixed if treasure is reachable after
            AiDecision pushFixedTurn = bonusUsageDecider.tryUsePushFixedForDirectGoal(
                    game, myPlayerId, playerPosition, availableBonuses, treasurePosition
            );

            if (pushFixedTurn != null) return pushFixedTurn;

            // 2) use push twice if treasure is reachable after
            if (canConsiderPushTwice && !push1CandidatesForPushTwice.isEmpty()) {

                push1CandidatesForPushTwice.sort((a, b) -> {
                    int c = Integer.compare(b.score(), a.score());
                    if (c != 0) return c;
                    c = Integer.compare(a.rotations(), b.rotations());
                    if (c != 0) return c;
                    return Integer.compare(a.pushIndex(), b.pushIndex());
                });

                int k = Math.min(PUSH_TWICE_TOP_K, push1CandidatesForPushTwice.size());

                for (int i = 0; i < k; i++) {
                    PushCandidate cand = push1CandidatesForPushTwice.get(i);

                    AiDecision pushTwiceTurn = bonusUsageDecider.tryUsePushTwiceForDirectGoalAfterPlannedPush(
                            game,
                            myPlayerId,
                            playerPosition,
                            treasurePosition,
                            availableBonuses,
                            cand.pushIndex(),
                            cand.pushDirection(),
                            cand.rotations(),
                            /*goalReachableAfterPlannedPush1*/ false
                    );

                    if (pushTwiceTurn != null) return pushTwiceTurn;
                }
            }

            // 3) use swap if treasure is reachable after
            AiDecision swapTurn = bonusUsageDecider.tryUseSwapIfItEnablesGoal(game, myPlayerId, availableBonuses, treasurePosition);
            if (swapTurn != null) return swapTurn;

            // 4) use beam to get to treasure
            AiDecision beamTurn = bonusUsageDecider.tryUseBeamForDirectTreasure(game, myPlayerId, availableBonuses, treasurePosition);
            if (beamTurn != null) return beamTurn;
        }

        // Safety
        if (bestRotatedEntrances == null) {
            bestRotatedEntrances = boardSimulationHelper.calculateRotatedEntrances(
                    board.getSpareTile().getEntrances(), bestRotations
            );
        }

        // MOVE TARGET
        if (bestTreasureReachable) {

            if (!moveTargetHelper.isPositionOccupiedAfterPush(game, bestTreasurePosAfterPush, myPlayerId,
                    bestPushIndex, bestPushDirection)) {
                bestMoveTarget = bestTreasurePosAfterPush;
                log.debug("Treasure is reachable: Bot collects treasure!");
            } else {
                bestMoveTarget = moveTargetHelper.findBestUnoccupiedPositionAfterPush(
                        game,
                        bestPath,
                        bestTreasurePosAfterPush,
                        myPlayerId,
                        bestPushIndex,
                        bestPushDirection,
                        bestAiPosAfterPush
                );
                log.debug("Treasure position is occupied: Bot moves to best unoccupied position!");
            }

        } else {
            Coordinates closestToTreasure = findClosestReachableToTreasureAfterVirtualPush(
                    board,
                    bestPushIndex,
                    bestPushDirection,
                    bestRotatedEntrances,
                    bestAiPosAfterPush,
                    bestTreasurePosAfterPush
            );

            if (closestToTreasure == null) {
                closestToTreasure = bestAiPosAfterPush;
            }

            if (!moveTargetHelper.isPositionOccupiedAfterPush(game, closestToTreasure, myPlayerId,
                    bestPushIndex, bestPushDirection)) {

                bestMoveTarget = closestToTreasure;

            } else {
                bestMoveTarget = moveTargetHelper.findBestUnoccupiedPositionAfterPush(
                        game,
                        bestPath,
                        closestToTreasure,
                        myPlayerId,
                        bestPushIndex,
                        bestPushDirection,
                        bestAiPosAfterPush
                );
            }

            if (bestPath == null || bestPath.isEmpty()) {
                bestMoveTarget = bestAiPosAfterPush;
            }

            Coordinates bonusMoveTarget = bonusCalculator.chooseBonusMoveTargetAfterChosenPush(
                    game, board, myPlayerId, bonusPositions,
                    bestPushIndex, bestPushDirection, bestRotatedEntrances,
                    bestAiPosAfterPush, bestTreasurePosAfterPush
            );


            if (bonusMoveTarget != null) {
                bestMoveTarget = bonusMoveTarget;
                log.info("Treasure not reachable: Bot collects bonus!");
            }

            log.debug("Treasure not reachable: Bot tries to get closer!");
        }

        return  AiDecision.pushAndMove(new AiDecision.PushDecision(bestPushIndex, bestPushDirection, bestRotations), new AiDecision.MoveDecision(bestMoveTarget));
    }



    /**
     * Finds the reachable coordinate (after a virtual push) that minimizes Manhattan distance to the treasure.
     *
     * <p><b>What it is for:</b> Used when the treasure is not directly reachable after the chosen push.
     * It computes the reachable set after the virtual push and selects the coordinate that is closest to {@code treasure}
     * according to {@link BoardSimulationHelper#manhattanDistance(Coordinates, Coordinates)}.</p>
     *
     * @param board the game board
     * @param pushIndex the push index used for the virtual push
     * @param pushDirection the push direction used for the virtual push
     * @param rotatedEntrances the rotated entrances for the spare tile used in the virtual push
     * @param from the AI position after the push
     * @param treasure the treasure position after the push
     * @return the reachable coordinate closest to {@code treasure} (or {@code from} if no better coordinate is found)
     */
    private Coordinates findClosestReachableToTreasureAfterVirtualPush(
            GameBoard board,
            int pushIndex,
            Direction pushDirection,
            Direction[] rotatedEntrances,
            Coordinates from,
            Coordinates treasure
    ) {
        Set<Coordinates> reachable = AiPathFindingService.getReachablePositionsAfterVirtualPush(
                board, pushIndex, pushDirection, rotatedEntrances, from
        );

        Coordinates best = from;
        int bestDist = boardSimulationHelper.manhattanDistance(from, treasure);

        for (Coordinates c : reachable) {
            int d = boardSimulationHelper.manhattanDistance(c, treasure);
            if (d < bestDist) {
                bestDist = d;
                best = c;
            }
        }

        return best;
    }

    /**
     * Checks whether the current target treasure is located on the spare tile.
     *
     * <p><b>What it is for:</b> Enables the special-case handling in calculateMoveTreasureOnSpareTile(...) by
     * detecting when the spare tile carries the target treasure id.</p>
     *
     * @param board the current game board
     * @param targetTreasure the target treasure id; may be {@code null}
     * @return {@code true} if the spare tile exists and carries the given treasure id; {@code false} otherwise
     */
    public boolean isTreasureOnSpareTile(GameBoard board, Integer targetTreasure) {
        if (targetTreasure == null) return false;
        Tile spare = board.getSpareTile();
        return spare != null && spare.getTreasure().getId() == targetTreasure;
    }

    /**
     * Computes the board coordinate where the spare tile is inserted for the given push action.
     *
     * <p><b>What it is for:</b> Used by calculateMoveTreasureOnSpareTile(...) to determine where a treasure on the
     * spare tile will appear on the board after performing the push.</p>
     *
     * @param board the game board (used for row/column bounds)
     * @param pushIndex the pushed row/column index
     * @param dir the push direction
     * @return the insertion-point coordinate for the spare tile for the given push direction and index
     */
    private Coordinates insertionPointCoordinates(GameBoard board, int pushIndex, Direction dir) {
        int rows = board.getRows();
        int cols = board.getCols();

        switch (dir) {
            case RIGHT: return AiContractUtil.coord(pushIndex, 0);
            case LEFT:  return AiContractUtil.coord(pushIndex, cols - 1);
            case DOWN:  return AiContractUtil.coord(0, pushIndex);
            case UP:    return AiContractUtil.coord(rows - 1, pushIndex);
        }
        return AiContractUtil.coord(0, 0);
    }
}

