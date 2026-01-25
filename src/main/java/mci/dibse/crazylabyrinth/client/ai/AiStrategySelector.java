package mci.dibse.crazylabyrinth.client.ai;


import labyrinth.contracts.models.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;



import java.util.List;
import java.util.Set;

/**
 * Main AI strategy orchestrator.
 * Implements the 3-priority decision logic:
 * 1. WINNING: Can AI win? → Go home
 * 2. DEFENSE: Can opponent win? → Block them
 * 3. OFFENSE: Normal play → Collect treasures / Get Bonus
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

    public AiDecision calculateFullTurn(GameStateEventPayload gameState, String myPlayerId, Treasure myCurrentTreasure) {
        if (gameState == null || myPlayerId == null) return null;

        GameBoard board = gameState.getBoard();
        PlayerState me = findPlayer(gameState.getPlayers(), myPlayerId);
        Coordinates myHomePosition = me.getHomePosition();
        Set<BonusType> availableBonuses = AiContractUtil.toBonusSet(me.getAvailableBonuses());
        Coordinates currentPos = me.getCurrentPosition();
        log.warn("AI input: myPos={}", currentPos.getRow() + "," + currentPos.getColumn());
        log.warn("AI TREASURE: " + myCurrentTreasure);
        Coordinates treasurePos = boardSimulationHelper.findTreasurePosition(board, myCurrentTreasure);
        log.warn("AI input: treasurePos={}", treasurePos);


        if (board == null || me == null) return null;

        // Debug last push
        PushActionInfo lastPush = board.getLastPush();
        if (lastPush != null) {
            log.debug("LastPush idx={}, dir={}", lastPush.getRowOrColIndex(), lastPush.getDirection());
        }

        Coordinates myPos = me.getCurrentPosition();
        if (myPos == null) return null;

        // 1) WINNING / HOME
        Treasure target = myCurrentTreasure;

        if (AiContractUtil.isHomeTarget(target)) {
            log.debug("AI USED calculateMoveToHome");
            return offensiveMoveCalculator.calculateMoveToHome(gameState, myPlayerId, availableBonuses, myPos, myHomePosition);
        }

        // 2) DEFENSE
        if (opponentThreatDetector.canOpponentWinNextTurn(gameState, board, myPlayerId)) {
            log.debug("AI USED calculateDefensiveMove");
            AiDecision def = defensiveMoveCalculator.calculateDefensiveMove(gameState, board, myPlayerId, availableBonuses, myPos, myHomePosition, myCurrentTreasure);
            if (def != null) return def;
        }

        // 3) OFFENSE: Treasure sammeln


        if (treasurePos == null) {
            if (offensiveMoveCalculator.isTreasureOnSpareTile(board, myCurrentTreasure)) {
                log.debug("AI USED calculateMoveTreasureOnSpareTile");
                return offensiveMoveCalculator.calculateMoveTreasureOnSpareTile(gameState, board, myPlayerId, myPos);
            }

            // harter fallback: legal push + stay (keine Crashes)
            AiDecision.PushDecision push = findAnyLegalPush(board);
            return AiDecision.pushAndMove(push, new AiDecision.MoveDecision(myPos));
        }

        log.debug("AI USED calculateOffensiveMove");
        return offensiveMoveCalculator.calculateOffensiveMove(gameState, board, myPlayerId, availableBonuses, myPos, treasurePos);
    }

    private PlayerState findPlayer(PlayerState[] players, String id) {
        if (players == null) return null;
        for (PlayerState p : players) {
            if (p != null && p.getPlayerInfo() != null && id.equals(p.getPlayerInfo().getId())) {
                return p;
            }
        }
        return null;
    }

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




