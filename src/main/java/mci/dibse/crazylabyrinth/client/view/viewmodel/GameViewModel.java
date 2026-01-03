package mci.dibse.crazylabyrinth.client.view.viewmodel;

import jakarta.annotation.PreDestroy;
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
import mci.dibse.crazylabyrinth.client.domain.model.ClientPlayer;
import mci.dibse.crazylabyrinth.client.infrastructure.util.JavaFXUtils;
import mci.dibse.crazylabyrinth.client.service.ClientCommandService;
import mci.dibse.crazylabyrinth.client.service.GameService;
import mci.dibse.crazylabyrinth.client.service.LobbyService;
import mci.dibse.crazylabyrinth.client.service.PlayerConnectedAckListener;
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

    @Getter private GameBoardItem gameBoardItem;
    @Getter private List<PlayerStateItem> playerStatesSorted = new ArrayList<>();
    @Setter @Getter private ClientPlayer clientPlayer;
    @Getter private OffsetDateTime gameEndTime;

    @Getter private CurrentTurnInfoItem currentTurnInfoItem;

    @Getter
    private final SimpleObjectProperty<TreasureItem> nextTreasure = new SimpleObjectProperty<>();

    @Getter final SimpleBooleanProperty pendingRotation = new SimpleBooleanProperty();
    @Getter final SimpleBooleanProperty turnTimeout = new SimpleBooleanProperty();

    private ChangeListener<TileItem> spareTileListener;
    private ChangeListener<TurnState> turnStateChangeListener;

    @Autowired
    public GameViewModel(
            NavigationStateViewModel vm,
            LobbyService lobbyService,
            GameService gameService,
            ClientCommandService clientCommandService) {
        super(vm);
        this.lobbyService = lobbyService;
        this.gameService = gameService;
        this.clientCommandService = clientCommandService;
        pendingRotation.set(false);

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
        return mapTurnStateToLabel(currentTurnInfoItem.getTurnState().get());
    }

    public boolean isClientPlayerTimeout() {
        return turnTimeout.get();
    }
    public SimpleStringProperty getCurrentPlayerIdProperty() {
        return currentTurnInfoItem.getCurrentPlayerId();
    }

    public TileItem getSpareTileItem() {
        return gameBoardItem.getSpareTile();
    }

    public ObjectProperty<TileItem> getSpareTileProperty() {
        return gameBoardItem.getSpareTileProperty();
    }

    public TurnState getCurrentTurnState() {
        return currentTurnInfoItem.getTurnState().get();
    }

    public SimpleObjectProperty<TurnState> getCurrentTurnStateProperty() {
        return currentTurnInfoItem.getTurnState();
    }

    public String getNextTreasureNameAsString() {
        return nextTreasure.get().getName();
    }

    public boolean isPendingRotation() {
        return pendingRotation.get();
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
        System.out.println("Cleanup GameViewModel");
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
        turnTimeout.set(false);
    }

    @EventListener
    public void handleGameStateEvent(GameStateEventPayload gameState) {
        Runnable task =
                () -> {
                    if (EventType.GAME_STARTED.equals(gameState.getType())) {
                        gameStarted(gameState);
                        return;
                    }
                    gameUpdated(gameState);
                };
        JavaFXUtils.runOnFxThread(task);
    }

    @EventListener
    public void handleTreasureCardEvent(NextTreasureCardEventPayload nextTreasureCard) {
        Runnable task =
                () -> {
                    System.out.println("set treasure");
                    TreasureItem treasureItem = new TreasureItem(nextTreasureCard.getTreasure());
                    nextTreasure.set(treasureItem);
                };
        JavaFXUtils.runOnFxThread(task);
    }

    @EventListener
    public void handleTurnTimeout(ApplicationEvents event) {
        if (!ApplicationEvents.TURN_TIMEOUT.equals(event)) return;
        log.debug("handle turn timeout");
        Runnable task = () -> turnTimeout.set(true);
        JavaFXUtils.runOnFxThread(task);
    }

    @EventListener
    public void handlePlayerUpdate(PlayerUpdatedEventPayload event) {
        log.debug("handle player update");
        String incomingPlayerId = event.getPlayer().getId();
        playerStatesSorted.stream().filter(player -> player.getPlayerIdAsString().equals(incomingPlayerId))
                .findFirst().orElseThrow()
                .updatePlayerInfoFrom(event.getPlayer());
    }

    public String mapTurnStateToLabel(TurnState turnState) {
        return switch (turnState) {
            case WAITING_FOR_MOVE -> WAITING_FOR_MOVE_LABEL;
            case WAITING_FOR_PUSH -> WAITING_FOR_PUSH_LABEL;
        };
    }

    public void movePawnCommand(int finalRow, int finalCol) {
        clientCommandService.sendMovePawnCommand(finalRow, finalCol);
    }

    public void pushTileCommand(
            int index, Direction pushDirection, Direction[] extraTileEntrances) {
        clientCommandService.sendPushTileCommand(index, pushDirection, extraTileEntrances);
    }

    public void onLeftRotationOfSpareTile() {}

    public void onRightRotationOfSpareTile() {
        clientCommandService.sendRotateTileCommand();
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
        return NavigationStateViewModel.ScreenId.GAMEBOARD.equals(
                navigationStateViewModel.getCurrentScreen().get());
    }

    private void gameStarted(GameStateEventPayload gameState) {
        GameBoard initialBoard = gameState.getBoard();
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
        this.pendingRotation.set(false);
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
}
