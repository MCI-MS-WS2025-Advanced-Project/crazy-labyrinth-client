package mci.dibse.crazylabyrinth.client.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mci.dibse.crazylabyrinth.client.domain.context.PushTileCommandRecord;
import mci.dibse.crazylabyrinth.client.view.viewmodel.GameViewModel;
import org.springframework.stereotype.Component;

import labyrinth.contracts.models.*;

import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClientAIExecutor {

    private final GameViewModel gameViewModel;

    private AiDecision pending;
    private boolean bonusSent;
    private int pushesSent;
    private boolean moveSent;

    private TurnState lastTurnState;

    public boolean hasPendingDecision() {
        return pending != null;
    }

    public void executeDecision(AiDecision decision) {
        if (decision == null) return;

        this.pending = decision;
        this.bonusSent = false;
        this.pushesSent = 0;
        this.moveSent = false;

        // try immediately with current known state (if any)
        TurnState current = safeCurrentTurnState();
        if (current != null) {
            onTurnStateChanged(current);
        }
    }

    public void onTurnStateChanged(TurnState newState) {
        this.lastTurnState = newState;

        if (pending == null || newState == null) return;

        switch (newState) {
            case WAITING_FOR_PUSH -> handleWaitingForPush();
            case WAITING_FOR_MOVE -> handleWaitingForMove();
        }
    }

    private void handleWaitingForPush() {
        if (pending == null) return;

        // 1) Send bonus activation/execution (once)
        if (!bonusSent) {
            sendBonusIfNeeded();
            bonusSent = true;

            // If BEAM/SWAP => bonus itself is the "push-phase action", no pushes needed
            BonusType bt = pending.hasBonus() ? pending.getBonus().getBonusType() : null;
            if (bt == BonusType.BEAM || bt == BonusType.SWAP) {
                // Now wait for WAITING_FOR_MOVE from server to send move.
                return;
            }
        }

        // 2) Send pushes (0..2)
        if (pushesSent < pending.getPushCount()) {
            AiDecision.PushDecision push = pending.getPushes().get(pushesSent);
            sendPush(push);
            pushesSent++;
            return;
        }

        // no more push actions to do in push phase => wait for server to switch to move phase
    }

    private void handleWaitingForMove() {
        if (pending == null) return;
        if (moveSent) return;

        if (pending.hasMove()) {
            Coordinates target = pending.getMove().getTarget();
            sendMove(target);
        } else {
            log.warn("AI: no move in pending decision - finishing.");
        }

        moveSent = true;
        finishDecision();
    }

    private void finishDecision() {
        pending = null;
        bonusSent = false;
        pushesSent = 0;
        moveSent = false;
    }

    private void sendBonusIfNeeded() {
        if (!pending.hasBonus()) return;

        BonusType bt = pending.getBonus().getBonusType();
        switch (bt) {
            case PUSH_TWICE -> {
                log.debug("AI: send USE_PUSH_TWICE");
                gameViewModel.sendUsePushTwiceCommand();
            }
            case BEAM -> {
                Coordinates beamTarget = pending.getBonus().getBeamTarget();
                log.debug("AI: send USE_BEAM target=({}, {})", beamTarget.getRow(), beamTarget.getColumn());
                gameViewModel.sendUseBeamCommand(beamTarget.getRow(), beamTarget.getColumn());
            }
            case SWAP -> {
                String targetId = pending.getBonus().getSwapTargetPlayerId();
                log.debug("AI: send USE_SWAP targetPlayer={}", targetId);
                gameViewModel.sendUseSwapCommand(targetId);
            }
            case PUSH_FIXED -> {
                // Depending on your protocol:
                // - If you have a dedicated command -> call it here
                // - If not, we encode it in the push CommandType below (USE_PUSH_FIXED)


                log.debug("AI: PUSH_FIXED activation handled via push CommandType (USE_PUSH_FIXED).");
            }
        }
    }

    private void sendPush(AiDecision.PushDecision push) {
        Objects.requireNonNull(push, "push");

        GameStateEventPayload gs = gameViewModel.getLastGameState();
        Direction[] baseEntrances = null;
        if (gs != null && gs.getBoard() != null && gs.getBoard().getSpareTile() != null) {
            baseEntrances = gs.getBoard().getSpareTile().getEntrances();
        }

        Direction[] rotated = rotateEntrances(baseEntrances, push.getRotations());

        // deprecated
        /*
        CommandType cmdType = CommandType.PUSH_TILE;
        if (pending.hasBonus() && pending.getBonus().getBonusType() == BonusType.PUSH_FIXED) {
            cmdType = CommandType.USE_PUSH_FIXED;
        }*/

        PushTileCommandRecord record = new PushTileCommandRecord(
                push.getPushIndex(),
                push.getPushDirection(),
                rotated,
                CommandType.PUSH_TILE
        );

        log.debug("AI: send push idx={} dir={} rot={}",
                 push.getPushIndex(), push.getPushDirection(), push.getRotations());

        gameViewModel.pushTileCommand(record);
    }

    private void sendMove(Coordinates target) {
        if (target == null) return;
        log.debug("AI: send move target=({}, {})", target.getRow(), target.getColumn());
        gameViewModel.movePawnCommand(target.getRow(), target.getColumn());
    }

    private TurnState safeCurrentTurnState() {
        try {
            return gameViewModel.getCurrentTurnState();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Rotates tile entrances clockwise 'rotations' times.
     * If base is null -> returns null (server may accept null, otherwise you should default it).
     */
    private Direction[] rotateEntrances(Direction[] base, int rotations) {
        if (base == null) return null;

        int r = ((rotations % 4) + 4) % 4;
        if (r == 0) return base;

        Direction[] out = base.clone();
        for (int i = 0; i < r; i++) {
            for (int j = 0; j < out.length; j++) {
                out[j] = rotateOnce(out[j]);
            }
        }
        return out;
    }

    private Direction rotateOnce(Direction d) {
        if (d == null) return null;
        return switch (d) {
            case UP -> Direction.RIGHT;
            case RIGHT -> Direction.DOWN;
            case DOWN -> Direction.LEFT;
            case LEFT -> Direction.UP;
        };
    }
}
