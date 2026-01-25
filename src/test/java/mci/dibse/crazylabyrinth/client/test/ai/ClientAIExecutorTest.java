package mci.dibse.crazylabyrinth.client.test.ai;

import labyrinth.contracts.models.*;
import mci.dibse.crazylabyrinth.client.ai.AiDecision;
import mci.dibse.crazylabyrinth.client.ai.BoardSimulationHelper;
import mci.dibse.crazylabyrinth.client.ai.ClientAIExecutor;
import mci.dibse.crazylabyrinth.client.domain.context.PushTileCommandRecord;
import mci.dibse.crazylabyrinth.client.view.viewmodel.GameViewModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**

 * Unit tests for {@link ClientAIExecutor}.
 *
 * <p>
 * These tests verify that {@link ClientAIExecutor} correctly translates a prepared {@link AiDecision}
 * into concrete client commands sent via {@link GameViewModel}, while respecting the server-side
 * {@link TurnState} state machine.
 * </p>
 *
 * <p>
 * Focus areas:
 * <ul>
 * <li>Correct handling of different {@link BonusType}s</li>
 * <li>Correct sequencing of bonus → push(es) → move</li>
 * <li>Ensuring that actions are only sent in the appropriate {@link TurnState}</li>
 * <li>Ensuring state is cleared after decision completion</li>
 * </ul>
 * </p>

 */
class ClientAIExecutorTest {

    /**
     * UT-AICLIENT-15
     *
     * <p>
     * Verifies the complete execution flow for {@link BonusType#PUSH_TWICE}.
     * </p>
     *
     * <p>
     * Expected behavior:
     * <ul>
     *   <li>The PUSH_TWICE bonus is activated exactly once</li>
     *   <li>Exactly two push commands are sent</li>
     *   <li>No move is sent during {@link TurnState#WAITING_FOR_PUSH}</li>
     *   <li>The move is sent exactly once during {@link TurnState#WAITING_FOR_MOVE}</li>
     *   <li>The pending decision is cleared afterwards</li>
     * </ul>
     * </p>
     */
    @Test
    @DisplayName("UT-AICLIENT-15: PUSH_TWICE executes bonus once, two pushes, then move")
    void pushTwiceFullFlow() {
        GameViewModel vm = mock(GameViewModel.class, RETURNS_DEEP_STUBS);
        BoardSimulationHelper helper = mock(BoardSimulationHelper.class);

        when(vm.getCurrentTurnState()).thenReturn(null);

        ClientAIExecutor sut = new ClientAIExecutor(vm, helper);

        AiDecision.PushDecision p1 = new AiDecision.PushDecision(1, Direction.RIGHT, 1);
        AiDecision.PushDecision p2 = new AiDecision.PushDecision(3, Direction.LEFT, 0);

        AiDecision decision = AiDecision.pushTwiceThenTwoPushesAndMove(
                p1,
                p2,
                new AiDecision.MoveDecision(coord(2, 2))
        );

        sut.executeDecision(decision);

        // push phase: first call => bonus + push#1
        sut.onTurnStateChanged(TurnState.WAITING_FOR_PUSH);
        // push phase: second call => push#2
        sut.onTurnStateChanged(TurnState.WAITING_FOR_PUSH);

        verify(vm, times(1)).sendUsePushTwiceCommand();
        verify(vm, times(2)).pushTileCommand(any(PushTileCommandRecord.class));
        verify(vm, never()).movePawnCommand(anyInt(), anyInt());

        prepareMoveState(vm, helper);
        sut.onTurnStateChanged(TurnState.WAITING_FOR_MOVE);

        verify(vm, times(1)).movePawnCommand(anyInt(), anyInt());
        assertFalse(sut.hasPendingDecision());
    }

    /**
     * UT-AICLIENT-16
     *
     * <p>
     * Verifies correct execution for {@link BonusType#BEAM}.
     * </p>
     *
     * <p>
     * Expected behavior:
     * <ul>
     *   <li>The BEAM command is sent once during {@link TurnState#WAITING_FOR_PUSH}</li>
     *   <li>No push commands are sent</li>
     *   <li>The move is sent during {@link TurnState#WAITING_FOR_MOVE}</li>
     * </ul>
     * </p>
     */
    @Test
    @DisplayName("UT-AICLIENT-16: BEAM sends beam command and skips push phase")
    void beamSkipsPush() {
        GameViewModel vm = mock(GameViewModel.class, RETURNS_DEEP_STUBS);
        BoardSimulationHelper helper = mock(BoardSimulationHelper.class);

        ClientAIExecutor sut = new ClientAIExecutor(vm, helper);

        Coordinates beamTarget = coord(1, 1);

        AiDecision decision = AiDecision.beamAndMove(
                beamTarget,
                new AiDecision.MoveDecision(coord(2, 2))
        );

        sut.executeDecision(decision);
        sut.onTurnStateChanged(TurnState.WAITING_FOR_PUSH);

        verify(vm, times(1)).sendUseBeamCommand(1, 1);
        verify(vm, never()).pushTileCommand(any());

        prepareMoveState(vm, helper);
        sut.onTurnStateChanged(TurnState.WAITING_FOR_MOVE);

        verify(vm, times(1)).movePawnCommand(anyInt(), anyInt());
        assertFalse(sut.hasPendingDecision());
    }

    /**
     * UT-AICLIENT-17
     *
     * <p>
     * Verifies correct execution for {@link BonusType#SWAP}.
     * </p>
     *
     * <p>
     * Expected behavior:
     * <ul>
     *   <li>The SWAP command is sent once during {@link TurnState#WAITING_FOR_PUSH}</li>
     *   <li>No push commands are sent</li>
     *   <li>The move is sent during {@link TurnState#WAITING_FOR_MOVE}</li>
     * </ul>
     * </p>
     */
    @Test
    @DisplayName("UT-AICLIENT-17: SWAP sends swap command and no pushes")
    void swapSkipsPush() {
        GameViewModel vm = mock(GameViewModel.class, RETURNS_DEEP_STUBS);
        BoardSimulationHelper helper = mock(BoardSimulationHelper.class);

        ClientAIExecutor sut = new ClientAIExecutor(vm, helper);

        AiDecision decision = AiDecision.swapAndMove(
                "P2",
                new AiDecision.MoveDecision(coord(0, 0))
        );

        sut.executeDecision(decision);
        sut.onTurnStateChanged(TurnState.WAITING_FOR_PUSH);

        verify(vm, times(1)).sendUseSwapCommand("P2");
        verify(vm, never()).pushTileCommand(any());

        prepareMoveState(vm, helper);
        sut.onTurnStateChanged(TurnState.WAITING_FOR_MOVE);

        verify(vm, times(1)).movePawnCommand(anyInt(), anyInt());
        assertFalse(sut.hasPendingDecision());
    }

    /**
     * UT-AICLIENT-18
     *
     * <p>
     * Verifies correct handling of {@link BonusType#PUSH_FIXED}.
     * </p>
     *
     * <p>
     * Expected behavior:
     * <ul>
     *   <li>No explicit bonus activation command is sent</li>
     *   <li>The push command is encoded with {@link CommandType#USE_PUSH_FIXED}</li>
     * </ul>
     * </p>
     */
    @Test
    @DisplayName("UT-AICLIENT-18: PUSH_FIXED encodes push as USE_PUSH_FIXED")
    void pushFixedUsesSpecialCommandType() {
        GameViewModel vm = mock(GameViewModel.class, RETURNS_DEEP_STUBS);
        BoardSimulationHelper helper = mock(BoardSimulationHelper.class);

        ClientAIExecutor sut = new ClientAIExecutor(vm, helper);

        AiDecision.PushDecision push = new AiDecision.PushDecision(1, Direction.DOWN, 0);

        AiDecision decision = AiDecision.pushFixedThenPushAndMove(
                push,
                new AiDecision.MoveDecision(coord(1, 1))
        );

        sut.executeDecision(decision);
        sut.onTurnStateChanged(TurnState.WAITING_FOR_PUSH);

        verify(vm).pushTileCommand(argThat(r ->
                r != null && r.commandType() == CommandType.USE_PUSH_FIXED
        ));
    }

    /**
     * UT-AICLIENT-19
     *
     * <p>
     * Verifies move gating: even for a normal decision, no move is sent during
     * {@link TurnState#WAITING_FOR_PUSH}, and the move is sent exactly once in
     * {@link TurnState#WAITING_FOR_MOVE}.
     * </p>
     */
    @Test
    @DisplayName("UT-AICLIENT-19: move is only sent in WAITING_FOR_MOVE, never in WAITING_FOR_PUSH")
    void moveOnlySentInMovePhase() {
        GameViewModel vm = mock(GameViewModel.class, RETURNS_DEEP_STUBS);
        BoardSimulationHelper helper = mock(BoardSimulationHelper.class);

        ClientAIExecutor sut = new ClientAIExecutor(vm, helper);

        AiDecision.PushDecision push = new AiDecision.PushDecision(1, Direction.RIGHT, 0);
        AiDecision decision = AiDecision.pushAndMove(push, new AiDecision.MoveDecision(coord(0, 0)));

        sut.executeDecision(decision);

        sut.onTurnStateChanged(TurnState.WAITING_FOR_PUSH);
        verify(vm, never()).movePawnCommand(anyInt(), anyInt());

        prepareMoveState(vm, helper);
        sut.onTurnStateChanged(TurnState.WAITING_FOR_MOVE);

        verify(vm, times(1)).movePawnCommand(anyInt(), anyInt());
    }

// -------------------------------------------------
// Helper methods
// -------------------------------------------------

    private static Coordinates coord(int r, int c) {
        Coordinates x = new Coordinates();
        x.setRow(r);
        x.setColumn(c);
        return x;
    }

    /**
     * Prepares a minimal, valid game state so that {@link ClientAIExecutor}
     * can recompute a move target during {@link TurnState#WAITING_FOR_MOVE}.
     *
     * <p>
     * The setup ensures:
     * <ul>
     *   <li>a board exists</li>
     *   <li>the AI player exists with current position and home position</li>
     *   <li>the AI's current target treasure is "home" (id=0)</li>
     * </ul>
     * </p>
     */
    private static void prepareMoveState(GameViewModel vm, BoardSimulationHelper helper) {
        GameStateEventPayload gs = new GameStateEventPayload();

        GameBoard b = new GameBoard();
        b.setRows(1);
        b.setCols(1);
        b.setTiles(new Tile[][]{{new Tile()}});
        gs.setBoard(b);

        PlayerInfo pi = new PlayerInfo();
        pi.setId("AI");

        PlayerState ps = new PlayerState();
        ps.setPlayerInfo(pi);
        ps.setCurrentPosition(coord(0, 0));
        ps.setHomePosition(coord(0, 0));

        gs.setPlayers(new PlayerState[]{ps});

        when(vm.getLastGameState()).thenReturn(gs);

        // Key fix: do NOT do "when(vm.getClientPlayer()).thenReturn(pi)" (type mismatch).
        when(vm.getClientPlayer().getId()).thenReturn("AI");

        Treasure t = new Treasure();
        t.setId(0);
        when(vm.getCurrentTargetTreasureForAi()).thenReturn(t);

        when(helper.findTreasurePosition(any(), anyInt())).thenReturn(coord(0, 0));
    }

}
