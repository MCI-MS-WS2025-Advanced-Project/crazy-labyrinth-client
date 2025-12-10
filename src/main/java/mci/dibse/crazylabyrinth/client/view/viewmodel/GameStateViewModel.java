package mci.dibse.crazylabyrinth.client.view.viewmodel;


import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import labyrinth.contracts.models.*;
import lombok.Getter;
import lombok.Setter;
import mci.dibse.crazylabyrinth.client.domain.model.ClientPlayer;
import mci.dibse.crazylabyrinth.client.service.LobbyService;
import mci.dibse.crazylabyrinth.client.service.PlayerConnectedAckListener;
import mci.dibse.crazylabyrinth.client.view.ViewCleanup;
import mci.dibse.crazylabyrinth.client.view.viewmodel.item.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.*;

import static labyrinth.contracts.models.TurnState.*;

@Component
public class GameStateViewModel implements ViewCleanup, PlayerConnectedAckListener {

    //TODO put into subViewModel GameBoardViewModel?? turnViewModel?

    private final String WAITING_FOR_BONUS_LABEL = "setzt einen Bonus ein";
    private final String WAITING_FOR_PUSH_LABEL = "verschiebt einen Gang";
    private final String WAITING_FOR_MOVE_LABEL = "bewegt seine Spielfigur";


    private final LobbyService lobbyService;

    @Getter private GameBoardItem gameBoardItem;
    @Getter
    private  List<PlayerStateItem> playerStates = new ArrayList<>();
    @Setter
    @Getter
    private ClientPlayer clientPlayer;

    @Getter private final SimpleObjectProperty<Map<String, Object>> additionalProperties = new SimpleObjectProperty<>();
    @Getter private final SimpleObjectProperty<TreasureItem> nextTeasure = new SimpleObjectProperty<>();
    @Getter private final SimpleStringProperty currentPlayerId = new SimpleStringProperty();
    @Getter private final SimpleObjectProperty<TurnState> currentTurnState = new SimpleObjectProperty<>();


    //todo: leave here?
    /*
     *  Player Turn
     */
        private final SimpleStringProperty playerTurnId = new SimpleStringProperty();
        private final SimpleObjectProperty<TileItem> extraTile = new SimpleObjectProperty<>();
        private final SimpleIntegerProperty turnTimeLimitSeconds = new SimpleIntegerProperty();

    @Autowired
    public GameStateViewModel(LobbyService lobbyService) {
        this.lobbyService = lobbyService;
        lobbyService.addPlayerListener(this);
    }
    // private final SimpleObjectProperty<Map<String, Object>> additionalProperties = new SimpleObjectProperty<>();


    @EventListener
    public void handleGameStartEvent(GameStartedEventPayload gameStarted) {
        GameBoard initialBoard = gameStarted.getInitialBoard();
        gameBoardItem = new GameBoardItem(initialBoard);

        Arrays.stream(gameStarted.getPlayers()).map(PlayerStateItem::new).forEach(playerStates::add);
        //sort client player first then the rest according to id
        playerStates.sort(Comparator.comparing((PlayerStateItem p) ->
                        !p.getPlayerInfo().getId().equals(clientPlayer.getId()))
                .thenComparing(p -> p.getPlayerInfo().getId()));

        additionalProperties.set(gameStarted.getAdditionalProperties());
    }

    @EventListener
    public void handleNextTreasureCardEvent(NextTreasureCardEventPayload nextTreasureCard) {
        TreasureItem treasureItem = new TreasureItem(nextTreasureCard.getTreasure());
        nextTeasure.set(treasureItem);
        // TODO additional properties??
    }

    @EventListener
    public void handleGameStateUpdateEvent(GameStateUpdateEventPayload gameStateUpdate) {
        this.gameBoardItem.updateFrom(gameStateUpdate.getBoard());

        Arrays.stream(gameStateUpdate.getPlayers()).forEach(p -> {
            String incomingId =p.getPlayerInfo().getId();
            playerStates.stream().filter(playerItem -> playerItem.getPlayerInfo().getId().equals(incomingId))
                    .findFirst()
                    .ifPresentOrElse(
                            playerItem -> playerItem.updateFrom(p),
                            () -> playerStates.add(new PlayerStateItem(p))
                    );
        });
        this.currentPlayerId.set(gameStateUpdate.getCurrentPlayerId());
        this.currentTurnState.set(gameStateUpdate.getCurrentTurnState());
        //TODO additional properties
    }

    @EventListener
    public void handlePlayerTurnEvent(PlayerTurnEventPayload playerTurn) {
        playerTurnId.set(playerTurn.getPlayerId());
        extraTile.set(new TileItem(playerTurn.getExtraTile()));
        this.turnTimeLimitSeconds.set(playerTurn.getTurnTimeLimitSeconds());
    }



    @Override
    public void cleanup() {
        gameBoardItem.dispose();
        playerStates.forEach(PlayerStateItem::dispose);
        this.gameBoardItem = null;
        this.playerStates = null;
        //TODO something to do here?
    }

    public String mapTurnStateToLabel(TurnState turnState){
        return switch(turnState) {
            case WAITING_FOR_BONUS -> WAITING_FOR_BONUS_LABEL;
            case WAITING_FOR_MOVE -> WAITING_FOR_MOVE_LABEL;
            case WAITING_FOR_PUSH -> WAITING_FOR_PUSH_LABEL;
        };
    }

    @Override
    public void onPlayerConnected(ClientPlayer clientPlayer) {
        this.clientPlayer = clientPlayer;
    }



}


