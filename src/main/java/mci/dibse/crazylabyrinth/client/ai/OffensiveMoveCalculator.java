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
import labyrinth.contracts.models.Treasure;

import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

// TODO: JAVADOC
@Component
public class OffensiveMoveCalculator {

    private final AiPathFindingService AiPathFindingService;
    private final BoardSimulationHelper boardSimulationHelper;
    private final BonusCalculator bonusCalculator;
    private final BonusUsageDecider bonusUsageDecider;
    private final MoveTargetHelper moveTargetHelper;

    private static final Logger log = LoggerFactory.getLogger(OffensiveMoveCalculator.class);

    public OffensiveMoveCalculator(BoardSimulationHelper boardSimulationHelper, BonusCalculator bonusCalculator, BonusUsageDecider bonusUsageDecider, MoveTargetHelper moveTargetHelper) {
        this.AiPathFindingService = new AiPathFindingService();
        this.boardSimulationHelper = boardSimulationHelper;
        this.bonusCalculator = bonusCalculator;
        this.bonusUsageDecider = bonusUsageDecider;
        this.moveTargetHelper = moveTargetHelper;

    }

    // TODO: JAVADOC richtig
    /**
     * Calculates offensive move when no treasures are left (return home).
     */
    public AiDecision calculateMoveToHome(GameStateEventPayload game, String myPlayerId, Set<BonusType> availableBonuses, Coordinates currentPos, Coordinates homePosition) {
        GameBoard board = game.getBoard();

        // use beam bonus if possible and win
        if (availableBonuses.contains(BonusType.BEAM)) {
            log.debug("AI USED BEAM to get home");
            // deprecated
            return AiDecision.beamAndMove( homePosition,
                    new AiDecision.MoveDecision(homePosition));
            /*
            return new AiDecision(
                    AiDecision.BonusDecision.beam(homePosition),
                    new AiDecision.MoveDecision(homePosition)
            );*/
        }

        // use push fixed bonus if direct win is possible
        if (availableBonuses.contains(BonusType.PUSH_FIXED)) {
            AiDecision pushFixedTurn = bonusUsageDecider.tryUsePushFixedForDirectGoal(
                    game, myPlayerId, currentPos, availableBonuses, homePosition
            );
            if (pushFixedTurn != null) {
                log.warn("USED PUSH_FIXED TO GET HOME!");
                return pushFixedTurn;
            }
        }

        // use swap bonus if direct win is possible
        if(availableBonuses.contains(BonusType.SWAP)) {
            AiDecision swapTurn = bonusUsageDecider.tryUseSwapIfItEnablesGoal(game, myPlayerId, availableBonuses, homePosition);
            if (swapTurn != null) {
                log.warn("USED SWAP TO GET HOME!");
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

            // Use push twice if home is reachable after
            boolean homeReachable = bestPath != null && !bestPath.isEmpty() && bestPath.contains(homePosition);

            if (!homeReachable && availableBonuses.contains(BonusType.PUSH_TWICE)) {
                AiDecision pushTwiceTurn = bonusUsageDecider.tryUsePushTwiceForDirectGoalAfterPlannedPush(
                        game,
                        myPlayerId,
                        currentPos,
                        homePosition,
                        availableBonuses,
                        bestPushIndex,
                        bestPushDirection,
                        bestRotations,
                        /*goalReachableAfterPlannedPush1*/ false
                );
                if (pushTwiceTurn != null) return pushTwiceTurn;
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
                log.warn("Home not possible: Bot collects bonus which is on the way!");
                bestMoveTarget = bonusMoveTarget;
            } else {
                bestMoveTarget = bestPosAfterPush;
            }
        }

        return AiDecision.pushAndMove(new AiDecision.PushDecision(bestPushIndex, bestPushDirection, bestRotations),
                new AiDecision.MoveDecision(bestMoveTarget));
        // deprecated
        /*
        return new AiDecision(
                new AiDecision.PushDecision(bestPushIndex, bestPushDirection, bestRotations),
                new AiDecision.MoveDecision(bestMoveTarget)
        );*/
    }


    // TODO JAVADOC
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
        // deprecated
        /*
        return new AiDecision(
                new AiDecision.PushDecision(bestPushIndex, bestPushDirection, bestRotations),
                new AiDecision.MoveDecision(moveTarget)
        );*/
    }


    // TODO: JAVADOC Anpassen
    /**
     * Calculates offensive move for normal treasure hunting (greedy algorithm).
     *
     * @param game the game state
     * @param board the game board
     * @param myPlayerId the AI player
     * @param playerPosition the current player position
     * @param treasurePosition the target treasure position (BEFORE push)
     * @return the best offensive decision
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
                    }

                    boolean shouldUpdate = false;

                    if (score > bestScore) {
                        shouldUpdate = true;
                    } else if (score == bestScore) {

                        // 1) safety: reachable better than not reachable
                        if (treasureReachable && !bestTreasureReachable) {
                            shouldUpdate = true;
                        } else if (treasureReachable == bestTreasureReachable) {

                            // 2) if reachable: shortest path
                            if (treasureReachable && pathLen < bestPathLen) {
                                shouldUpdate = true;
                            }

                            // 3) not reachable: AI is moved is better
                            else if (!treasureReachable && pushMovesAI && !bestPushMovesAI) {
                                shouldUpdate = true;
                            }

                            // 4) not reachable and AI not moved: use the one with less rotations
                            else if (rotations < bestRotations) {
                                shouldUpdate = true;
                            }

                            // 5) not reachable, AI not moved, same rotations: use the one with smaller index
                            else if (pushIndex < bestPushIndex) {
                                shouldUpdate = true;
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
            if (!bestTreasureReachable && availableBonuses.contains(BonusType.PUSH_TWICE)) {
                AiDecision pushTwiceTurn = bonusUsageDecider.tryUsePushTwiceForDirectGoalAfterPlannedPush(
                        game,
                        myPlayerId,
                        playerPosition,
                        treasurePosition,
                        availableBonuses,
                        bestPushIndex,
                        bestPushDirection,
                        bestRotations,
                        /*goalReachableAfterPlannedPush1*/ false
                );
                if (pushTwiceTurn != null) return pushTwiceTurn;
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
                log.warn("Treasure is reachable: Bot collects treasure!");
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
                log.warn("Treasure position is occupied: Bot moves to best unoccupied position!");
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
                log.warn("Treasure not reachable: Bot collects bonus!");
            }

            log.warn("Treasure not reachable: Bot tries to get closer!");
        }



        return  AiDecision.pushAndMove(new AiDecision.PushDecision(bestPushIndex, bestPushDirection, bestRotations), new AiDecision.MoveDecision(bestMoveTarget));
        //deprecated
        /*
        AiDecision.PushDecision pushDecision = new AiDecision.PushDecision(
                bestPushIndex,
                bestPushDirection,
                bestRotations
        );

        AiDecision.MoveDecision moveDecision = new AiDecision.MoveDecision(bestMoveTarget);

        return new AiDecision(pushDecision, moveDecision);*/
    }



    // TODO: JAVADOC
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

    // TODO JAVADOC
    public boolean isTreasureOnSpareTile(GameBoard board, Treasure targetTreasure) {
        if (targetTreasure == null) return false;
        Tile spare = board.getSpareTile();
        return spare != null && spare.getTreasure() == targetTreasure;
    }

    //TODO JAVADOC
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

