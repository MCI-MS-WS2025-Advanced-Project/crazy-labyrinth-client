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


/**
 * Decides whether and how the AI should use available bonus actions to directly enable or improve reaching a target.
 *
 * <p>Purpose in the AI flow:
 * <ul>
 *   <li>Evaluates specific bonus types (e.g., {@link BonusType#BEAM}, {@link BonusType#SWAP},
 *       {@link BonusType#PUSH_FIXED}, {@link BonusType#PUSH_TWICE}) against a provided goal/treasure context.</li>
 *   <li>Returns an {@link AiDecision} only when the corresponding bonus is available and the method's
 *       internal conditions are met.</li>
 * </ul>
 *
 * <p>This component uses {@link AiPathFindingService} for reachability checks, {@link BoardSimulationHelper}
 * for coordinate updates under virtual pushes, and {@link MoveTargetHelper} for occupancy-related decisions.
 */
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

    /**
     * Attempts to use the {@link BonusType#BEAM} bonus to directly collect a treasure by beaming to its position.
     *
     * <p>Returns {@code null} if:
     * <ul>
     *   <li>{@code treasurePos} is {@code null},</li>
     *   <li>{@link BonusType#BEAM} is not contained in {@code availableBonuses},</li>
     *   <li>or the treasure position is occupied by another player according to
     *       {@link MoveTargetHelper#isPositionOccupied(GameStateEventPayload, Coordinates, String)}.</li>
     * </ul>
     *
     * @param game the current game state payload (used for occupancy checks)
     * @param myPlayerId the AI player's id (excluded from occupancy checks)
     * @param availableBonuses the set of bonuses available to the AI
     * @param treasurePos the target treasure position
     * @return an {@link AiDecision} representing a beam action and a move to {@code treasurePos}, or {@code null} if not applicable
     */
    public AiDecision tryUseBeamForDirectTreasure(GameStateEventPayload game, String myPlayerId, Set<BonusType> availableBonuses, Coordinates treasurePos) {
        if (treasurePos == null) return null;

        if (!availableBonuses.contains(BonusType.BEAM)) {
            return null;
        }

        if (moveTargetHelper.isPositionOccupied(game, treasurePos, myPlayerId)) {
            return null;
        }

        System.out.println("AI USED BEAM BONUS");

        return AiDecision.beamAndMove(treasurePos, new AiDecision.MoveDecision(treasurePos));
    }


    /**
     * Attempts to use the {@link BonusType#PUSH_FIXED} bonus to make the goal reachable in the same turn.
     *
     * <p>What this method does:
     * <ul>
     *   <li>Enumerates indices from {@link BoardSimulationHelper#getValidPushFixedIndices(GameBoard)} and the four {@link Direction}s.</li>
     *   <li>Filters to legal shifts via {@link AiContractUtil#canShift(GameBoard, int, Direction)} and excludes reverse pushes
     *       via {@link BoardSimulationHelper#isReversePush(GameBoard, int, Direction)}.</li>
     *   <li>Tries all 4 rotations for the spare tile entrances and performs virtual reachability checks with
     *       {@code AiPathFindingService.findPathAfterVirtualPush(...)}.</li>
     *   <li>Scores candidates by path length and selects the best-scoring push/rotation.</li>
     *   <li>Selects the move target as the goal position after the chosen push if it is not occupied; otherwise chooses an
     *       alternative via {@link MoveTargetHelper#findBestUnoccupiedPositionAfterPush(GameStateEventPayload, List, Coordinates, String, int, Direction, Coordinates)}.</li>
     * </ul>
     *
     * <p>Returns {@code null} if the goal is {@code null}, the bonus is not available, or no push/rotation makes the goal reachable.</p>
     *
     * @param game the current game state payload
     * @param myPlayerId the AI player's id (used for occupancy checks)
     * @param currentPos the AI's current position (pre-push)
     * @param availableBonuses the set of bonuses available to the AI
     * @param goalPos the desired goal position (pre-push)
     * @return an {@link AiDecision} representing the push-fixed bonus usage and a move, or {@code null} if not applicable
     */
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

    }


    /**
     * Attempts to use the {@link BonusType#SWAP} bonus to enable reaching the given goal position.
     *
     * <p>This method searches for a target player to swap with. It considers:
     * <ul>
     *   <li>a player currently standing on {@code goalPos} (immediate best case),</li>
     *   <li>otherwise, players for which a path to {@code goalPos} exists according to {@code AiPathFindingService.findPath(...)}.</li>
     * </ul>
     *
     * <p>The selected target is the one with the shortest path length; ties are resolved deterministically by comparing
     * player ids lexicographically.</p>
     *
     * <p>Returns {@code null} if the goal is {@code null}, the bonus is not available, or no suitable target player is found.</p>
     *
     * @param game the current game state payload
     * @param myPlayerId the AI player's id (excluded from candidate selection)
     * @param availableBonuses the set of bonuses available to the AI
     * @param goalPos the goal position the AI wants to reach
     * @return an {@link AiDecision} representing the swap action and a move towards {@code goalPos}, or {@code null} if not applicable
     */
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
    }


    /**
     * Attempts to use the {@link BonusType#PUSH_TWICE} bonus to make the goal reachable after a planned first push.
     *
     * <p>Purpose of this method:
     * <ul>
     *   <li>Given an already planned first push (index/direction/rotation), decide whether spending PUSH_TWICE
     *       and selecting a second push can make the goal reachable within the same turn.</li>
     * </ul>
     *
     * <p>Contract implemented by this method:
     * <ul>
     *   <li>Does not re-evaluate or change the provided first push parameters.</li>
     *   <li>Returns {@code null} when the goal is already reachable after push #1
     *       (as indicated by {@code goalReachableAfterPlannedPush1}).</li>
     *   <li>Searches a second push among {@link BoardSimulationHelper#getValidPushIndices(GameBoard)} and the four {@link Direction}s.</li>
     *   <li>Excludes illegal shifts via {@link AiContractUtil#canShift(GameBoard, int, Direction)} and excludes a direct undo of push #1
     *       via {@link #isUndoSecondPush(int, Direction, int, Direction)}.</li>
     *   <li>Uses {@code AiPathFindingService.findPathAfterTwoPushes(...)} to check reachability after both pushes.</li>
     *   <li>Applies a conservative occupancy filter via {@link #isOccupiedAfterTwoPushes(GameStateEventPayload, Coordinates, String, int, Direction, int, Direction)}.</li>
     * </ul>
     *
     * @param game the current game state payload
     * @param myPlayerId the AI player's id (excluded from occupancy checks)
     * @param originalPlayerPos the AI's original position (pre-push #1)
     * @param originalGoalPos the goal position (pre-push #1)
     * @param availableBonuses the set of bonuses available to the AI
     * @param push1Index the already planned first push index
     * @param push1Direction the already planned first push direction
     * @param push1Rotations the already planned first push rotation count (0..3)
     * @param goalReachableAfterPlannedPush1 whether the goal is already reachable after push #1
     * @return an {@link AiDecision} representing PUSH_TWICE usage (two pushes + move), or {@code null} if not applicable
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

                // Must be a legal shift on the current board
                if (!AiContractUtil.canShift(board, push2Index, push2Direction)) continue;

                if (isUndoSecondPush(push1Index, push1Direction, push2Index, push2Direction)) continue;


                for (int rot2 = 0; rot2 < 4; rot2++) {


                    Coordinates aiAfter2 = boardSimulationHelper.calculatePositionAfterPush(
                            aiAfter1, push2Index, push2Direction, rows, cols
                    );
                    Coordinates goalAfter2 = boardSimulationHelper.calculatePositionAfterPush(
                            goalAfter1, push2Index, push2Direction, rows, cols
                    );

                    List<Coordinates> path = AiPathFindingService.findPathAfterTwoPushes(
                            board,
                            push1Index, push1Direction, push1Rotations,
                            push2Index, push2Direction, rot2,
                            aiAfter2, goalAfter2
                    );

                    boolean reachable = path != null && !path.isEmpty() && path.contains(goalAfter2);
                    if (!reachable) continue;


                    if (isOccupiedAfterTwoPushes(
                            game, goalAfter2, myPlayerId,
                            push1Index, push1Direction,
                            push2Index, push2Direction
                    )) {
                        continue;
                    }

                    int len = path.size() - 1;
                    int score = 1_000_000 - len * 100;

                    // Tie-breaker: deterministic order
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

        AiDecision.PushDecision push1 = new AiDecision.PushDecision(push1Index, push1Direction, push1Rotations);
        AiDecision.PushDecision push2 = new AiDecision.PushDecision(bestPush2Index, bestPush2Direction, bestPush2Rotations);

        System.out.println("AI USED PUSH TWICE BONUS");
        return AiDecision.pushTwiceThenTwoPushesAndMove(push1, push2, new AiDecision.MoveDecision(bestGoalAfter2));

    }

    /**
     * Returns the opposite direction for the given {@link Direction}.
     *
     * @param d a direction
     * @return the opposite direction
     */
    private Direction opposite(Direction d) {
        return switch (d) {
            case UP -> Direction.DOWN;
            case DOWN -> Direction.UP;
            case LEFT -> Direction.RIGHT;
            case RIGHT -> Direction.LEFT;
        };
    }


    /**
     * Checks whether the second push would directly undo the first push by using the same index and the opposite direction.
     *
     * <p>This is an internal constraint comparing push #2 against push #1. It is distinct from
     * {@link BoardSimulationHelper#isReversePush(GameBoard, int, Direction)}, which compares against the board's stored last push.</p>
     *
     * @param push1Index the first push index
     * @param push1Dir the first push direction
     * @param push2Index the second push index
     * @param push2Dir the second push direction
     * @return {@code true} if push #2 undoes push #1; {@code false} otherwise
     */
    private boolean isUndoSecondPush(int push1Index, Direction push1Dir, int push2Index, Direction push2Dir) {
        return push1Index == push2Index && push2Dir == opposite(push1Dir);
    }


    /**
     * Conservatively checks whether a given position would be occupied by another player after applying two pushes.
     *
     * <p>For each player (excluding {@code excludePlayerId}), this method applies
     * {@link BoardSimulationHelper#calculatePositionAfterPush(Coordinates, int, Direction, int, int)} twice:
     * first with push #1 and then with push #2. If any resulting position equals {@code position}, the position is treated as occupied.</p>
     *
     * @param game the current game state payload
     * @param position the position to test (interpreted as the post-two-push position)
     * @param excludePlayerId a player id to exclude from the occupancy check (typically the AI itself)
     * @param push1Index first push index
     * @param push1Direction first push direction
     * @param push2Index second push index
     * @param push2Direction second push direction
     * @return {@code true} if any other player ends up on {@code position} after both pushes; {@code false} otherwise
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


