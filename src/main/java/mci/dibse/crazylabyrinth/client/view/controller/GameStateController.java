package mci.dibse.crazylabyrinth.client.view.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import labyrinth.contracts.models.TurnState;
import mci.dibse.crazylabyrinth.client.view.ViewCleanup;
import mci.dibse.crazylabyrinth.client.view.viewmodel.GameStateViewModel;
import mci.dibse.crazylabyrinth.client.view.viewmodel.item.PlayerStateItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class GameStateController implements ViewCleanup {


    @Autowired
    private  GameStateViewModel vm;

    private final Map<String, Label> idToPlayerLabel = new HashMap<>();

    @FXML
    public Label playerTurnLabel;
    @FXML
    public Label player1;
    @FXML
    public Label player2;
    @FXML
    public Label player3;
    @FXML
    public Label player4;
    @FXML
    public VBox extraTileWrapper;
    @FXML
    public ImageView extraTile;
    @FXML
    public Button rotateLeftButton;
    @FXML
    public Button rotateRightButton;
    @FXML
    public GridPane boardGrid;
    @FXML
    public Button arrowTop1;
    @FXML
    public Button arrowTop3;
    @FXML
    public Button arrowTop5;
    @FXML
    public Button arrowBottom1;
    @FXML
    public Button arrowBottom3;
    @FXML
    public Button arrowBottom;
    @FXML
    public Button arrowLeft1;
    @FXML
    public Button arrowLeft3;
    @FXML
    public Button arrowLeft5;
    @FXML
    public Button arrowRight1;
    @FXML
    public Button arrowRight3;
    @FXML
    public Button arrowRight5;
    @FXML
    public ImageView nextTreasure;
    @FXML
    public Label gameCounterLabel;

    @FXML
    public void initialize() {

        // cuurent turn label
        playerTurnLabel.textProperty().bind(javafx.beans.binding.Bindings.createStringBinding(
                () -> {
                    String currentId = vm.getCurrentPlayerId().get();
                    PlayerStateItem playerStateTurn =  vm.getPlayerStates().stream().filter(playerItem -> playerItem.getPlayerInfo().getId().equals(currentId)).findFirst().get();
                    String name = playerStateTurn.getPlayerInfo().nameProperty().get();
                    String turnLabel = vm.mapTurnStateToLabel(vm.getCurrentTurnState().get());
                    return name + ": " + turnLabel;

                },vm.getCurrentPlayerId(),vm.getCurrentTurnState()
                ));


        //player labels
        List<PlayerStateItem> playerStates = vm.getPlayerStates();
        for (int i = 0; i < playerStates.size(); i++) {
            PlayerStateItem playerStateItem = playerStates.get(i);
            idToPlayerLabel.put(playerStateItem.getPlayerInfo().getId(),
                    getPlayerLabelByIndex(i));
        }


        PlayerStateItem playerStateClient =  vm.getPlayerStates().stream()
                .filter(playerItem -> playerItem.getPlayerInfo().getId().equals(vm.getClientPlayer().getId())).findFirst().get();


        idToPlayerLabel.get(vm.getClientPlayer().getId()).textProperty().bind(javafx.beans.binding.Bindings.createStringBinding(
                () -> {

                    String name = playerStateClient.getPlayerInfo().getName();

                    return name + " (" + playerStateClient.getTreasuresFound().size() + " / " + playerStateClient.getRemainingTreasureCount() + ")";
                }, playerStateClient.getTreasuresFound()
        ));

        playerStates.subList(1, playerStates.size()).forEach(playerState -> {
            Label playerLabel = idToPlayerLabel.get(playerState.getPlayerInfo().getId());
            playerLabel.textProperty().bind(javafx.beans.binding.Bindings.createStringBinding(
                    () -> {
                        String name = playerState.getPlayerInfo().getName();
                        return name + " (" + playerState.getTreasuresFound().size() + " / " + playerState.getRemainingTreasureCount() + ")";
                    }, playerState.getTreasuresFound()
            ));
        });


        // extra Tile
        extraTileWrapper.visibleProperty().bind(javafx.beans.binding.Bindings.createBooleanBinding(
                () -> {
                    String playerId = playerStateClient.getPlayerInfo().getId();
                    return  (playerId.equals(vm.getCurrentPlayerId().get())
                    && vm.getCurrentTurnState().get().equals(TurnState.WAITING_FOR_PUSH));
                },vm.getCurrentPlayerId(), vm.getCurrentTurnState()
        ));
        public ImageView extraTile;
        @FXML
        public Button rotateLeftButton;
        @FXML
        public Button rotateRightButton;

    }



    @Override
    public void cleanup() {
        //todo
    }


    private Label getPlayerLabelByIndex(int index) {
        return switch (index) {
            case 0 -> player1;
            case 1 -> player2;
            case 2 -> player3;
            case 3 -> player4;
            default -> throw new IllegalArgumentException("Invalid player index: " + index);
        };
    }

}



/*

imageView.fitWidthProperty().bind(boardGrid.widthProperty().divide(7));
imageView.fitHeightProperty().bind(boardGrid.heightProperty().divide(7));
imageView.setPreserveRatio(true);

fitWidth = totalWidth/7
fitHeight = totalHeight/7

 */