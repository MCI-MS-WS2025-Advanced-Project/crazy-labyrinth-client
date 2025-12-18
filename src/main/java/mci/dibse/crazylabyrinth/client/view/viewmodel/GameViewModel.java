package mci.dibse.crazylabyrinth.client.view.viewmodel;

import jakarta.annotation.PreDestroy;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import javafx.beans.property.SimpleObjectProperty;
import labyrinth.contracts.models.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import mci.dibse.crazylabyrinth.client.domain.model.ClientPlayer;
import mci.dibse.crazylabyrinth.client.infrastructure.util.JavaFXUtils;
import mci.dibse.crazylabyrinth.client.service.GameService;
import mci.dibse.crazylabyrinth.client.service.LobbyService;
import mci.dibse.crazylabyrinth.client.service.PlayerConnectedAckListener;
import mci.dibse.crazylabyrinth.client.view.viewmodel.item.CurrentTurnInfoItem;
import mci.dibse.crazylabyrinth.client.view.viewmodel.item.GameBoardItem;
import mci.dibse.crazylabyrinth.client.view.viewmodel.item.PlayerStateItem;
import mci.dibse.crazylabyrinth.client.view.viewmodel.item.TreasureItem;
import mci.dibse.crazylabyrinth.client.view.viewmodel.navigation.NavigationStateViewModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class GameViewModel extends BaseViewModel implements PlayerConnectedAckListener {

    private final String WAITING_FOR_BONUS_LABEL = "setzt einen Bonus ein";
    private final String WAITING_FOR_PUSH_LABEL = "verschiebt einen Gang";
    private final String WAITING_FOR_MOVE_LABEL = "bewegt seine Spielfigur";

    private final LobbyService lobbyService;
    private final GameService gameService;

    @Getter private GameBoardItem gameBoardItem;
    @Getter private List<PlayerStateItem> playerStates = new ArrayList<>();
    @Setter @Getter private ClientPlayer clientPlayer;
    @Getter private OffsetDateTime gameEndTime;

    @Getter private CurrentTurnInfoItem currentTurnInfoItem;

    @Getter
    private final SimpleObjectProperty<TreasureItem> nextTreasure = new SimpleObjectProperty<>();

    @Autowired
    public GameViewModel(
            NavigationStateViewModel vm, LobbyService lobbyService, GameService gameService) {
        super(vm);
        this.lobbyService = lobbyService;
        this.gameService = gameService;

        lobbyService.addPlayerListener(this);
    }

    @PreDestroy
    public void preDestroy() {
        lobbyService.removePlayerListener(this);
    }

    public String getCurrentPlayerId() {
        return this.currentTurnInfoItem.getCurrentPlayerId();
    }

    @Override
    public void cleanup() {
        // todo?
        gameBoardItem.dispose();
        gameBoardItem = null;
        playerStates.forEach(PlayerStateItem::dispose);
        playerStates.clear();
        currentTurnInfoItem.dispose();
        currentTurnInfoItem = null;
        clientPlayer = null;
        gameEndTime = null;
        // JavaFXUtils.runOnFxThread();
    }

    @Override
    public void onPlayerConnected(ClientPlayer clientPlayer) {
        this.clientPlayer = clientPlayer;
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
                    if (nextTreasure.get() == null) {
                        TreasureItem treasureItem =
                                new TreasureItem(nextTreasureCard.getTreasure());
                        nextTreasure.set(treasureItem);
                        return;
                    }
                    nextTreasure.get().updateFrom(nextTreasureCard.getTreasure());
                };
        JavaFXUtils.runOnFxThread(task);
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
        Arrays.stream(gameState.getPlayers()).map(PlayerStateItem::new).forEach(playerStates::add);
        // sort client player first then the rest according to id
        playerStates.sort(
                Comparator.comparing(
                                (PlayerStateItem p) ->
                                        !p.getPlayerId().equals(clientPlayer.getId()))
                        .thenComparing(PlayerStateItem::getPlayerId));

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
                            playerStates.stream()
                                    .filter(
                                            playerItem ->
                                                    playerItem.getPlayerId().equals(incomingId))
                                    .findFirst()
                                    .ifPresentOrElse(
                                            playerItem -> playerItem.updateFrom(p),
                                            () -> playerStates.add(new PlayerStateItem(p)));
                        });
        this.currentTurnInfoItem.updateFrom(gameState.getCurrentTurnInfo());
    }
}

/*

   GameStateEventPayload:
     type: object
     required: [type, board, players, currentTurnInfo]
     properties:
       type: { $ref: "#/components/schemas/EventType" }
       board: { $ref: "#/components/schemas/GameBoard" }
       players:
         type: array
         items: { $ref: "#/components/schemas/PlayerState" }
       currentTurnInfo: { $ref: "#/components/schemas/CurrentTurnInfo" }
                   gameEndTime: { type: string, format: date-time }




                       CurrentTurnInfo:
     type: object
     required: [ currentPlayerId, timeLimitSeconds, state ]
     properties:
       currentPlayerId: { type: string }
       turnEndTime: { type: string, format: date-time }
       state: { $ref: "#/components/schemas/TurnState" } --> enum




   CurrentTurnInfo:
     type: object
     required: [ currentPlayerId, timeLimitSeconds, state ]
     properties:
       currentPlayerId: { type: string }
       turnEndTime: { type: string, format: date-time }
       state: { $ref: "#/components/schemas/TurnState" }
*/
