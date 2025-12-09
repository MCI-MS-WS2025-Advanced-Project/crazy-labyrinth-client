package mci.dibse.crazylabyrinth.client.view.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import mci.dibse.crazylabyrinth.client.view.ViewCleanup;
import org.springframework.stereotype.Component;

@Component
public class GameStateController implements ViewCleanup {


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