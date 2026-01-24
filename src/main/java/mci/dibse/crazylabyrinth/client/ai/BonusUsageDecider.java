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


import labyrinth.contracts.models.BonusType;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Set;


// TODO: ADD JAVADOCS
@Component
public class BonusUsageDecider {

    private final AiPathFindingService AiPathFindingService;
    private final BoardSimulationHelper boardSimulationHelper;
    private final MoveTargetHelper moveTargetHelper;

    public BonusUsageDecider(BoardSimulationHelper boardSimulationHelper, MoveTargetHelper moveTargetHelper) {
        this.AiPathFindingService = new AiPathFindingService();
        this.boardSimulationHelper = boardSimulationHelper;
        this.moveTargetHelper = moveTargetHelper;
    }

    public AiDecision tryUseBeamForDirectTreasure(GameStateEventPayload game, String myPlayerId, Set<BonusType> availableBonuses, Coordinates treasurePos) {
        if (treasurePos == null) return null;

        if (!availableBonuses.contains(BonusType.BEAM)) {
            return null;
        }

        if (moveTargetHelper.isPositionOccupied(game, treasurePos, myPlayerId)) {
            return null;
        }

        System.out.println("AI USED BEAM BONUS");
        // TODO: hier könnte auch beam auf target und dann move der am besten ist (bei beam wird schon gesammelt), gleiches für swap

        return AiDecision.beamAndMove(treasurePos, new AiDecision.MoveDecision(treasurePos));
        /*
        return new AiDecision(
                AiDecision.BonusDecision.beam(treasurePos),
                new AiDecision.MoveDecision(treasurePos)
        );*/
    }


    public AiDecision tryUsePushFixedForDirectGoal(
            GameStateEventPayload game,
            String myPlayerId,
            Coordinates currentPos,
            Set<BonusType> availableBonuses,
            Coordinates goalPos
    ) {
        if (goalPos == null) return null;
        if (!availableBonuses.contains(BonusType.PUSH_FIXED)) return null;

        GameBoard board = game.getBoard();

        List<Integer> indices = boardSimulationHelper.getValidPushFixedIndices(board);
        Direction[] dirs = {Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT};

        int bestScore = Integer.MIN_VALUE;
        int bestIdx = -1;
        Direction bestDir = null;
        int bestRot = 0;

        Coordinates bestAiAfter = currentPos;
        Coordinates bestGoalAfter = goalPos;
        List<Coordinates> bestPath = null;

        for (int idx : indices) {
            for (Direction dir : dirs) {
                if (!AiContractUtil.canShift(board, idx, dir)) continue;

                // uses existing "last push" tracking from the board (good)
                if (boardSimulationHelper.isReversePush(board, idx, dir)) continue;

                for (int rot = 0; rot < 4; rot++) {
                    Direction[] rotatedEntrances =
                            boardSimulationHelper.calculateRotatedEntrances(board.getSpareTile().getEntrances(), rot);

                    Coordinates aiAfter = boardSimulationHelper.calculatePositionAfterPush(
                            currentPos, idx, dir, board.getRows(), board.getCols()
                    );

                    Coordinates goalAfter = boardSimulationHelper.calculatePositionAfterPush(
                            goalPos, idx, dir, board.getRows(), board.getCols()
                    );

                    List<Coordinates> path = AiPathFindingService.findPathAfterVirtualPush(
                            board, idx, dir, rotatedEntrances, aiAfter, goalAfter
                    );

                    boolean reachable = path != null && !path.isEmpty() && path.contains(goalAfter);
                    if (!reachable) continue;

                    int pathLen = path.size() - 1;
                    int score = 1_000_000 - pathLen * 100;

                    // deterministic: only update on strictly better
                    if (score > bestScore) {
                        bestScore = score;
                        bestIdx = idx;
                        bestDir = dir;
                        bestRot = rot;
                        bestAiAfter = aiAfter;
                        bestGoalAfter = goalAfter;
                        bestPath = path;
                    }
                }
            }
        }

        if (bestIdx == -1) return null;

        Coordinates moveTarget;
        if (!moveTargetHelper.isPositionOccupiedAfterPush(game, bestGoalAfter, myPlayerId, bestIdx, bestDir)) {
            moveTarget = bestGoalAfter;
        } else {
            moveTarget = moveTargetHelper.findBestUnoccupiedPositionAfterPush(
                    game, bestPath, bestGoalAfter, myPlayerId, bestIdx, bestDir, bestAiAfter
            );
        }

        System.out.println("AI USED PushedFix BONUS");
        return AiDecision.pushFixedThenPushAndMove(
                new AiDecision.PushDecision(bestIdx, bestDir, bestRot),
                new AiDecision.MoveDecision(moveTarget)
        );

        // deprecated
        /*
        return new AiDecision(
                AiDecision.BonusDecision.pushFixed(bestIdx, bestDir, bestRot),
                new AiDecision.MoveDecision(moveTarget)
        );*/
    }


    public AiDecision tryUseSwapIfItEnablesGoal(GameStateEventPayload game, String myPlayerId, Set<BonusType> availableBonuses, Coordinates goalPos) {
        if (goalPos == null) return null;
        if (!availableBonuses.contains(BonusType.SWAP)) return null;

        GameBoard board = game.getBoard();
        PlayerState[] players = game.getPlayers();

        PlayerState playerOnGoal = null;
        for (PlayerState p : players) {
            if (goalPos.equals(p.getCurrentPosition())) {
                playerOnGoal = p;
                break;
            }
        }

        String bestTargetPlayerId = null;
        int bestPathLen = Integer.MAX_VALUE;

        for (PlayerState other : players) {
            if (other.getPlayerInfo().getId().equals(myPlayerId)) continue;

            Coordinates otherPos = other.getCurrentPosition();

            // Case 1: Opponent is on target
            if (goalPos.equals(otherPos)) {
                bestTargetPlayerId = other.getPlayerInfo().getId();
                bestPathLen = 0;
                break;
            }

            // Case 2: Different opponent is on target
            if (playerOnGoal != null && !playerOnGoal.getPlayerInfo().getId().equals(other.getPlayerInfo().getId())) {
                continue;
            }

            // Case 3: Direct path from opponent position to target
            List<Coordinates> path = AiPathFindingService.findPath(board, otherPos, goalPos);
            if (path == null || path.isEmpty()) continue;
            if (!path.get(0).equals(otherPos)) continue;
            if (!path.get(path.size() - 1).equals(goalPos)) continue;

            int len = path.size() - 1;

            if (len < bestPathLen ||
                    (len == bestPathLen && bestTargetPlayerId != null &&
                            other.getPlayerInfo().getId().compareTo(bestTargetPlayerId) < 0)) {

                bestPathLen = len;
                bestTargetPlayerId = other.getPlayerInfo().getId();
            }
        }

        if (bestTargetPlayerId == null) return null;

        System.out.println("AI USED Swap BONUS");

        return AiDecision.swapAndMove(bestTargetPlayerId, new AiDecision.MoveDecision(goalPos));
        /*
        return new AiDecision(
                AiDecision.BonusDecision.swap(bestTargetPlayerId),
                new AiDecision.MoveDecision(goalPos)
        );*/
    }


    /**
     * Decides whether using PUSH_TWICE after the already planned first push would enable
     * reaching the goal in the same turn.
     *
     * Contract:
     * - This method does NOT re-evaluate push1 (already chosen by Offensive/Defensive calculator).
     * - It only decides whether to activate PUSH_TWICE and which second push (index/dir/rot) to do.
     * - Conservative V1: only return a decision if goal becomes reachable after two pushes.
     *
     * @param goalReachableAfterPlannedPush1 pass the already-known result from the calculator
     *                                      (avoid duplicate pathfinding for push1).
     */
    public AiDecision tryUsePushTwiceForDirectGoalAfterPlannedPush(
            GameStateEventPayload game,
            String myPlayerId,
            Coordinates originalPlayerPos,
            Coordinates originalGoalPos,
            Set<BonusType> availableBonuses,
            int push1Index,
            Direction push1Direction,
            int push1Rotations,
            boolean goalReachableAfterPlannedPush1
    ) {
        if (originalGoalPos == null) return null;
        if (!availableBonuses.contains(BonusType.PUSH_TWICE)) return null;

        // If goal already reachable after push #1, no need to spend PUSH_TWICE.
        if (goalReachableAfterPlannedPush1) return null;

        GameBoard board = game.getBoard();
        int rows = board.getRows();
        int cols = board.getCols();

        // Positions after push #1 (player + goal)
        Coordinates aiAfter1 = boardSimulationHelper.calculatePositionAfterPush(
                originalPlayerPos, push1Index, push1Direction, rows, cols
        );
        Coordinates goalAfter1 = boardSimulationHelper.calculatePositionAfterPush(
                originalGoalPos, push1Index, push1Direction, rows, cols
        );

        int bestScore = Integer.MIN_VALUE;
        int bestPush2Index = -1;
        Direction bestPush2Direction = null;
        int bestPush2Rotations = 0;
        Coordinates bestGoalAfter2 = null;
        List<Coordinates> bestPath = null;

        List<Integer> validIndices = boardSimulationHelper.getValidPushIndices(board);
        Direction[] directions = {Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT};

        for (int push2Index : validIndices) {
            for (Direction push2Direction : directions) {

                // Must be a legal shift on the *current* board
                if (!AiContractUtil.canShift(board, push2Index, push2Direction)) continue;

                // Avoid "undo" of push #1 (internal constraint)
                if (isUndoSecondPush(push1Index, push1Direction, push2Index, push2Direction)) continue;

                // Optional: you can also reuse the existing helper against last real push,
                // but note: after push1 is planned, the board's last push is still the real one.
                // So this is NOT the right check for undo; keep it only if you want to ban
                // pushing the reverse of the *last real push* again:
                // if (boardSimulationHelper.isReversePush(board, push2Index, push2Direction)) continue;

                for (int rot2 = 0; rot2 < 4; rot2++) {

                    // Positions after push #2 (apply to already shifted positions from push1)
                    Coordinates aiAfter2 = boardSimulationHelper.calculatePositionAfterPush(
                            aiAfter1, push2Index, push2Direction, rows, cols
                    );
                    Coordinates goalAfter2 = boardSimulationHelper.calculatePositionAfterPush(
                            goalAfter1, push2Index, push2Direction, rows, cols
                    );

                    // Path after two pushes (uses your PathFindingService's built-in virtual 2-push logic)
                    List<Coordinates> path = AiPathFindingService.findPathAfterTwoPushes(
                            board,
                            push1Index, push1Direction, push1Rotations,
                            push2Index, push2Direction, rot2,
                            aiAfter2, goalAfter2
                    );

                    boolean reachable = path != null && !path.isEmpty() && path.contains(goalAfter2);
                    if (!reachable) continue;

                    // Conservative: don't attempt landing on a spot that will be occupied
                    if (isOccupiedAfterTwoPushes(
                            game, goalAfter2, myPlayerId,
                            push1Index, push1Direction,
                            push2Index, push2Direction
                    )) {
                        continue;
                    }

                    int len = path.size() - 1;
                    int score = 1_000_000 - len * 100;

                    // Tie-breaker: deterministic order (keep first found unless strictly better)
                    if (score > bestScore) {
                        bestScore = score;
                        bestPush2Index = push2Index;
                        bestPush2Direction = push2Direction;
                        bestPush2Rotations = rot2;
                        bestGoalAfter2 = goalAfter2;
                        bestPath = path;
                    }
                }
            }
        }

        if (bestPush2Index == -1) return null;

        // Conservative V1 move: go directly to goalAfter2 (it's reachable).
        // If you later want collision-avoiding "best unoccupied on path", do it here.
        AiDecision.PushDecision push1 = new AiDecision.PushDecision(push1Index, push1Direction, push1Rotations);
        AiDecision.PushDecision push2 = new AiDecision.PushDecision(bestPush2Index, bestPush2Direction, bestPush2Rotations);

        System.out.println("AI USED PUSH TWICE BONUS");
        return AiDecision.pushTwiceThenTwoPushesAndMove(push1, push2, new AiDecision.MoveDecision(bestGoalAfter2));
        //deprecated
        /*
        return new AiDecision(
                push1,
                AiDecision.BonusDecision.pushTwice(),
                push2,
                new AiDecision.MoveDecision(bestGoalAfter2)
        );*/
    }

    private Direction opposite(Direction d) {
        return switch (d) {
            case UP -> Direction.DOWN;
            case DOWN -> Direction.UP;
            case LEFT -> Direction.RIGHT;
            case RIGHT -> Direction.LEFT;
        };
    }

    /**
     * Prevents the common "undo" case where the second push simply reverses the first push.
     * This is NOT the same as BoardSimulationHelper.isReversePush(...) which compares to the
     * last real push stored in the Board. Here we compare push2 vs push1 (internal).
     */
    private boolean isUndoSecondPush(int push1Index, Direction push1Dir, int push2Index, Direction push2Dir) {
        return push1Index == push2Index && push2Dir == opposite(push1Dir);
    }

    /**
     * Conservative occupancy check: after two pushes, do not move onto a position that will
     * be occupied by another player.
     */
    private boolean isOccupiedAfterTwoPushes(
            GameStateEventPayload game,
            Coordinates position,
            String excludePlayerId,
            int push1Index, Direction push1Direction,
            int push2Index, Direction push2Direction
    ) {
        GameBoard board = game.getBoard();
        int rows = board.getRows();
        int cols = board.getCols();

        for (PlayerState p : game.getPlayers()) {
            if (p.getPlayerInfo().getId().equals(excludePlayerId)) continue;

            Coordinates after1 = boardSimulationHelper.calculatePositionAfterPush(
                    p.getCurrentPosition(), push1Index, push1Direction, rows, cols
            );
            Coordinates after2 = boardSimulationHelper.calculatePositionAfterPush(
                    after1, push2Index, push2Direction, rows, cols
            );

            if (position.equals(after2)) return true;
        }
        return false;
    }




}


