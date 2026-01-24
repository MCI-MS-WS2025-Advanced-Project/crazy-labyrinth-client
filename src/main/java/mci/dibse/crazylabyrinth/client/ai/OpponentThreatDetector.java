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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Helper class for detecting opponent winning threats.
 * Analyzes if opponents can win in their next turn, either immediately or after one push.
 */
@Component
public class OpponentThreatDetector {

    private final AiPathFindingService aipathFindingService;
    private final BoardSimulationHelper boardSimulationHelper;

    private static final Logger log = LoggerFactory.getLogger(OpponentThreatDetector.class);

    public OpponentThreatDetector(BoardSimulationHelper boardSimulationHelper) {
        this.aipathFindingService = new AiPathFindingService();
        this.boardSimulationHelper = boardSimulationHelper;
    }


    /**
     * Checks if any opponent can win in their next turn,
     * considering that AI will make a push first.
     *
     * This method simulates ALL possible AI pushes and checks if
     * the opponent can win AFTER any of those pushes.
     *
     * @param game the current game state
     * @param board the game board
     * @return true if at least one opponent can win after ANY AI push
     */
    public boolean canOpponentWinNextTurn(GameStateEventPayload game, GameBoard board, String myPlayerId) {

        for (PlayerState opponent : game.getPlayers()) {
            if (opponent.getPlayerInfo().getId().equals(myPlayerId)) {
                continue; // Skip AI itself
            }

            Coordinates opponentPos = opponent.getCurrentPosition();
            Coordinates opponentHome = opponent.getHomePosition();

            // CASE 1: Opponent has collected all treasures
            Integer rem = opponent.getRemainingTreasureCount();
            if (rem != null && rem == 0) {
                System.out.println("\nOpponent has collected all targets!");
                System.out.println("   Opponent: " + opponent.getPlayerInfo().getId() + " at " + opponentPos);
                System.out.println("   Home: " + opponentHome);

                // Can opponent reach home NOW? (without any push)
                List<Coordinates> pathNow = aipathFindingService.findPath(
                        board, opponentPos, opponentHome
                );

                if (pathNow != null && !pathNow.isEmpty() && pathNow.contains(opponentHome)) {
                    log.warn("DANGER detected: Opponent can win now (without push)!");
                    return true;
                }

                // Check if opponent can win AFTER any AI push!
                log.warn("Checking: Can opponent win after ANY possible AI push?");

                List<Integer> validIndices = boardSimulationHelper.getValidPushIndices(board);
                Direction[] directions = {Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT};

                int totalChecked = 0;
                int dangerousAiPushes = 0;

                // Test all possible AI pushes
                for (int aiPushIndex : validIndices) {
                    for (Direction aiPushDirection : directions) {
                        if (!AiContractUtil.canShift(board, aiPushIndex, aiPushDirection)) {
                            continue;
                        }

                        if (boardSimulationHelper.isReversePush(board, aiPushIndex, aiPushDirection)) {
                            continue;
                        }

                        // Where is opponent after this AI push?
                        Coordinates opponentPosAfterAiPush = boardSimulationHelper.calculatePositionAfterPush(
                                opponentPos, aiPushIndex, aiPushDirection,
                                board.getRows(), board.getCols()
                        );

                        // Get the new spare tile (after AI's push)
                        Tile spareTileAfterAiPush = boardSimulationHelper.getTilePushedOut(
                                board, aiPushIndex, aiPushDirection
                        );


                        boolean opponentCanWinAfterThisAiPush = false;
                        // Test all rotations of the new spare tile
                        for (int oppRotations = 0; oppRotations < 4; oppRotations++) {
                            totalChecked++;

                            Direction[] rotatedEntrances = boardSimulationHelper.calculateRotatedEntrances(
                                    spareTileAfterAiPush.getEntrances(),
                                    oppRotations
                            );

                            // Test all possible opponent pushes
                            for (int oppPushIndex : validIndices) {
                                for (Direction oppPushDirection : directions) {
                                    if (!AiContractUtil.canShift(board,oppPushIndex, oppPushDirection)) {
                                        continue;
                                    }

                                    // Skip reverse of AI's push
                                    if (boardSimulationHelper.isReversePush(board, aiPushIndex, aiPushDirection)) {
                                        continue;
                                    }

                                    // Where is opponent after both pushes?
                                    Coordinates opponentPosAfterBothPushes =
                                            boardSimulationHelper.calculatePositionAfterPush(
                                                    opponentPosAfterAiPush,
                                                    oppPushIndex, oppPushDirection,
                                                    board.getRows(), board.getCols()
                                            );

                                    // Can opponent reach home?
                                    List<Coordinates> pathToHome =
                                            aipathFindingService.findPathAfterTwoPushes(
                                                    board,
                                                    aiPushIndex, aiPushDirection, 0,
                                                    oppPushIndex, oppPushDirection, oppRotations,
                                                    opponentPosAfterBothPushes,
                                                    opponentHome
                                            );

                                    if (pathToHome != null && !pathToHome.isEmpty() &&
                                            pathToHome.contains(opponentHome)) {
                                        log.warn("DANGER detected: After Bot push " + aiPushIndex +
                                                " and direction " + aiPushDirection + ", opponent can win with push " +
                                                oppPushIndex + " " + oppPushDirection + " (rotation: " + oppRotations + ")!");
                                        opponentCanWinAfterThisAiPush = true;
                                        break;
                                    }
                                }
                                if (opponentCanWinAfterThisAiPush) break;
                            }
                            if (opponentCanWinAfterThisAiPush) break;
                        }

                        if (opponentCanWinAfterThisAiPush) {
                            dangerousAiPushes++;
                        }
                    }
                }

                if (dangerousAiPushes > 0) {
                    log.warn("THREAT detected: Opponent can win after " +
                            dangerousAiPushes + " possible Bot pushes!");
                    log.warn("Total combinations checked: " + totalChecked);
                    return true;  // DANGER!
                }
            }
            else {
                // CASE 2: Opponent still needs last treasure
                // CASE 2 heuristic: opponent has 1 treasure left -> avoid giving free home path
                if (rem == null || rem != 1) {
                    continue;
                }

                List<Integer> validIndices = boardSimulationHelper.getValidPushIndices(board);
                Direction[] directions = {Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT};

                boolean foundThreat = false;

                // We simulate OUR push options (AI push), then check if opponent can reach HOME after that.
                for (int aiPushIndex : validIndices) {
                    for (Direction aiPushDirection : directions) {

                        if (!AiContractUtil.canShift(board, aiPushIndex, aiPushDirection)) continue;
                        if (boardSimulationHelper.isReversePush(board, aiPushIndex, aiPushDirection)) continue;

                        // opponent position after our push
                        Coordinates oppAfterAiPush = boardSimulationHelper.calculatePositionAfterPush(
                                opponentPos, aiPushIndex, aiPushDirection, board.getRows(), board.getCols()
                        );

                        // NEW spare tile after our push
                        Tile spareAfterAiPush = boardSimulationHelper.getTilePushedOut(board, aiPushIndex, aiPushDirection);

                        // ---- Threat 1: opponent has an immediate home path after our push (no opponent push needed)
                        // We must use the virtual board after OUR push with some spare-rotation.
                        // Conservative: if ANY rotation yields a path -> treat as threat.
                        for (int rot = 0; rot < 4; rot++) {

                            Direction[] rotatedEntrances = boardSimulationHelper.calculateRotatedEntrances(
                                    spareAfterAiPush.getEntrances(), rot
                            );

                            List<Coordinates> pathNow = aipathFindingService.findPathAfterVirtualPush(
                                    board, aiPushIndex, aiPushDirection, rotatedEntrances,
                                    oppAfterAiPush, opponentHome
                            );

                            if (pathNow != null && !pathNow.isEmpty() && pathNow.contains(opponentHome)) {
                                foundThreat = true;
                                break;
                            }
                        }
                        if (foundThreat) break;

                        // ---- Threat 2: opponent can make home reachable with ONE opponent push (+ move)
                        // Simulate opponent push on top of our virtual push.
                        for (int oppPushIndex : validIndices) {
                            for (Direction oppPushDirection : directions) {

                                if (!AiContractUtil.canShift(board, oppPushIndex, oppPushDirection)) continue;

                                // IMPORTANT: this reverse check is tricky in simulations; at least don't use aiPushIndex here
                                // If you still want it, check against opponent push itself:
                                // if (boardSimulationHelper.isReversePush(board, oppPushIndex, oppPushDirection)) continue;

                                for (int oppRot = 0; oppRot < 4; oppRot++) {

                                    // Can opponent reach home after two pushes?
                                    List<Coordinates> pathToHome = aipathFindingService.findPathAfterTwoPushes(
                                            board,
                                            aiPushIndex, aiPushDirection, 0,            // our rotation not modeled here; if you track it, pass it
                                            oppPushIndex, oppPushDirection, oppRot,
                                            oppAfterAiPush,
                                            opponentHome
                                    );

                                    if (pathToHome != null && !pathToHome.isEmpty() && pathToHome.contains(opponentHome)) {
                                        foundThreat = true;
                                        break;
                                    }
                                }
                                if (foundThreat) break;
                            }
                            if (foundThreat) break;
                        }

                        if (foundThreat) break;
                    }
                    if (foundThreat) break;
                }

                if (foundThreat) {
                    return true;
                }

            }
        }
        log.warn("No threat: Opponent cannot win after any push of bot!");
        return false;
    }

    /**
     * Checks if opponent can still win AFTER a given AI push.
     * Uses PERFECT simulation with findPathAfterTwoPushes().
     *
     * @param board the game board
     * @param aiPushIndex the AI's push index
     * @param aiPushDirection the AI's push direction
     * @param aiRotations number of 90-degree clockwise rotations
     * @param opponent the opponent to check
     * @param opponentPosAfterAiPush opponent's position after AI's push
     * @return true if opponent can still win after AI's push
     */
    public boolean canOpponentWinAfterThisPush(
            GameBoard board,
            int aiPushIndex,
            Direction aiPushDirection,
            int aiRotations,
            PlayerState opponent,
            Coordinates opponentPosAfterAiPush) {

        Coordinates opponentHome = opponent.getHomePosition();

        List<Integer> validIndices = boardSimulationHelper.getValidPushIndices(board);
        Direction[] directions = {Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT};

        int checkedCombinations = 0;
        int winningCombinations = 0;

        // Try all possible opponent pushes after the AI push
        for (int oppPushIndex : validIndices) {

            for (Direction oppPushDirection : directions) {
                // Skip reverse of AI's push
                if (!AiContractUtil.canShift(board, oppPushIndex, oppPushDirection)) {
                    continue;
                }
                if (boardSimulationHelper.isReversePush(board, aiPushIndex, aiPushDirection)) {
                    continue;
                }

                for (int oppRotations = 0; oppRotations < 4; oppRotations++) {
                    checkedCombinations++;

                    // Where would opponent be after BOTH pushes?
                    Coordinates opponentPosAfterBothPushes = boardSimulationHelper.calculatePositionAfterPush(
                            opponentPosAfterAiPush,
                            oppPushIndex,
                            oppPushDirection,
                            board.getRows(),
                            board.getCols()
                    );

                    List<Coordinates> pathToHome = aipathFindingService.findPathAfterTwoPushes(
                            board,
                            aiPushIndex, aiPushDirection, aiRotations,
                            oppPushIndex, oppPushDirection, oppRotations,
                            opponentPosAfterBothPushes,
                            opponentHome
                    );


                    // Can opponent reach home after both pushes?
                    if (pathToHome != null && !pathToHome.isEmpty() &&
                            pathToHome.contains(opponentHome)) {
                        winningCombinations++;
                        log.warn("CONFIRMED dangerous push: After Bot push " + aiPushIndex + " " + aiPushDirection +
                                " (rotations: " + aiRotations + "), opponent can win with push " +
                                oppPushIndex + " " + oppPushDirection +
                                " (rotations: " + oppRotations + ")");


                        // Early exit - One winning path is enough
                        return true;
                    }
                }
            }
        }

        log.warn("After bot push " + aiPushIndex + " " + aiPushDirection +
                " (rotations: " + aiRotations + "), checked " + checkedCombinations +
                " opponent combinations, " + winningCombinations + " would lead to victory");
        return false;
    }

    /**
     * Checks if a push would reverse another push.
     *
     * @param pushIndex1 first push index
     * @param pushDir1 first push direction
     * @param pushIndex2 second push index
     * @param pushDir2 second push direction
     * @return true if push2 would reverse push1
     */
    /*
    private boolean isReversePush(int pushIndex1, Direction pushDir1, int pushIndex2, Direction pushDir2) {
        if (pushIndex1 != pushIndex2) {
            return false;
        }

        return (pushDir1 == Direction.LEFT && pushDir2 == Direction.RIGHT) ||
                (pushDir1 == Direction.RIGHT && pushDir2 == Direction.LEFT) ||
                (pushDir1 == Direction.UP && pushDir2 == Direction.DOWN) ||
                (pushDir1 == Direction.DOWN && pushDir2 == Direction.UP);
    }*/

}