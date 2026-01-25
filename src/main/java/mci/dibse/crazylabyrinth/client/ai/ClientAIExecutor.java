package mci.dibse.crazylabyrinth.client.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mci.dibse.crazylabyrinth.client.domain.context.PushTileCommandRecord;
import mci.dibse.crazylabyrinth.client.view.viewmodel.GameViewModel;
import org.springframework.stereotype.Component;

import labyrinth.contracts.models.*;

import java.util.Objects;


/**
 * Client-side executor for {@link AiDecision} objects.
 *
 * <p><b>What it is for:</b> Bridges the AI decision logic with the client UI/network layer by translating a precomputed
 * {@link AiDecision} into concrete commands sent through {@link GameViewModel}.</p>
 *
 * <p><b>How it works:</b> The executor reacts to the server-driven turn state machine ({@link TurnState}) and sends the
 * required commands in the correct phase:
 * <ul>
 *   <li>{@link TurnState#WAITING_FOR_PUSH}: optionally send bonus activation and then send 0..2 pushes</li>
 *   <li>{@link TurnState#WAITING_FOR_MOVE}: send the move once (preferably recomputed from latest client game state)</li>
 * </ul>
 *
 * <p><b>Statefulness:</b> Keeps an internal pending decision and flags that track which parts (bonus/pushes/move) have
 * already been sent.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClientAIExecutor {

    private final GameViewModel gameViewModel;

    private AiDecision pending;
    private boolean bonusSent;
    private int pushesSent;
    private boolean moveSent;


    private final BoardSimulationHelper boardSimulationHelper;
    private final AiPathFindingService pathFinding = new AiPathFindingService();


    private TurnState lastTurnState;

    /**
     * Indicates whether there is an {@link AiDecision} currently pending execution.
     *
     * @return {@code true} if a decision is pending, otherwise {@code false}
     */
    public boolean hasPendingDecision() {
        return pending != null;
    }


    /**
     * Starts executing the given {@link AiDecision}.
     *
     * <p><b>What it is for:</b> Initializes the executor for a new decision by storing it as {@code pending} and resetting
     * the internal execution flags. If the current {@link TurnState} can be read from {@link GameViewModel}, execution is
     * attempted immediately by calling {@link #onTurnStateChanged(TurnState)}.</p>
     *
     * @param decision the decision to execute; ignored if {@code null}
     */
    public void executeDecision(AiDecision decision) {
        if (decision == null) return;

        // prevent resetting execution if a decision is already in progress
        if (this.pending != null) {
            log.debug("AI: executeDecision ignored - decision already pending (pushesSent={}, moveSent={}, bonusSent={})",
                    pushesSent, moveSent, bonusSent);
            return;
        }

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



    /**
     * Informs the executor that the server-side {@link TurnState} has changed.
     *
     * <p><b>What it is for:</b> Continues execution of a pending decision according to the new phase:
     * {@link TurnState#WAITING_FOR_PUSH} triggers the push-phase handler and {@link TurnState#WAITING_FOR_MOVE} triggers the
     * move-phase handler.</p>
     *
     * @param newState the new turn state; may be {@code null}
     */
    public void onTurnStateChanged(TurnState newState) {
        this.lastTurnState = newState;

        if (pending == null || newState == null) return;

        switch (newState) {
            case WAITING_FOR_PUSH -> handleWaitingForPush();
            case WAITING_FOR_MOVE -> handleWaitingForMove();
        }
    }


    /**
     * Handles the push phase (server state {@link TurnState#WAITING_FOR_PUSH}).
     * <p>
     * The executor:
     * <ol>
     *   <li>sends bonus activation once (if needed)</li>
     *   <li>sends the next pending push (0..2) including server rotations</li>
     * </ol>
     * For {@link BonusType#BEAM} and {@link BonusType#SWAP}, no pushes are sent in this phase.
     * </p>
     */
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


    /**
     * Handles the move phase (server state {@link TurnState#WAITING_FOR_MOVE}).
     * <p>
     * The executor sends the move exactly once. The move target is preferably recomputed from the latest
     * known game state to reflect state changes caused by bonuses and pushes. If recomputation fails,
     * the decision's stored target is used as fallback.
     * </p>
     */
    private void handleWaitingForMove() {
        if (pending == null) return;
        if (moveSent) return;

        if (pushesSent < pending.getPushCount()) {
            log.debug("AI: server is WAITING_FOR_MOVE but pushes remaining ({}/{}) -> waiting / not moving yet",
                    pushesSent, pending.getPushCount());
            return;
        }

        Coordinates target = recomputeMoveTargetFromLatestState();
        if (target != null) {
            sendMove(target);
        } else if (pending.hasMove()) {
            // fallback (should rarely happen)
            sendMove(pending.getMove().getTarget());
        } else {
            log.debug("AI: no move possible - finishing.");
        }


        moveSent = true;
        finishDecision();
    }


    /**
     * Clears the current pending decision and resets all execution flags.
     * <p>
     * After calling this method, the executor is ready for the next {@link AiDecision}.
     * </p>
     */
    private void finishDecision() {
        pending = null;
        bonusSent = false;
        pushesSent = 0;
        moveSent = false;
    }


    /**
     * Sends the bonus activation/execution command once if the pending decision contains a bonus.
     * <p>
     * Protocol specifics:
     * <ul>
     *   <li>{@link BonusType#PUSH_TWICE}: sends the activation command before the first push</li>
     *   <li>{@link BonusType#BEAM}: sends the beam command with the selected target</li>
     *   <li>{@link BonusType#SWAP}: sends the swap command with the selected player id</li>
     *   <li>{@link BonusType#PUSH_FIXED}: handled via {@link CommandType#USE_PUSH_FIXED} in the push command</li>
     * </ul>
     * </p>
     */
    private void sendBonusIfNeeded() {
        if (!pending.hasBonus()) return;

        BonusType bt = pending.getBonus().getBonusType();
        switch (bt) {
            case PUSH_TWICE -> {
                log.info("AI: send USE_PUSH_TWICE");
                gameViewModel.sendUsePushTwiceCommand();
            }
            case BEAM -> {
                Coordinates beamTarget = pending.getBonus().getBeamTarget();
                log.info("AI: send USE_BEAM target=({}, {})", beamTarget.getRow(), beamTarget.getColumn());
                gameViewModel.sendUseBeamCommand(beamTarget.getRow(), beamTarget.getColumn());
            }
            case SWAP -> {
                String targetId = pending.getBonus().getSwapTargetPlayerId();
                log.info("AI: send USE_SWAP targetPlayer={}", targetId);
                gameViewModel.sendUseSwapCommand(targetId);
            }
            case PUSH_FIXED -> {

                log.info("AI: PUSH_FIXED activation handled via push CommandType (USE_PUSH_FIXED).");
            }
        }
    }


    /**
     * Sends one push operation to the server, including the required spare-tile rotations.
     * <p>
     * Rotations are sent as repeated rotate commands to the server before issuing the push command.
     * If the pending decision uses {@link BonusType#PUSH_FIXED}, the push is encoded as
     * {@link CommandType#USE_PUSH_FIXED}; otherwise {@link CommandType#PUSH_TILE} is used.
     * </p>
     *
     * @param push the push decision to execute
     * @throws NullPointerException if {@code push} is {@code null}
     */
    private void sendPush(AiDecision.PushDecision push) {
        Objects.requireNonNull(push, "push");

        int rotations = ((push.getRotations() % 4) + 4) % 4;

        // 1) rotate on server
        for (int i = 0; i < rotations; i++) {
            log.info("AI: send ROTATE_TILE ({}/{})", i + 1, rotations);
            gameViewModel.onRightRotationOfSpareTile();
        }

        // 2) push
        CommandType cmdType = CommandType.PUSH_TILE;
        if (pending.hasBonus() && pending.getBonus().getBonusType() == BonusType.PUSH_FIXED) {
            cmdType = CommandType.USE_PUSH_FIXED;
        }

        PushTileCommandRecord record = new PushTileCommandRecord(
                push.getPushIndex(),
                push.getPushDirection(),
                null,      // payload can’t carry this anyway
                cmdType
        );

        log.info("AI: send push idx={} dir={} rot={}",
                push.getPushIndex(), push.getPushDirection(), rotations);

        gameViewModel.pushTileCommand(record);
    }

    /**
     * Sends the move command for the given target coordinate.
     *
     * @param target the target coordinate; ignored if {@code null}
     */
    private void sendMove(Coordinates target) {
        if (target == null) return;
        log.info("AI: send move target=({}, {})", target.getRow(), target.getColumn());
        gameViewModel.movePawnCommand(target.getRow(), target.getColumn());
    }

    /**
     * Reads the current turn state from {@link GameViewModel} in a safe way.
     * <p>
     * Any exception is swallowed and {@code null} is returned to avoid breaking execution startup.
     * </p>
     *
     * @return the current {@link TurnState}, or {@code null} if not available
     */
    private TurnState safeCurrentTurnState() {
        try {
            return gameViewModel.getCurrentTurnState();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Recomputes the move target based on the latest known game state from {@link GameViewModel}.
     * <p>
     * The method:
     * <ol>
     *   <li>determines the AI player's current position</li>
     *   <li>resolves the current target (home vs. treasure position)</li>
     *   <li>computes a path to the target</li>
     *   <li>selects the farthest reachable step on the path that is not occupied by another player</li>
     * </ol>
     * </p>
     *
     * <p>
     * Returns {@code null} if required game state is missing. If pathfinding fails, returns the current position
     * as a "stay" target.
     * </p>
     *
     * @return a recomputed move target coordinate, or {@code null} if recomputation is not possible
     */
    private Coordinates recomputeMoveTargetFromLatestState() {
        GameStateEventPayload gs = gameViewModel.getLastGameState();
        if (gs == null || gs.getBoard() == null) return null;

        String myId = gameViewModel.getClientPlayer().getId();
        PlayerState me = findPlayer(gs.getPlayers(), myId);
        if (me == null || me.getCurrentPosition() == null) return null;

        Coordinates myPos = me.getCurrentPosition();

        // current target treasure (already updated after BEAM/SWAP/PUSH)
        Integer targetTreasure = gameViewModel.getCurrentTargetTreasureForAi().getId();
        if (targetTreasure == null) return myPos;

        Coordinates targetPos;

        // home target?
        if (AiContractUtil.isHomeTarget(targetTreasure)) {
            targetPos = me.getHomePosition();
            if (targetPos == null) return myPos;
        } else {
            targetPos = boardSimulationHelper.findTreasurePosition(gs.getBoard(), targetTreasure);
            if (targetPos == null) return myPos;
        }

        // If occupied, choose best free on path
        var path = pathFinding.findPath(gs.getBoard(), myPos, targetPos);
        if (path == null || path.isEmpty()) return myPos;

        // pick farthest reachable step that is not occupied
        for (int i = path.size() - 1; i >= 0; i--) {
            Coordinates c = path.get(i);
            if (!isOccupied(gs, c, myId)) {
                return c;
            }
        }

        return myPos;
    }


    /**
     * Finds a player state by player id.
     *
     * @param players array of player states
     * @param id player id to search for
     * @return the matching {@link PlayerState}, or {@code null} if not found
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
     * Checks whether the given coordinate is currently occupied by another player.
     *
     * @param gs current game state payload
     * @param pos coordinate to check
     * @param myId id of the AI player (ignored for occupation checks)
     * @return {@code true} if occupied by a different player, otherwise {@code false}
     */
    private boolean isOccupied(GameStateEventPayload gs, Coordinates pos, String myId) {
        if (gs.getPlayers() == null || pos == null) return false;
        for (PlayerState p : gs.getPlayers()) {
            if (p == null || p.getPlayerInfo() == null) continue;
            if (myId.equals(p.getPlayerInfo().getId())) continue;
            Coordinates pp = p.getCurrentPosition();
            if (pp != null && pp.equals(pos)) return true;
        }
        return false;
    }
}
