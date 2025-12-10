package mci.dibse.crazylabyrinth.client.view.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import mci.dibse.crazylabyrinth.client.view.ViewCleanup;
import mci.dibse.crazylabyrinth.client.view.viewmodel.GameStateViewModel;
import mci.dibse.crazylabyrinth.client.view.viewmodel.item.PlayerStateItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.script.Bindings;

@Component
public class GameStateController implements ViewCleanup {

    @Autowired
    private  GameStateViewModel vm;


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
        playerTurnLabel.textProperty().bind(javafx.beans.binding.Bindings.createStringBinding(
                () -> {
                    String currentId = vm.getCurrentPlayerId().get();
                    PlayerStateItem playerState =  vm.getPlayerStates().stream().filter(playerItem -> playerItem.getPlayerInfo().getId().equals(currentId)).findFirst().get();
                    String name = playerState.getPlayerInfo().nameProperty().get();
                    String turnLabel = vm.mapTurnStateToLabel(vm.getCurrentTurnState().get());
                    return name + " " + turnLabel;

                },vm.getCurrentPlayerId(),vm.getCurrentTurnState()
                ));



    }



    @Override
    public void cleanup() {
        //todo
    }
}



/*

imageView.fitWidthProperty().bind(boardGrid.widthProperty().divide(7));
imageView.fitHeightProperty().bind(boardGrid.heightProperty().divide(7));
imageView.setPreserveRatio(true);

fitWidth = totalWidth/7
fitHeight = totalHeight/7

 */