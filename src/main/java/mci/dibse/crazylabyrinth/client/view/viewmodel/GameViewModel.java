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
import labyrinth.contracts.models.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import mci.dibse.crazylabyrinth.client.domain.context.ApplicationEvents;
import mci.dibse.crazylabyrinth.client.domain.context.PushTileCommandRecord;
import mci.dibse.crazylabyrinth.client.domain.model.ClientPlayer;
import mci.dibse.crazylabyrinth.client.infrastructure.exceptions.GeneralUiException;
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
    private final SimpleObjectProperty<TreasureItem> nextTreasure = new SimpleObjectProperty<>();

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

    @PreDestroy
    public void preDestroy() {
        lobbyService.removePlayerListener(this);
    }

    public String getCurrentPlayerIdAsString() {
        return this.currentTurnInfoItem.getCurrentPlayerIdAsString();
    }

    public String getCurrentTurnStateLabelAsString() {
        return mapTurnStateToLabel(currentTurnInfoItem.getTurnStateProperty().get());
    }

    public boolean isClientPlayerTimeout() {
        return turnTimeoutProperty.get();
    }

    public SimpleStringProperty getCurrentPlayerIdProperty() {
        return currentTurnInfoItem.getCurrentPlayerIdProperty();
    }

    public TileItem getSpareTileItem() {
        return gameBoardItem.getSpareTile();
    }

    public ObjectProperty<TileItem> getSpareTileProperty() {
        return gameBoardItem.getSpareTileProperty();
    }

    public TurnState getCurrentTurnState() {
        return currentTurnInfoItem.getTurnStateProperty().get();
    }

    public SimpleObjectProperty<TurnState> getCurrentTurnStateProperty() {
        return currentTurnInfoItem.getTurnStateProperty();
    }

    public String getNextTreasureNameAsString() {
        return nextTreasure.get().getName();
    }

    public boolean isPendingRotation() {
        return pendingRotationProperty.get();
    }

    public boolean isBonusEnabled() {
        return isBonusEnabledProperty.get();
    }

    public boolean isPushFixedTileBonusActive() {
        return isPushFixedTileBonusActiveProperty.get();
    }

    public boolean isPushTwiceTileBonusActive() {
        return isPushTwiceTileBonusActiveProperty.get();
    }

    public boolean isSwapBonusActive() {
        return isSwapBonusActiveProperty.get();
    }

    public boolean isBeamTileBonusActive() {
        return isBeamTileBonusActiveProperty.get();
    }

    public void addListenerToCurrentTurnEndTime(
            ChangeListener<OffsetDateTime> turnTimeChangeListener) {
        currentTurnInfoItem.addListenerToTurnEndTimeProperty(turnTimeChangeListener);
    }

    public boolean isAnyBonusActive(BonusType bonusType) {
        switch (bonusType) {
            case BonusType.SWAP -> {
                return isPushFixedTileBonusActive()
                        || isPushTwiceTileBonusActive()
                        || isBeamTileBonusActive();
            }
            case BonusType.PUSH_FIXED -> {
                return isPushTwiceTileBonusActive()
                        || isSwapBonusActive()
                        || isBeamTileBonusActive();
            }
            case BonusType.BEAM -> {
                return isPushFixedTileBonusActive()
                        || isPushTwiceTileBonusActive()
                        || isSwapBonusActive();
            }
        }
        return false;
    }

    public PlayerStateItem getClientPlayerState() {
        return playerStatesSorted.stream()
                .filter(i -> i.getPlayerIdAsString().equals(clientPlayer.getId()))
                .findFirst()
                .orElseThrow();
    }

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

    @Override
    public void onPlayerConnected(ClientPlayer clientPlayer) {
        this.clientPlayer = clientPlayer;
        turnTimeoutProperty.set(false);
    }

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

    @EventListener
    public void handleTreasureCardEvent(NextTreasureCardEventPayload nextTreasureCard) {
        Runnable task =
                () -> {
                    TreasureItem treasureItem = new TreasureItem(nextTreasureCard.getTreasure());
                    log.info("set new treasure: {}", treasureItem.getId());
                    nextTreasure.set(treasureItem);
                };
        JavaFXUtils.runOnFxThread(task);
    }

    @EventListener
    public void handleTurnTimeout(ApplicationEvents event) {
        if (!ApplicationEvents.TURN_TIMEOUT.equals(event)) return;
        log.debug("handle turn timeout");
        Runnable task = () -> turnTimeoutProperty.set(true);
        JavaFXUtils.runOnFxThread(task);
    }

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

    public void addListenerToSpareTileProperty(ChangeListener<TileItem> spareTileListener) {
        getSpareTileProperty().addListener(spareTileListener);
        this.spareTileListener = spareTileListener;
    }

    public void addListenerToCurrentTurnStateProperty(
            ChangeListener<TurnState> turnStateChangeListener) {
        getCurrentTurnStateProperty().addListener(turnStateChangeListener);
        this.turnStateChangeListener = turnStateChangeListener;
    }

    public String mapTurnStateToLabel(TurnState turnState) {
        return switch (turnState) {
            case WAITING_FOR_MOVE -> WAITING_FOR_MOVE_LABEL;
            case WAITING_FOR_PUSH -> WAITING_FOR_PUSH_LABEL;
        };
    }

    public void movePawnCommand(int finalRow, int finalCol) {
        try {
            clientCommandService.sendMovePawnCommand(finalRow, finalCol);
        } catch (GeneralUiException e) {
            setError(e.getMessage());
        }
    }

    public void pushTileCommand(PushTileCommandRecord data) {
        try {
            clientCommandService.sendPushTileCommand(data);
        } catch (GeneralUiException e) {
            setError(e.getMessage());
        }
    }

    public void onLeftRotationOfSpareTile() {}

    public void onRightRotationOfSpareTile() {
        try {
            clientCommandService.sendRotateTileCommand();
        } catch (GeneralUiException e) {
            setError(e.getMessage());
        }
    }

    public void sendUsePushTwiceCommand() {
        try {
            clientCommandService.sendUsePushTwiceComnmand();
        } catch (GeneralUiException e) {
            setError(e.getMessage());
        }
    }

    public void sendUseBeamCommand(int finalRow, int finalCol) {
        try {
            clientCommandService.sendUseBeamCommand(finalRow, finalCol);
        } catch (GeneralUiException e) {
            setError(e.getMessage());
        }
    }

    public void sendUseSwapCommand(String playerId) {
        try {
            clientCommandService.sendUseSwapCommand(playerId);
        } catch (GeneralUiException e) {
            setError(e.getMessage());
        }
    }

    public Long getRemainingGameTimeInSeconds() {
        return Duration.between(
                        timerService.getCurrentServerTimeWithGameServerOffset(), gameEndTime)
                .toSeconds();
    }

    public Long getRemainingTurnTimeInSeconds() {
        return Duration.between(
                        timerService.getCurrentServerTimeWithGameServerOffset(),
                        currentTurnInfoItem.getTurnEndTime())
                .toSeconds();
    }

    public boolean isCurrentTurnTimeSet() {
        return currentTurnInfoItem != null;
    }

    @Override
    protected boolean customCleanupCheckpoint(
            NavigationStateViewModel.ScreenId oldValue,
            NavigationStateViewModel.ScreenId newValue) {
        return newValue != null
                && NavigationStateViewModel.ScreenId.GAMEBOARD.equals(oldValue)
                && !NavigationStateViewModel.ScreenId.GAMEBOARD.equals(newValue);
    }

    @Override
    protected boolean isErrorRelevant() {
        log.debug("is error relevant for game view model:");
        return NavigationStateViewModel.ScreenId.GAMEBOARD.equals(
                navigationStateViewModel.getCurrentScreenProperty().get());
    }

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

    private void checkIfLastTreasureIsFound(PlayerState p) {
        if (p.getRemainingTreasureCount() > 0) return;
        Treasure t = new Treasure();
        t.setId(999);
        t.setName("Zurück zum Start");
        TreasureItem backToHome = new TreasureItem(t);
        nextTreasure.set(backToHome);
    }

    private boolean isBonusEnabled(GameStateEventPayload gameState) {
        GameBoard initialBoard = gameState.getBoard();
        if (initialBoard.getSpareTile().getBonus() != null) return true;
        return Arrays.stream(initialBoard.getTiles())
                .flatMap(Arrays::stream)
                .anyMatch(tile -> tile.getBonus() != null);
    }

    public void leaveGame() {
        clientCommandService.sendDisconnectCommand();
    }
}
