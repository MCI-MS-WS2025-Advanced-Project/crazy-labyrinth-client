package mci.dibse.crazylabyrinth.client.ai;


import labyrinth.contracts.models.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


import java.util.Set;

/**
 * Selects an {@link AiDecision} for a full AI turn based on the current {@link GameStateEventPayload}.
 *
 * <p>This component orchestrates the decision by delegating to collaborating helpers/calculators.
 * The implementation evaluates (in order):
 * <ul>
 *   <li>home/winning logic when the current target represents the home target,</li>
 *   <li>defensive logic when the opponent is detected as able to win next turn,</li>
 *   <li>otherwise offensive logic to pursue the current treasure target.</li>
 * </ul>
 *
 * <p>Decision building and board-related checks are delegated to
 * {@link BoardSimulationHelper}, {@link OpponentThreatDetector},
 * {@link DefensiveMoveCalculator}, {@link OffensiveMoveCalculator}, and {@link AiContractUtil}.
 */
@Component
public class AiStrategySelector {

    private final BoardSimulationHelper boardSimulationHelper;
    private final OpponentThreatDetector opponentThreatDetector;
    private final DefensiveMoveCalculator defensiveMoveCalculator;
    private final OffensiveMoveCalculator offensiveMoveCalculator;
    private final Logger log = LoggerFactory.getLogger(AiStrategySelector.class);


    public AiStrategySelector(
            BoardSimulationHelper boardSimulationHelper,
            OpponentThreatDetector opponentThreatDetector,
            DefensiveMoveCalculator defensiveMoveCalculator,
            OffensiveMoveCalculator offensiveMoveCalculator
    ) {
        this.boardSimulationHelper = boardSimulationHelper;
        this.opponentThreatDetector = opponentThreatDetector;
        this.defensiveMoveCalculator = defensiveMoveCalculator;
        this.offensiveMoveCalculator = offensiveMoveCalculator;
    }


    /**
     * Calculates the AI's decision for a full turn (push and move, and potentially bonus usage)
     * for the given game state and AI player id.
     *
     * @param gameState the current game state payload received from the client contracts; may be {@code null}
     * @param myPlayerId the id of the AI player; may be {@code null}
     * @param myCurrentTreasure the current target identifier as provided to the AI; may be {@code null}
     * @return the computed {@link AiDecision}, or {@code null} if required inputs are missing
     *         (e.g., {@code gameState}, {@code myPlayerId}, board/player position), or if the player cannot be resolved
     */
    public AiDecision calculateFullTurn(GameStateEventPayload gameState, String myPlayerId, Integer myCurrentTreasure) {
        if (gameState == null || myPlayerId == null) return null;

        GameBoard board = gameState.getBoard();
        PlayerState me = findPlayer(gameState.getPlayers(), myPlayerId);
        if (board == null || me == null) return null;

        Coordinates myHomePosition = me.getHomePosition();
        Set<BonusType> availableBonuses = AiContractUtil.toBonusSet(me.getAvailableBonuses());

        log.info("AI TREASURE: " + myCurrentTreasure);

        Coordinates myPos = me.getCurrentPosition();
        if (myPos == null) return null;


        // 1) WINNING / HOME
        if (AiContractUtil.isHomeTarget(myCurrentTreasure)) {
            log.info("Bot tries to get home!");
            return offensiveMoveCalculator.calculateMoveToHome(gameState, myPlayerId, availableBonuses, myPos, myHomePosition);
        }


        Coordinates treasurePos = boardSimulationHelper.findTreasurePosition(board, myCurrentTreasure);


        // Debug last push
        PushActionInfo lastPush = board.getLastPush();
        if (lastPush != null) {
            log.debug("LastPush idx={}, dir={}", lastPush.getRowOrColIndex(), lastPush.getDirection());
        }


        // 2) DEFENSE
        if (opponentThreatDetector.canOpponentWinNextTurn(gameState, board, myPlayerId)) {
            log.info("Bot tries to defend the opponent!");
            AiDecision def = defensiveMoveCalculator.calculateDefensiveMove(gameState, board, myPlayerId, availableBonuses, myPos, myHomePosition, myCurrentTreasure);
            if (def != null) return def;
        }

        // 3) OFFENSE: Treasure sammeln
        if (treasurePos == null) {
            if (offensiveMoveCalculator.isTreasureOnSpareTile(board, myCurrentTreasure)) {
                log.info("Bot tries to hunt a treasure!");
                return offensiveMoveCalculator.calculateMoveTreasureOnSpareTile(gameState, board, myPlayerId, myPos);
            }

            // harter fallback: legal push + stay (keine Crashes)
            AiDecision.PushDecision push = findAnyLegalPush(board);
            return AiDecision.pushAndMove(push, new AiDecision.MoveDecision(myPos));
        }

        log.debug("AI USED calculateOffensiveMove");
        return offensiveMoveCalculator.calculateOffensiveMove(gameState, board, myPlayerId, availableBonuses, myPos, treasurePos);
    }


    /**
     * Finds the {@link PlayerState} with the given player id in the provided array.
     *
     * @param players array of player states; may be {@code null}
     * @param id the player id to search for; may be {@code null}
     * @return the matching {@link PlayerState}, or {@code null} if not found or if inputs are {@code null}
     */
    private PlayerState findPlayer(PlayerState[] players, String id) {
        if (players == null) return null;
        for (PlayerState p : players) {
            if (p != null && p.getPlayerInfo() != null && id.equals(p.getPlayerInfo().getId())) {
                return p;
            }
        }
        return null;
    }


    /**
     * Returns an arbitrary legal push decision for the given board.
     *
     * <p>The method iterates over indices and the four cardinal {@link Direction}s and returns the first push that:
     * <ul>
     *   <li>is allowed by {@link AiContractUtil#canShift(GameBoard, int, Direction)}, and</li>
     *   <li>is not considered a reverse push according to {@link BoardSimulationHelper#isReversePush(GameBoard, int, Direction)}.</li>
     * </ul>
     *
     * <p>If no push is found during the scan, a fixed fallback decision is returned.
     *
     * @param board the current {@link GameBoard}
     * @return a push decision that is legal according to the checks above, or a fixed fallback push decision
     */
    private AiDecision.PushDecision findAnyLegalPush(GameBoard board) {
        log.warn("AI USED findAnyLegalPush");
        Direction[] dirs = {Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT};

        int rows = board.getRows() != null ? board.getRows() : 7;
        int cols = board.getCols() != null ? board.getCols() : 7;
        int maxIndex = Math.max(rows, cols);

        for (int idx = 0; idx < maxIndex; idx++) {
            for (Direction d : dirs) {
                if (!AiContractUtil.canShift(board, idx, d)) continue;
                if (boardSimulationHelper.isReversePush(board, idx, d)) continue;
                return new AiDecision.PushDecision(idx, d, 0);
            }
        }

        return new AiDecision.PushDecision(1, Direction.DOWN, 0);
    }

}




