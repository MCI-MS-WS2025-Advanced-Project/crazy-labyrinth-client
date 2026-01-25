package mci.dibse.crazylabyrinth.client.ai;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import labyrinth.contracts.models.BonusType;
import labyrinth.contracts.models.Coordinates;
import labyrinth.contracts.models.Direction;

/**
 * Immutable value object describing the AI decision for one logical turn on the client side.
 * <p>
 * A decision consists of:
 * <ul>
 *   <li>an optional {@link BonusDecision} (BEAM, SWAP, PUSH_FIXED, PUSH_TWICE)</li>
 *   <li>zero, one or two {@link PushDecision} objects</li>
 *   <li>an optional {@link MoveDecision} (usually present)</li>
 * </ul>
 * </p>
 *
 * <p>
 * Interpretation:
 * <ul>
 *   <li>Normal turn: 1 push + move</li>
 *   <li>Beam / Swap: bonus + 0 pushes + move</li>
 *   <li>Push fixed: bonus + 1 push + move</li>
 *   <li>Push twice: bonus + 2 pushes + move</li>
 * </ul>
 * </p>
 *
 * <p>
 * This class is purely descriptive and contains no execution logic.
 * It is interpreted by the client-side AI executor.
 * </p>
 */
public final class AiDecision {

    private final BonusDecision bonus;
    private final List<PushDecision> pushes;
    private final MoveDecision move;

    /**
     * Internal constructor used by the factory methods.
     *
     * @param bonus optional bonus decision (may be {@code null})
     * @param pushes list of push decisions (never {@code null}, may be empty)
     * @param move optional move decision (usually non-null)
     */
    private AiDecision(BonusDecision bonus, List<PushDecision> pushes, MoveDecision move) {
        this.bonus = bonus;
        this.pushes = pushes == null ? Collections.emptyList() : List.copyOf(pushes);
        this.move = move;
    }

    /**
     * @return the bonus decision, or {@code null} if no bonus is used
     */
    public BonusDecision getBonus() {
        return bonus;
    }

    /**
     * @return {@code true} if this decision uses a bonus, otherwise {@code false}
     */
    public boolean hasBonus() {
        return bonus != null;
    }

    /**
     * @return immutable list of push decisions
     */
    public List<PushDecision> getPushes() {
        return pushes;
    }

    /**
     * @return immutable list of push decisions
     */
    public int getPushCount() {
        return pushes.size();
    }

    /**
     * @return {@code true} if this decision contains a valid move target
     */
    public boolean hasMove() {
        return move != null && move.getTarget() != null;
    }

    /**
     * @return the move decision, or {@code null} if none is defined
     */
    public MoveDecision getMove() {
        return move;
    }


    /**
     * Creates a normal turn consisting of exactly one push and one move.
     *
     * @param push the push to execute
     * @param move the move to execute afterwards
     * @return a new {@link AiDecision} representing a normal turn
     */
    public static AiDecision pushAndMove(PushDecision push, MoveDecision move) {
        Objects.requireNonNull(push, "push");
        return new AiDecision(null, List.of(push), move);
    }

    /**
     * Creates a turn that uses the BEAM bonus and then performs a move.
     *
     * @param beamTarget the target coordinate for the beam
     * @param move the move to execute after the beam
     * @return a new {@link AiDecision} representing a beam turn
     */
    public static AiDecision beamAndMove(Coordinates beamTarget, MoveDecision move) {
        Objects.requireNonNull(beamTarget, "beamTarget");
        return new AiDecision(BonusDecision.beam(beamTarget), Collections.emptyList(), move);
    }

    /**
     * Creates a turn that uses the SWAP bonus and then performs a move.
     *
     * @param swapTargetPlayerId the id of the player to swap with
     * @param move the move to execute after the swap
     * @return a new {@link AiDecision} representing a swap turn
     */
    public static AiDecision swapAndMove(String swapTargetPlayerId, MoveDecision move) {
        Objects.requireNonNull(swapTargetPlayerId, "swapTargetPlayerId");
        return new AiDecision(BonusDecision.swap(swapTargetPlayerId), Collections.emptyList(), move);
    }

    /**
     * Creates a turn that activates the PUSH_FIXED bonus and then performs one push and a move.
     *
     * @param pushFixedPush the push to execute using the PUSH_FIXED bonus
     * @param move the move to execute afterwards
     * @return a new {@link AiDecision} representing a push-fixed turn
     */
    public static AiDecision pushFixedThenPushAndMove(PushDecision pushFixedPush, MoveDecision move) {
        Objects.requireNonNull(pushFixedPush, "pushFixedPush");
        return new AiDecision(BonusDecision.pushFixed(), List.of(pushFixedPush), move);
    }

    /**
     * Creates a turn that activates the PUSH_TWICE bonus, performs two pushes, and then moves.
     *
     * @param first the first push
     * @param second the second push
     * @param move the move to execute after both pushes
     * @return a new {@link AiDecision} representing a push-twice turn
     */
    public static AiDecision pushTwiceThenTwoPushesAndMove(PushDecision first, PushDecision second, MoveDecision move) {
        Objects.requireNonNull(first, "firstPush");
        Objects.requireNonNull(second, "secondPush");
        return new AiDecision(BonusDecision.pushTwice(), List.of(first, second), move);
    }


    /**
     * Immutable value object describing a single push operation.
     * <p>
     * A push is defined by:
     * <ul>
     *   <li>the index to push at</li>
     *   <li>the push direction</li>
     *   <li>the number of clockwise 90° rotations applied to the spare tile before pushing</li>
     * </ul>
     * </p>
     */
    public static final class PushDecision {
        private final int pushIndex;
        private final Direction pushDirection;
        private final int rotations; // 0..3 clockwise rotations of spare tile before this push


        /**
         * Creates a push decision.
         *
         * @param pushIndex the row/column index to push at
         * @param pushDirection the push direction
         * @param rotations number of clockwise 90° rotations of the spare tile
         */
        public PushDecision(int pushIndex, Direction pushDirection, int rotations) {
            this.pushIndex = pushIndex;
            this.pushDirection = Objects.requireNonNull(pushDirection, "pushDirection");
            this.rotations = rotations;
        }

        /** @return the push index */
        public int getPushIndex() {
            return pushIndex;
        }

        /** @return the push direction */
        public Direction getPushDirection() {
            return pushDirection;
        }

        /** @return number of clockwise 90° rotations */
        public int getRotations() {
            return rotations;
        }
    }


    /**
     * Immutable value object describing a movement target.
     */
    public static final class MoveDecision {
        private final Coordinates target;


        /**
         * Creates a move decision.
         *
         * @param target the target coordinate to move to
         */
        public MoveDecision(Coordinates target) {
            this.target = target;
        }

        /** @return the movement target coordinate */
        public Coordinates getTarget() {
            return target;
        }
    }

    /**
     * Immutable value object describing how a bonus is used.
     * <p>
     * Depending on {@link #getBonusType()}, different fields are used:
     * <ul>
     *   <li>{@link BonusType#BEAM}: {@link #getBeamTarget()}</li>
     *   <li>{@link BonusType#SWAP}: {@link #getSwapTargetPlayerId()}</li>
     *   <li>{@link BonusType#PUSH_FIXED}: no additional parameters</li>
     *   <li>{@link BonusType#PUSH_TWICE}: no additional parameters</li>
     * </ul>
     * </p>
     */
    public static final class BonusDecision {
        private final BonusType bonusType;
        private final Coordinates beamTarget;
        private final String swapTargetPlayerId;

        private BonusDecision(BonusType bonusType, Coordinates beamTarget, String swapTargetPlayerId) {
            this.bonusType = Objects.requireNonNull(bonusType, "bonusType");
            this.beamTarget = beamTarget;
            this.swapTargetPlayerId = swapTargetPlayerId;
        }

        /** @return the bonus type used */
        public BonusType getBonusType() {
            return bonusType;
        }

        /** @return the beam target (only for BEAM), otherwise {@code null} */
        public Coordinates getBeamTarget() {
            return beamTarget;
        }

        /** @return the swap target player id (only for SWAP), otherwise {@code null} */
        public String getSwapTargetPlayerId() {
            return swapTargetPlayerId;
        }

        /**
         * Creates a BEAM bonus decision.
         *
         * @param target the beam destination
         * @return a BEAM bonus decision
         */
        public static BonusDecision beam(Coordinates target) {
            return new BonusDecision(BonusType.BEAM, Objects.requireNonNull(target, "target"), null);
        }

        /**
         * Creates a SWAP bonus decision.
         *
         * @param targetPlayerId the id of the player to swap with
         * @return a SWAP bonus decision
         */
        public static BonusDecision swap(String targetPlayerId) {
            return new BonusDecision(BonusType.SWAP, null, Objects.requireNonNull(targetPlayerId, "targetPlayerId"));
        }

        /**
         * Creates a PUSH_FIXED bonus decision.
         *
         * @return a PUSH_FIXED bonus decision
         */
        public static BonusDecision pushFixed() {
            return new BonusDecision(BonusType.PUSH_FIXED, null, null);
        }

        /**
         * Creates a PUSH_TWICE bonus decision.
         *
         * @return a PUSH_TWICE bonus decision
         */
        public static BonusDecision pushTwice() {
            return new BonusDecision(BonusType.PUSH_TWICE, null, null);
        }
    }
}


