package mci.dibse.crazylabyrinth.client.ai;

import labyrinth.contracts.models.GameStateEventPayload;
import labyrinth.contracts.models.PlayerState;

import labyrinth.contracts.models.GameBoard;
import labyrinth.contracts.models.Coordinates;
import labyrinth.contracts.models.Direction;
import labyrinth.contracts.models.Tile;

import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Detects whether an opponent has a winning threat on their next turn.
 *
 * <p><b>What it is for:</b> This component is used by the AI to decide whether it must switch into a defensive mode.
 * It analyzes opponents and checks if they could reach their home position either immediately or after simulated pushes.</p>
 *
 * <p><b>How it works (as implemented):</b>
 * <ul>
 *   <li>Iterates all players except the AI player.</li>
 *   <li>Handles two cases based on {@code remainingTreasureCount}:
 *     <ul>
 *       <li><b>Case 1:</b> {@code rem != null && rem == 0} — opponent has collected all treasures and may win by reaching home.</li>
 *       <li><b>Case 2 (heuristic):</b> {@code rem == 1} — opponent is one treasure away; the code treats certain newly enabled
 *           home paths as threats.</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <p>This class relies on {@link AiPathFindingService} for reachability checks and {@link BoardSimulationHelper} for
 * coordinate updates under virtual pushes.</p>
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
     * Checks whether any opponent has a winning threat for their next turn, taking into account possible AI pushes.
     *
     * <p><b>What it is for:</b> Returns {@code true} if the method detects that an opponent could win either:
     * <ul>
     *   <li>immediately (without any push) in the "all treasures collected" case, or</li>
     *   <li>after at least one possible AI push (based on the simulations implemented in this method), or</li>
     *   <li>via the additional "one treasure left" heuristic simulation.</li>
     * </ul>
     *
     * <p><b>Case 1: opponent has all treasures ({@code rem == 0}):</b>
     * <ul>
     *   <li>First checks if the opponent can already reach home on the current board via {@link AiPathFindingService#findPath(GameBoard, Coordinates, Coordinates)}.</li>
     *   <li>Then simulates all legal AI pushes (valid indices, four directions, excluding reverse pushes).</li>
     *   <li>For each simulated AI push, simulates opponent rotations and opponent pushes and checks reachability to home using
     *       {@code AiPathFindingService.findPathAfterTwoPushes(...)}.</li>
     *   <li>If any combination yields a path to home, the method returns {@code true}.</li>
     * </ul>
     *
     * <p><b>Case 2: heuristic for {@code rem == 1}:</b>
     * <ul>
     *   <li>Simulates AI pushes and checks if the opponent can reach home after that push with:
     *     <ul>
     *       <li>a direct path after the AI push (using {@code findPathAfterVirtualPush(...)} across any spare rotation), or</li>
     *       <li>a two-push sequence (AI push + opponent push) using {@code findPathAfterTwoPushes(...)}.</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * <p><b>Notes (as visible in code):</b>
     * <ul>
     *   <li>This method prints and logs diagnostic messages during detection.</li>
     *   <li>The method uses {@link BoardSimulationHelper#getTilePushedOut(GameBoard, int, Direction)} to obtain the spare tile
     *       that results from an AI push, and rotates its entrances via {@link BoardSimulationHelper#calculateRotatedEntrances(Direction[], int)}.</li>
     * </ul>
     *
     * @param game the current game state payload
     * @param board the current game board
     * @param myPlayerId the AI player's id (excluded from opponent evaluation)
     * @return {@code true} if at least one opponent winning threat is detected; {@code false} otherwise
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

                // Can opponent reach home NOW? (without any push)
                List<Coordinates> pathNow = aipathFindingService.findPath(
                        board, opponentPos, opponentHome
                );

                if (pathNow != null && !pathNow.isEmpty() && pathNow.contains(opponentHome)) {
                    log.info("DANGER detected: Opponent can win now (without push)!");
                    return true;
                }

                // Check if opponent can win AFTER any AI push!
                log.info("Checking: Can opponent win after ANY possible AI push?");

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
                                        log.debug("DANGER detected: After Bot push " + aiPushIndex +
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
                    log.info("THREAT detected: Opponent can win after " +
                            dangerousAiPushes + " possible Bot pushes!");
                    return true;  // DANGER!
                }
            }
            else {
                // CASE 2: Opponent still needs last treasure
                if (rem == null || rem != 1) {
                    continue;
                }

                List<Integer> validIndices = boardSimulationHelper.getValidPushIndices(board);
                Direction[] directions = {Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT};

                boolean foundThreat = false;

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

                        // Threat 1: opponent has an immediate home path after our push (no opponent push needed)
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
                        for (int oppPushIndex : validIndices) {
                            for (Direction oppPushDirection : directions) {

                                if (!AiContractUtil.canShift(board, oppPushIndex, oppPushDirection)) continue;

                                for (int oppRot = 0; oppRot < 4; oppRot++) {

                                    // Can opponent reach home after two pushes?
                                    List<Coordinates> pathToHome = aipathFindingService.findPathAfterTwoPushes(
                                            board,
                                            aiPushIndex, aiPushDirection, 0,
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
        log.info("No threat: Opponent cannot win after any push of bot!");
        return false;
    }

    /**
     * Checks whether a specific opponent can win on their next turn after a given AI push.
     *
     * <p><b>What it is for:</b> Used as a reusable simulation step by defensive logic (e.g. when evaluating whether a
     * candidate AI push "allows" an opponent to win next).</p>
     *
     * <p><b>How it works (as implemented):</b>
     * <ul>
     *   <li>Iterates all legal opponent pushes (valid indices, four directions).</li>
     *   <li>For each opponent push, tries all 4 opponent rotations.</li>
     *   <li>Computes the opponent position after both pushes using
     *       {@link BoardSimulationHelper#calculatePositionAfterPush(Coordinates, int, Direction, int, int)}.</li>
     *   <li>Checks for a winning path to the opponent's home using {@code AiPathFindingService.findPathAfterTwoPushes(...)}.</li>
     *   <li>Returns {@code true} immediately on the first winning combination.</li>
     * </ul>
     *
     * @param board the game board
     * @param aiPushIndex the AI's push index (first push)
     * @param aiPushDirection the AI's push direction (first push)
     * @param aiRotations the number of 90-degree clockwise rotations applied for the AI push
     * @param opponent the opponent player state to evaluate
     * @param opponentPosAfterAiPush the opponent position after applying the AI push
     * @return {@code true} if the opponent can reach home after a possible opponent push/rotation; {@code false} otherwise
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
                        log.debug("CONFIRMED dangerous push: After Bot push " + aiPushIndex + " " + aiPushDirection +
                                " (rotations: " + aiRotations + "), opponent can win with push " +
                                oppPushIndex + " " + oppPushDirection +
                                " (rotations: " + oppRotations + ")");


                        // Early exit - One winning path is enough
                        return true;
                    }
                }
            }
        }

        log.debug("After bot push " + aiPushIndex + " " + aiPushDirection +
                " (rotations: " + aiRotations + "), checked " + checkedCombinations +
                " opponent combinations, " + winningCombinations + " would lead to victory");
        return false;
    }

}