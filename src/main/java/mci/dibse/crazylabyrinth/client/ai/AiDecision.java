package mci.dibse.crazylabyrinth.client.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import labyrinth.contracts.models.BonusType;
import labyrinth.contracts.models.Coordinates;
import labyrinth.contracts.models.Direction;

/**
 * AI decision for one logical turn.
 *
 * Model:
 * - Optional bonus (BEAM/SWAP/PUSH_FIXED/PUSH_TWICE)
 * - 0..2 pushes (normal: 1, push_twice: 2, beam/swap: 0)
 * - optional move (usually always set)
 */
public final class AiDecision {

    private final BonusDecision bonus;          // may be null
    private final List<PushDecision> pushes;    // never null (may be empty)
    private final MoveDecision move;            // may be null (but your flow normally sets it)

    private AiDecision(BonusDecision bonus, List<PushDecision> pushes, MoveDecision move) {
        this.bonus = bonus;
        this.pushes = pushes == null ? Collections.emptyList() : List.copyOf(pushes);
        this.move = move;
    }

    public BonusDecision getBonus() {
        return bonus;
    }

    public boolean hasBonus() {
        return bonus != null;
    }

    public List<PushDecision> getPushes() {
        return pushes;
    }

    public int getPushCount() {
        return pushes.size();
    }

    public boolean hasMove() {
        return move != null && move.getTarget() != null;
    }

    public MoveDecision getMove() {
        return move;
    }

    // ---------- Factory helpers ----------

    /** Normal turn: push once + move */
    public static AiDecision pushAndMove(PushDecision push, MoveDecision move) {
        Objects.requireNonNull(push, "push");
        return new AiDecision(null, List.of(push), move);
    }

    /** Beam: bonus beam (no push) + move */
    public static AiDecision beamAndMove(Coordinates beamTarget, MoveDecision move) {
        Objects.requireNonNull(beamTarget, "beamTarget");
        return new AiDecision(BonusDecision.beam(beamTarget), Collections.emptyList(), move);
    }

    /** Swap: bonus swap (no push) + move */
    public static AiDecision swapAndMove(String swapTargetPlayerId, MoveDecision move) {
        Objects.requireNonNull(swapTargetPlayerId, "swapTargetPlayerId");
        return new AiDecision(BonusDecision.swap(swapTargetPlayerId), Collections.emptyList(), move);
    }

    /**
     * Push fixed: activate PUSH_FIXED and then do one push + move.
     * (If your server uses "push commandType USE_PUSH_FIXED" instead of separate USE_PUSH_FIXED, the executor will handle it.)
     */
    public static AiDecision pushFixedThenPushAndMove(PushDecision pushFixedPush, MoveDecision move) {
        Objects.requireNonNull(pushFixedPush, "pushFixedPush");
        return new AiDecision(BonusDecision.pushFixed(), List.of(pushFixedPush), move);
    }

    /** Push twice: activate PUSH_TWICE and then do two pushes + move */
    public static AiDecision pushTwiceThenTwoPushesAndMove(PushDecision first, PushDecision second, MoveDecision move) {
        Objects.requireNonNull(first, "firstPush");
        Objects.requireNonNull(second, "secondPush");
        return new AiDecision(BonusDecision.pushTwice(), List.of(first, second), move);
    }

    // ---------- Value objects ----------

    public static final class PushDecision {
        private final int pushIndex;
        private final Direction pushDirection;
        private final int rotations; // 0..3 clockwise rotations of spare tile before this push

        public PushDecision(int pushIndex, Direction pushDirection, int rotations) {
            this.pushIndex = pushIndex;
            this.pushDirection = Objects.requireNonNull(pushDirection, "pushDirection");
            this.rotations = rotations;
        }

        public int getPushIndex() {
            return pushIndex;
        }

        public Direction getPushDirection() {
            return pushDirection;
        }

        public int getRotations() {
            return rotations;
        }
    }

    public static final class MoveDecision {
        private final Coordinates target;

        public MoveDecision(Coordinates target) {
            this.target = target;
        }

        public Coordinates getTarget() {
            return target;
        }
    }

    public static final class BonusDecision {
        private final BonusType bonusType;
        private final Coordinates beamTarget;
        private final String swapTargetPlayerId;

        private BonusDecision(BonusType bonusType, Coordinates beamTarget, String swapTargetPlayerId) {
            this.bonusType = Objects.requireNonNull(bonusType, "bonusType");
            this.beamTarget = beamTarget;
            this.swapTargetPlayerId = swapTargetPlayerId;
        }

        public BonusType getBonusType() {
            return bonusType;
        }

        public Coordinates getBeamTarget() {
            return beamTarget;
        }

        public String getSwapTargetPlayerId() {
            return swapTargetPlayerId;
        }

        public static BonusDecision beam(Coordinates target) {
            return new BonusDecision(BonusType.BEAM, Objects.requireNonNull(target, "target"), null);
        }

        public static BonusDecision swap(String targetPlayerId) {
            return new BonusDecision(BonusType.SWAP, null, Objects.requireNonNull(targetPlayerId, "targetPlayerId"));
        }

        public static BonusDecision pushFixed() {
            return new BonusDecision(BonusType.PUSH_FIXED, null, null);
        }

        public static BonusDecision pushTwice() {
            return new BonusDecision(BonusType.PUSH_TWICE, null, null);
        }
    }
}


