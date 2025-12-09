package mci.dibse.crazylabyrinth.client.view.viewmodel;


import javafx.beans.property.SimpleObjectProperty;
import labyrinth.contracts.models.*;
import mci.dibse.crazylabyrinth.client.view.ViewCleanup;
import mci.dibse.crazylabyrinth.client.view.viewmodel.item.GameBoardItem;
import mci.dibse.crazylabyrinth.client.view.viewmodel.item.PlayerStateItem;
import mci.dibse.crazylabyrinth.client.view.viewmodel.item.TreasureItem;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class GameStateViewModel implements ViewCleanup {

    //TODO put into subViewModel GameBoardViewModel??
    private GameBoardItem gameBoardItem;
    private List<PlayerStateItem> playerStates;
    private final SimpleObjectProperty<Map<String, Object>> additionalProperties = new SimpleObjectProperty<>();
    private final SimpleObjectProperty<TreasureItem> nextTeasure = new SimpleObjectProperty<>();


    @EventListener
    public void handleGameStartEvent(GameStartedEventPayload gameStarted) {
        GameBoard initialBoard = gameStarted.getInitialBoard();
        gameBoardItem = new GameBoardItem(initialBoard);

        Arrays.stream(gameStarted.getPlayers()).map(PlayerStateItem::new).forEach(playerStates::add);

        additionalProperties.set(gameStarted.getAdditionalProperties());
    }

    @EventListener
    public void handleNextTreasureCardEvent(NextTreasureCardEventPayload nextTreasureCard) {
        TreasureItem treasureItem = new TreasureItem(nextTreasureCard.getTreasure());
        nextTeasure.set(treasureItem);
    }

    @EventListener
    public void handleGameStateUpdateEvent(GameStateUpdateEventPayload gameStateUpdate) {

    }

    @EventListener
    public void handlePlayerTurnEvent(PlayerTurnEventPayload playerTurn) {

    }



    @Override
    public void cleanup() {
        gameBoardItem.dispose();
        playerStates.forEach(PlayerStateItem::dispose);
        this.gameBoardItem = null;
        this.playerStates = null;
        //TODO something to do here?
    }
}


