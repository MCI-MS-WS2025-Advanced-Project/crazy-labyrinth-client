package mci.dibse.crazylabyrinth.client.view.viewmodel;

import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.image.Image;
import labyrinth.contracts.models.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import mci.dibse.crazylabyrinth.client.domain.context.ApplicationEvents;
import mci.dibse.crazylabyrinth.client.domain.context.PushTileCommandRecord;
import mci.dibse.crazylabyrinth.client.domain.model.ClientPlayer;
import mci.dibse.crazylabyrinth.client.infrastructure.exceptions.GeneralUiException;
import mci.dibse.crazylabyrinth.client.infrastructure.util.FileNameMapper;
import mci.dibse.crazylabyrinth.client.infrastructure.util.JavaFXUtils;
import mci.dibse.crazylabyrinth.client.service.*;
import mci.dibse.crazylabyrinth.client.view.viewmodel.item.*;
import mci.dibse.crazylabyrinth.client.view.viewmodel.navigation.NavigationStateViewModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class GameViewModel extends BaseViewModel implements PlayerConnectedAckListener {

    private final String WAITING_FOR_PUSH_LABEL = "verschiebt einen Gang";
    private final String WAITING_FOR_MOVE_LABEL = "bewegt seine Spielfigur";

    private final LobbyService lobbyService;
    private final GameService gameService;
    private ClientCommandService clientCommandService;
    private final TimerService timerService;

    @Getter private GameBoardItem gameBoardItem;
    @Getter private List<PlayerStateItem> playerStatesSorted = new ArrayList<>();
    @Setter @Getter private ClientPlayer clientPlayer;
    @Getter private OffsetDateTime gameEndTime;

    @Getter private CurrentTurnInfoItem currentTurnInfoItem;

    @Getter
    private final SimpleObjectProperty<TreasureItem> nextTreasureItemProperty = new SimpleObjectProperty<>();

    @Getter final SimpleBooleanProperty pendingRotationProperty = new SimpleBooleanProperty();
    @Getter final SimpleBooleanProperty turnTimeoutProperty = new SimpleBooleanProperty();

    @Getter final SimpleBooleanProperty isBonusEnabledProperty = new SimpleBooleanProperty();

    @Getter
    final SimpleBooleanProperty isPushFixedTileBonusActiveProperty = new SimpleBooleanProperty();

    @Getter
    final SimpleBooleanProperty isPushTwiceTileBonusActiveProperty = new SimpleBooleanProperty();

    @Getter final SimpleBooleanProperty isSwapBonusActiveProperty = new SimpleBooleanProperty();
    @Getter final SimpleBooleanProperty isBeamTileBonusActiveProperty = new SimpleBooleanProperty();

    private ChangeListener<TileItem> spareTileListener;
    private ChangeListener<TurnState> turnStateChangeListener;

    @Autowired
    public GameViewModel(
            NavigationStateViewModel vm,
            LobbyService lobbyService,
            GameService gameService,
            ClientCommandService clientCommandService,
            TimerService timerService) {
        super(vm);
        this.lobbyService = lobbyService;
        this.gameService = gameService;
        this.clientCommandService = clientCommandService;
        this.timerService = timerService;
        pendingRotationProperty.set(false);

        lobbyService.addPlayerListener(this);
    }

    /**
     * Unregister listeners before bean destruction.
     */
    @PreDestroy
    public void preDestroy() {
        lobbyService.removePlayerListener(this);
    }

    /**
     * Return the state item for the current turn's player.
     *
     * @return player state for the current turn
     */
    public PlayerStateItem getCurrentPlayerState() {
        return playerStatesSorted.stream()
                .filter(
                        i ->
                                i.getPlayerIdAsString()
                                        .equals(currentTurnInfoItem.getCurrentPlayerIdAsString()))
                .findFirst()
                .orElseThrow();
    }

    /**
     * Return the current turn player id.
     *
     * @return current player id as string
     */
    public String getCurrentPlayerIdAsString() {
        return this.currentTurnInfoItem.getCurrentPlayerIdAsString();
    }

    /**
     * Return a localized label for the current turn state.
     *
     * @return label for the current turn state
     */
    public String getCurrentTurnStateLabelAsString() {
        return mapTurnStateToLabel(currentTurnInfoItem.getTurnStateProperty().get());
    }

    /**
     * Check whether the client player has timed out.
     *
     * @return true when the client player timed out
     */
    public boolean isClientPlayerTimeout() {
        return turnTimeoutProperty.get();
    }

    /**
     * Return the property that holds the current player id.
     *
     * @return current player id property
     */
    public SimpleStringProperty getCurrentPlayerIdProperty() {
        return currentTurnInfoItem.getCurrentPlayerIdProperty();
    }

    /**
     * Return the current spare tile item.
     *
     * @return spare tile item
     */
    public TileItem getSpareTileItem() {
        return gameBoardItem.getSpareTile();
    }

    /**
     * Return the spare tile property.
     *
     * @return spare tile property
     */
    public ObjectProperty<TileItem> getSpareTileProperty() {
        return gameBoardItem.getSpareTileProperty();
    }

    /**
     * Return the current turn state.
     *
     * @return current turn state
     */
    public TurnState getCurrentTurnState() {
        return currentTurnInfoItem.getTurnStateProperty().get();
    }

    /**
     * Return the current turn state property.
     *
     * @return current turn state property
     */
    public SimpleObjectProperty<TurnState> getCurrentTurnStateProperty() {
        return currentTurnInfoItem.getTurnStateProperty();
    }

    /**
     * Return the name of the next treasure.
     *
     * @return next treasure name
     */
    public String getNextTreasureNameAsString() {
        return nextTreasureItemProperty.get().getName();
    }

    /**
     * Check whether a pending rotation is active.
     *
     * @return true when a rotation is pending
     */
    public boolean isPendingRotation() {
        return pendingRotationProperty.get();
    }

    /**
     * Check whether bonuses are enabled for the game.
     *
     * @return true when bonuses are enabled
     */
    public boolean isBonusEnabled() {
        return isBonusEnabledProperty.get();
    }

    /**
     * Check whether the push fixed bonus is active.
     *
     * @return true when push fixed bonus is active
     */
    public boolean isPushFixedTileBonusActive() {
        return isPushFixedTileBonusActiveProperty.get();
    }

    /**
     * Check whether the push twice bonus is active.
     *
     * @return true when push twice bonus is active
     */
    public boolean isPushTwiceTileBonusActive() {
        return isPushTwiceTileBonusActiveProperty.get();
    }

    /**
     * Check whether the swap bonus is active.
     *
     * @return true when swap bonus is active
     */
    public boolean isSwapBonusActive() {
        return isSwapBonusActiveProperty.get();
    }

    /**
     * Check whether the beam bonus is active.
     *
     * @return true when beam bonus is active
     */
    public boolean isBeamTileBonusActive() {
        return isBeamTileBonusActiveProperty.get();
    }

    /**
     * Add a listener to the turn end time property.
     *
     * @param turnTimeChangeListener listener for turn end time changes
     */
    public void addListenerToCurrentTurnEndTime(
            ChangeListener<OffsetDateTime> turnTimeChangeListener) {
        currentTurnInfoItem.addListenerToTurnEndTimeProperty(turnTimeChangeListener);
    }

    /**
     * Return the image property for the next treasure.
     *
     * @return next treasure image property
     */
    public SimpleObjectProperty<Image> getNextTreasureImageProperty() {
        return nextTreasureItemProperty.get().getTreasureImageProperty();
    }

    /**
     * Check if any other bonus is active that would block the given bonus type.
     *
     * @param bonusType              bonus type to check
     * @param pushTwicePendingAction
     * @return true when another bonus is currently active
     */
    public boolean isAnyBonusActive(BonusType bonusType, SimpleBooleanProperty pushTwicePendingAction) {
        switch (bonusType) {
            case BonusType.SWAP -> {
                return isPushFixedTileBonusActive()
                        || isPushTwiceTileBonusActive()
                        || isBeamTileBonusActive()
                        || pushTwicePendingAction.get();
            }
            case BonusType.PUSH_FIXED -> {
                return isPushTwiceTileBonusActive()
                        || isSwapBonusActive()
                        || isBeamTileBonusActive()
                        || pushTwicePendingAction.get();
            }
            case BonusType.BEAM -> {
                return isPushFixedTileBonusActive()
                        || isPushTwiceTileBonusActive()
                        || isSwapBonusActive()
                        || pushTwicePendingAction.get();
            }
            case PUSH_TWICE -> {
                return isPushFixedTileBonusActive()
                        || isSwapBonusActive()
                        || isBeamTileBonusActive()
                        || pushTwicePendingAction.get();
            }
        }
        return false;
    }

    /**
     * Return the player state for the client player.
     *
     * @return client player state
     */
    public PlayerStateItem getClientPlayerState() {
        return playerStatesSorted.stream()
                .filter(i -> i.getPlayerIdAsString().equals(clientPlayer.getId()))
                .findFirst()
                .orElseThrow();
    }

    /**
     * Release resources and remove listeners for this view model.
     */
    @Override
    public void cleanup() {
        // todo?
        log.info("Cleanup GameViewModel");
        getSpareTileProperty().removeListener(spareTileListener);
        getCurrentTurnStateProperty().removeListener(turnStateChangeListener);

        gameBoardItem.dispose();
        gameBoardItem = null;
        playerStatesSorted.forEach(PlayerStateItem::dispose);
        playerStatesSorted.clear();
        currentTurnInfoItem.dispose();
        currentTurnInfoItem = null;
        gameEndTime = null;
    }

    /**
     * Capture the connected client player details.
     *
     * @param clientPlayer connected player payload
     */
    @Override
    public void onPlayerConnected(ClientPlayer clientPlayer) {
        this.clientPlayer = clientPlayer;
        turnTimeoutProperty.set(false);
    }

    /**
     * Handle game state events and update view state on the FX thread.
     *
     * @param gameState game state payload
     */
    @EventListener
    public void handleGameStateEvent(GameStateEventPayload gameState) {
        Runnable task =
                () -> {
                    if (EventType.GAME_STARTED.equals(gameState.getType())) {
                        gameStarted(gameState);
                        return;
                    }
                    if(NavigationStateViewModel.ScreenId.GAMEBOARD.equals(navigationStateViewModel.getCurrentScreenId())) gameUpdated(gameState);

                };
        JavaFXUtils.runOnFxThread(task);
    }

    /**
     * Update the next treasure card when the event arrives.
     *
     * @param nextTreasureCard next treasure payload
     */
    @EventListener
    public void handleTreasureCardEvent(NextTreasureCardEventPayload nextTreasureCard) {
        Runnable task =
                () -> {
                    TreasureItem treasureItem = new TreasureItem(nextTreasureCard.getTreasure());
                    log.info("set new treasure: {}", treasureItem.getId());
                    nextTreasureItemProperty.set(treasureItem);
                };
        JavaFXUtils.runOnFxThread(task);
    }

    /**
     * Mark the turn as timed out when the timeout event occurs.
     *
     * @param event application event
     */
    @EventListener
    public void handleTurnTimeout(ApplicationEvents event) {
        if (!ApplicationEvents.TURN_TIMEOUT.equals(event)) return;
        log.debug("handle turn timeout");
        Runnable task = () -> turnTimeoutProperty.set(true);
        JavaFXUtils.runOnFxThread(task);
    }

    /**
     * Apply player updates to the existing player state item.
     *
     * @param event player update payload
     */
    @EventListener
    public void handlePlayerUpdate(PlayerUpdatedEventPayload event) {
        log.debug("handle player update");
        String incomingPlayerId = event.getPlayer().getId();
        playerStatesSorted.stream()
                .filter(player -> player.getPlayerIdAsString().equals(incomingPlayerId))
                .findFirst()
                .orElseThrow()
                .updatePlayerInfoFrom(event.getPlayer());
    }

    /**
     * Add a listener for spare tile changes.
     *
     * @param spareTileListener listener to add
     */
    public void addListenerToSpareTileProperty(ChangeListener<TileItem> spareTileListener) {
        getSpareTileProperty().addListener(spareTileListener);
        this.spareTileListener = spareTileListener;
    }

    /**
     * Add a listener for turn state changes.
     *
     * @param turnStateChangeListener listener to add
     */
    public void addListenerToCurrentTurnStateProperty(
            ChangeListener<TurnState> turnStateChangeListener) {
        getCurrentTurnStateProperty().addListener(turnStateChangeListener);
        this.turnStateChangeListener = turnStateChangeListener;
    }

    /**
     * Map a turn state to a localized label.
     *
     * @param turnState turn state enum
     * @return label for the turn state
     */
    public String mapTurnStateToLabel(TurnState turnState) {
        return switch (turnState) {
            case WAITING_FOR_MOVE -> WAITING_FOR_MOVE_LABEL;
            case WAITING_FOR_PUSH -> WAITING_FOR_PUSH_LABEL;
        };
    }

    /**
     * Send a move pawn command and handle any UI errors.
     *
     * @param finalRow target row
     * @param finalCol target column
     */
    public void movePawnCommand(int finalRow, int finalCol) {
        try {
            clientCommandService.sendMovePawnCommand(finalRow, finalCol);
        } catch (GeneralUiException e) {
            setError(e.getMessage());
        }
    }

    /**
     * Send a push tile command and handle any UI errors.
     *
     * @param data push tile command data
     */
    public void pushTileCommand(PushTileCommandRecord data) {
        try {
            clientCommandService.sendPushTileCommand(data);
        } catch (GeneralUiException e) {
            setError(e.getMessage());
        }
    }

    /**
     * Handle rotation left; currently no-op.
     */
    public void onLeftRotationOfSpareTile() {}

    /**
     * Send a rotate tile command and handle any UI errors.
     */
    public void onRightRotationOfSpareTile() {
        try {
            clientCommandService.sendRotateTileCommand();
        } catch (GeneralUiException e) {
            setError(e.getMessage());
        }
    }

    /**
     * Send a use push twice bonus command.
     */
    public void sendUsePushTwiceCommand() {
        try {
            clientCommandService.sendUsePushTwiceComnmand();
        } catch (GeneralUiException e) {
            setError(e.getMessage());
            throw new RuntimeException("error sending push twice command");
        }
    }

    /**
     * Send a use beam bonus command.
     *
     * @param finalRow target row
     * @param finalCol target column
     */
    public void sendUseBeamCommand(int finalRow, int finalCol) {
        try {
            clientCommandService.sendUseBeamCommand(finalRow, finalCol);
        } catch (GeneralUiException e) {
            setError(e.getMessage());
        }
    }

    /**
     * Send a use swap bonus command.
     *
     * @param playerId target player id
     */
    public void sendUseSwapCommand(String playerId) {
        try {
            clientCommandService.sendUseSwapCommand(playerId);
        } catch (GeneralUiException e) {
            setError(e.getMessage());
        }
    }

    /**
     * Return the remaining game time in seconds.
     *
     * @return remaining game time in seconds
     */
    public Long getRemainingGameTimeInSeconds() {
        return Duration.between(
                        timerService.getCurrentServerTimeWithGameServerOffset(), gameEndTime)
                .toSeconds();
    }

    /**
     * Return the remaining turn time in seconds.
     *
     * @return remaining turn time in seconds
     */
    public Long getRemainingTurnTimeInSeconds() {
        return Duration.between(
                        timerService.getCurrentServerTimeWithGameServerOffset(),
                        currentTurnInfoItem.getTurnEndTime())
                .toSeconds();
    }

    /**
     * Check whether the current turn time is initialized.
     *
     * @return true when the turn time is available
     */
    public boolean isCurrentTurnTimeSet() {
        return currentTurnInfoItem != null;
    }

    /**
     * Decide whether to run custom cleanup on navigation changes.
     *
     * @param oldValue previous screen
     * @param newValue new screen
     * @return true when cleanup should run
     */
    @Override
    protected boolean customCleanupCheckpoint(
            NavigationStateViewModel.ScreenId oldValue,
            NavigationStateViewModel.ScreenId newValue) {
        return newValue != null
                && NavigationStateViewModel.ScreenId.GAMEBOARD.equals(oldValue)
                && !NavigationStateViewModel.ScreenId.GAMEBOARD.equals(newValue);
    }

    /**
     * Determine whether errors are relevant for the game view.
     *
     * @return true when the game screen is active
     */
    @Override
    protected boolean isErrorRelevant() {
        log.debug("is error relevant for game view model:");
        return NavigationStateViewModel.ScreenId.GAMEBOARD.equals(
                navigationStateViewModel.getCurrentScreenProperty().get());
    }

    /**
     * Initialize game state for a newly started game.
     *
     * @param gameState game state payload
     */
    private void gameStarted(GameStateEventPayload gameState) {
        GameBoard initialBoard = gameState.getBoard();
        isBonusEnabledProperty.set(isBonusEnabled(gameState));
        gameBoardItem = new GameBoardItem(initialBoard);
        Arrays.stream(gameState.getPlayers())
                .map(PlayerStateItem::new)
                .forEach(playerStatesSorted::add);
        // sort client player first then the rest according to id
        playerStatesSorted.sort(
                Comparator.comparing(
                                (PlayerStateItem p) ->
                                        !p.getPlayerIdAsString().equals(clientPlayer.getId()))
                        .thenComparing(PlayerStateItem::getPlayerIdAsString));

        gameEndTime = gameState.getGameEndTime();
        currentTurnInfoItem = new CurrentTurnInfoItem(gameState.getCurrentTurnInfo());

        navigationStateViewModel.navigateTo(NavigationStateViewModel.ScreenId.GAMEBOARD);
    }

    /**
     * Apply incremental game state updates.
     *
     * @param gameState game state payload
     */
    private void gameUpdated(GameStateEventPayload gameState) {
        this.gameBoardItem.updateFrom(gameState.getBoard());
        Arrays.stream(gameState.getPlayers())
                .forEach(
                        p -> {
                            String incomingId = p.getPlayerInfo().getId();
                            if (incomingId.equals(clientPlayer.getId())) checkIfLastTreasureIsFound(p);
                            playerStatesSorted.stream()
                                    .filter(
                                            playerItem ->
                                                    playerItem
                                                            .getPlayerIdAsString()
                                                            .equals(incomingId))
                                    .findFirst()
                                    .ifPresentOrElse(
                                            playerItem -> playerItem.updateFrom(p),
                                            () -> playerStatesSorted.add(new PlayerStateItem(p)));
                        });
        this.currentTurnInfoItem.updateFrom(gameState.getCurrentTurnInfo());
        this.pendingRotationProperty.set(false);
    }

    /**
     * Replace the next treasure with "back to home" when the last treasure is found.
     *
     * @param p player state payload
     */
    private void checkIfLastTreasureIsFound(PlayerState p) {
        if (p.getRemainingTreasureCount() > 0) return;
        log.info("last treasure found setting up back to base treasure");
        int id = FileNameMapper.getHomeTreasureIdForPlayerColor(p.getPlayerInfo().getColor());
        Treasure t = new Treasure();
        t.setId(id);
        t.setName("Zurück zum Start");
        TreasureItem backToHome = new TreasureItem(t);
        nextTreasureItemProperty.set(backToHome);
    }

    /**
     * Determine whether any bonus tiles are available for the game.
     *
     * @param gameState game state payload
     * @return true when a bonus is present on the board
     */
    private boolean isBonusEnabled(GameStateEventPayload gameState) {
        GameBoard initialBoard = gameState.getBoard();
        if (initialBoard.getSpareTile().getBonus() != null) return true;
        return Arrays.stream(initialBoard.getTiles())
                .flatMap(Arrays::stream)
                .anyMatch(tile -> tile.getBonus() != null);
    }

    /**
     * Leave the current game by sending a disconnect command.
     */
    public void leaveGame() {
        clientCommandService.sendDisconnectCommand();
    }
}
