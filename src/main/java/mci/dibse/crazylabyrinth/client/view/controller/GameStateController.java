package mci.dibse.crazylabyrinth.client.view.controller;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import labyrinth.contracts.models.Direction;
import labyrinth.contracts.models.PlayerColor;
import labyrinth.contracts.models.TurnState;
import mci.dibse.crazylabyrinth.client.infrastructure.util.PlayerPawnUtils;
import mci.dibse.crazylabyrinth.client.view.ViewCleanup;
import mci.dibse.crazylabyrinth.client.view.viewmodel.GameStateViewModel;
import mci.dibse.crazylabyrinth.client.view.viewmodel.item.GameBoardItem;
import mci.dibse.crazylabyrinth.client.view.viewmodel.item.PlayerStateItem;
import mci.dibse.crazylabyrinth.client.view.viewmodel.item.TileItem;
import mci.dibse.crazylabyrinth.client.view.viewmodel.item.TreasureItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;



import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class GameStateController implements ViewCleanup {


    @FXML public BorderPane gameContainer;
    @FXML public StackPane nextTreasurePane;
    @FXML public HBox topArrowBox;
    @FXML public VBox rightArrowBox;
    @FXML public VBox leftArrowBox;
    @FXML public HBox bottomArrowBox;
    @FXML public StackPane extraTileContainer;
    @FXML public Label extraTileTreasureLabel;

    @Autowired
    private  GameStateViewModel vm;

    private final Map<String, Label> idToPlayerLabel = new HashMap<>();

    @FXML
    public Label playerTurnLabel;
    @FXML public Label player1;
    @FXML public Circle player1Circle;
    @FXML public HBox player1Box;
    @FXML public Label player2;
    @FXML public Circle player2Circle;
    @FXML public HBox player2Box;
    @FXML public Label player3;
    @FXML public Circle player3Circle;
    @FXML public HBox player3Box;
    @FXML public Label player4;
    @FXML public Circle player4Circle;
    @FXML public HBox player4Box;
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
    public Button arrowBottom5;
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
    //todo rework
    @FXML
    //public ImageView nextTreasure;
    public Label nextTreasure;
    @FXML
    public Label gameCounterLabel;

    @FXML
    public void initialize() {

        // 1. Reset all boxes to hidden first
        player1Box.setVisible(false); player1Box.setManaged(false);
        player2Box.setVisible(false); player2Box.setManaged(false);
        player3Box.setVisible(false); player3Box.setManaged(false);
        player4Box.setVisible(false); player4Box.setManaged(false);

        // cuurent turn label
        playerTurnLabel.textProperty().bind(javafx.beans.binding.Bindings.createStringBinding(
                () -> {
                    //todo rework
                    String currentId = vm.getCurrentPlayerId().get();
                    if(currentId != null) {
                        PlayerStateItem playerStateTurn =  vm.getPlayerStates().stream().filter(playerItem -> playerItem.getPlayerInfo().getId().equals(currentId)).findFirst().get();
                        String name = playerStateTurn.getPlayerInfo().nameProperty().get();
                        String turnLabel = vm.mapTurnStateToLabel(vm.getCurrentTurnState().get());
                        return name + ": " + turnLabel;
                    } else {
                        String currentId2 = vm.getPlayerTurnId().get();
                        PlayerStateItem playerStateTurn =  vm.getPlayerStates().stream().filter(playerItem -> playerItem.getPlayerInfo().getId().equals(currentId2)).findFirst().get();
                        String name = playerStateTurn.getPlayerInfo().nameProperty().get();
                        String turnLabel = vm.mapTurnStateToLabel(TurnState.WAITING_FOR_PUSH);
                        return name + ": " + turnLabel;
                    }
                },vm.getCurrentPlayerId(),vm.getCurrentTurnState()
                ));


        //player labels
        List<PlayerStateItem> playerStates = vm.getPlayerStates();
        for (int i = 0; i < playerStates.size(); i++) {
            // Show the box
            javafx.scene.layout.HBox box = getPlayerBoxByIndex(i);
            box.setVisible(true);
            box.setManaged(true);

            PlayerStateItem playerStateItem = playerStates.get(i);
            getPlayerCircleByIndex(i).setFill(PlayerPawnUtils.mapPlayerColorToFxColor(playerStateItem.getPlayerInfo().getColor()));
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
        /*
        extraTileWrapper.visibleProperty().bind(javafx.beans.binding.Bindings.createBooleanBinding(
                () -> {

                    String playerId = playerStateClient.getPlayerInfo().getId();
                    return  (playerId.equals(vm.getCurrentPlayerId().get())
                    && vm.getCurrentTurnState().get().equals(TurnState.WAITING_FOR_PUSH));
                },vm.getCurrentPlayerId(), vm.getCurrentTurnState()
        ));
         */
        extraTileWrapper.visibleProperty().set(true);
      //  extraTileTreasureLabel.visibleProperty().bind(vm.getExtraTile().get().getTreasure().isNotNull());
        extraTileTreasureLabel.visibleProperty().set(true);

        extraTileTreasureLabel.textProperty().bind(
                Bindings.createStringBinding(() -> {
                    TileItem tile = vm.getExtraTile().get();
                    if (tile != null && tile.getTreasure().get() != null) {
                        return String.valueOf(tile.getTreasure().get().getId().get());
                    } else {
                        return "";
                    }
                }, vm.getExtraTile())
        );



        vm.getExtraTile().addListener((observable, oldValue, newValue) -> {
            System.out.println("extra tile changed");
            if (oldValue != null) {
                oldValue.dispose();
            }
            extraTile.imageProperty().unbind();
            extraTile.rotateProperty().unbind();
            // Add null check for newValue
            if (newValue != null) {
                extraTile.imageProperty().bind(
                        javafx.beans.binding.Bindings.createObjectBinding(
                                () -> {
                                    System.out.println("extra tile image binding");
                                    return newValue.getTileImage().get();
                                }, vm.getExtraTile(), newValue.getRotation()
                        ));
                extraTile.rotateProperty().bindBidirectional(newValue.getRotation());
            }
        });
        if (vm.getExtraTile().get() != null) {
            TileItem currentTile = vm.getExtraTile().get();
            extraTile.imageProperty().bind(
                    javafx.beans.binding.Bindings.createObjectBinding(
                            () -> currentTile.getTileImage().get(),
                            vm.getExtraTile(), currentTile.getRotation()
                    ));
            extraTile.rotateProperty().bindBidirectional(currentTile.getRotation());
        }



        // extraTile.setImage(vm.getExtraTile().get().getTileImage().get());
       // extraTile.setRotate(vm.getExtraTile().get().getRotation().get().doubleValue());


        rotateLeftButton.visibleProperty().bind(Bindings.createBooleanBinding(
                () -> {
                    String playerId = playerStateClient.getPlayerInfo().getId();
                    if(!vm.getCurrentTurnState().isNull().get()){
                        return  (playerId.equals(vm.getCurrentPlayerId().get())
                                && vm.getCurrentTurnState().get().equals(TurnState.WAITING_FOR_PUSH));
                    } else {
                        if (vm.getPlayerTurnId().get().equals(playerId)) {
                            return true;
                        } else {
                            return false;
                        }
                    }
                },vm.getCurrentPlayerId(), vm.getCurrentTurnState()
        ));
        rotateRightButton.visibleProperty().bind(Bindings.createBooleanBinding(
                () -> {
                    String playerId = playerStateClient.getPlayerInfo().getId();
                    if(!vm.getCurrentTurnState().isNull().get()) {
                        return  (playerId.equals(vm.getCurrentPlayerId().get())
                                && vm.getCurrentTurnState().get().equals(TurnState.WAITING_FOR_PUSH));
                    } else {
                        if (vm.getPlayerTurnId().get().equals(playerId)) {
                            return true;
                        } else {
                            return false;
                        }
                    }
                },vm.getCurrentPlayerId(), vm.getCurrentTurnState()
        ));

        rotateLeftButton.setOnAction(
                _ ->  vm.getExtraTile().get().onLeftRotation()
        );
        rotateRightButton.setOnAction(
                _ ->  {
                    vm.getExtraTile().get().onRightRotation();
                }
        );

        initGameGrid();
        /*
        vm.getGameBoardItem().tilesProperty().addListener((observable, oldValue, newValue) -> {
            initGameGrid();
        });

         */

        vm.getCurrentTurnState().addListener((observable, oldValue, newValue) -> {
            initGameGrid();
        });

        initPushTileButtons();

        initNextTreasure();


        initGameCounter();
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
    private Circle getPlayerCircleByIndex(int index) {
        return switch (index) {
            case 0 -> player1Circle;
            case 1 -> player2Circle;
            case 2 -> player3Circle;
            case 3 -> player4Circle;
            default -> throw new IllegalArgumentException("Invalid player index: " + index);
        };
    }


    private void initGameGrid() {
        Runnable apply = () -> {


            //debug  Clear constraints
            boardGrid.getColumnConstraints().clear();
            boardGrid.getRowConstraints().clear();
            boardGrid.setGridLinesVisible(true);


            if (gameContainer.getParent() instanceof javafx.scene.layout.Region parent) {
                var squareSize = javafx.beans.binding.Bindings.min(parent.widthProperty(), parent.heightProperty())
                        .subtract(20); // padding

                gameContainer.minWidthProperty().bind(squareSize);
                gameContainer.minHeightProperty().bind(squareSize);
                gameContainer.maxWidthProperty().bind(squareSize);
                gameContainer.maxHeightProperty().bind(squareSize);
            }



            GameBoardItem gameBoardItem = vm.getGameBoardItem();

            //todo rework for other game board sizes

            // Populate the board with ImageViews for every cell
            for (int row = 0; row < gameBoardItem.getRows(); row++) {
                for (int col = 0; col < gameBoardItem.getCols(); col++) {

                    StackPane cellContainer = new StackPane();
                    // Crucial: Allow the container to shrink below the image's intrinsic size
                    cellContainer.setMinSize(0, 0);
                    cellContainer.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
                    // Tell GridPane this child may grow to fill its cell:
                    GridPane.setHgrow(cellContainer, Priority.ALWAYS);
                    GridPane.setVgrow(cellContainer, Priority.ALWAYS);
                    // Total horizontal deduction:  gaps (2px * (7-1) = 12)
                    double horizontalDeduction = 12;
                    double verticalDeduction   = 12;

                    cellContainer.prefWidthProperty().bind(
                            boardGrid.widthProperty().subtract(horizontalDeduction).divide(7)
                    );
                    cellContainer.prefHeightProperty().bind(
                            boardGrid.heightProperty().subtract(verticalDeduction).divide(7)
                    );

                    TileItem[][] tiles = gameBoardItem.getTiles();


                    TileItem tile = tiles[row][col];
                    ImageView imageView = new ImageView(tile.getTileImage().get());
                    imageView.setRotate(tile.getRotation().get().doubleValue());


                  //  System.out.println("tile at board: row" + row + " col: " + col + "  directions"   + tile.getDirections().toString() + " rotation: " + tile.getRotation().get() + " treasure: " + tile.getTreasure().get());


                            //todo ?
                    // Bind image dimensions to the grid cell size
                    // Subtracting 32px accounts for padding (10+10) and gaps (2*6) to prevent overflow
                   // imageView.fitWidthProperty().bind(boardGrid.widthProperty().subtract(32).divide(7));
                   // imageView.fitHeightProperty().bind(boardGrid.heightProperty().subtract(32).divide(7));
                   // imageView.setPreserveRatio(true);
                    // Bind image to the cell container, not the whole grid
                    imageView.fitWidthProperty().bind(cellContainer.widthProperty());
                    imageView.fitHeightProperty().bind(cellContainer.heightProperty());
                    imageView.setPreserveRatio(true);

                    // Add to the grid at the specified column and row
                    cellContainer.getChildren().add(imageView);


                    //todo use decicated treasure image
                    //    ImageView treasureImage = new ImageView(new Image());
                    if (tile.getTreasure().get() != null) {
                        Label treasureNode = tempLabelImplforTreasure(tile.getTreasure().get());
                        cellContainer.getChildren().add(treasureNode);
                    }



                    int finalRow = row;
                    int finalCol = col;
                    Optional<PlayerStateItem> playerStateItemOptional = vm.getPlayerStates().stream().filter(player -> player.getCurrentPosition().getY().get() == finalCol
                            && player.getCurrentPosition().getX().get() == finalRow).findFirst();
                    if (playerStateItemOptional.isPresent()) {
                        PlayerColor playerColor = playerStateItemOptional.get().getPlayerInfo().colorProperty().get();
                        System.out.println("Player found at: " + playerStateItemOptional.get().getPlayerInfo().getName()+ " -> x: " + finalRow + ", y:" + finalCol);
                        Circle playerPawn = new Circle(10, PlayerPawnUtils.mapPlayerColorToFxColor(playerColor));
                        cellContainer.getChildren().add(playerPawn);
                    }


                    //set mouse clicks tuff for move Phase
                    cellContainer.setOnMouseClicked(event -> {
                        System.out.println("Clicked: " + finalRow + ", " + finalCol);
                         vm.movePawnCommand(finalRow, finalCol);
                    });
                    BooleanBinding isMyTurnAndMovePhase = Bindings.createBooleanBinding(
                            () -> {
                                return vm.getPlayerTurnId().get().equals(vm.getClientPlayer().getId())
                                        && ( vm.getCurrentTurnState().get() != null && vm.getCurrentTurnState().get().equals(TurnState.WAITING_FOR_MOVE));
                            }, vm.getCurrentPlayerId(), vm.getCurrentTurnState()
                    );
                    boardGrid.add(cellContainer, col, row);
                    cellContainer.mouseTransparentProperty().bind(isMyTurnAndMovePhase.not());
                    cellContainer.cursorProperty().bind(
                            javafx.beans.binding.Bindings.when(isMyTurnAndMovePhase)
                                    .then(javafx.scene.Cursor.HAND)
                                    .otherwise(javafx.scene.Cursor.DEFAULT)
                    );

                }
            }
            initArrowAlignments();
        };
        if (Platform.isFxApplicationThread()) apply.run();
        else Platform.runLater(apply);


    }
    //todo rework for dynamic buttons with different board sizes
    private void initPushTileButtons() {
        arrowTop1.setOnAction(_ -> pushTileCommand(1, Direction.DOWN, vm.getExtraTile().get().getDirections().toArray(Direction[]::new)));
        arrowTop3.setOnAction(_ -> pushTileCommand(3, Direction.DOWN, vm.getExtraTile().get().getDirections().toArray(Direction[]::new)));
        arrowTop5.setOnAction(_ -> pushTileCommand(5, Direction.DOWN, vm.getExtraTile().get().getDirections().toArray(Direction[]::new)));
        arrowBottom1.setOnAction(_ -> pushTileCommand(1, Direction.UP, vm.getExtraTile().get().getDirections().toArray(Direction[]::new)));
        arrowBottom3.setOnAction(_ -> pushTileCommand(3, Direction.UP, vm.getExtraTile().get().getDirections().toArray(Direction[]::new)));
        arrowBottom5.setOnAction(_ -> pushTileCommand(5, Direction.UP, vm.getExtraTile().get().getDirections().toArray(Direction[]::new)));
        arrowLeft1.setOnAction(_ -> pushTileCommand(1, Direction.RIGHT, vm.getExtraTile().get().getDirections().toArray(Direction[]::new)));
        arrowLeft3.setOnAction(_ -> pushTileCommand(3, Direction.RIGHT, vm.getExtraTile().get().getDirections().toArray(Direction[]::new)));
        arrowLeft5.setOnAction(_ -> pushTileCommand(5, Direction.RIGHT, vm.getExtraTile().get().getDirections().toArray(Direction[]::new)));
        arrowRight1.setOnAction(_ -> pushTileCommand(1, Direction.LEFT, vm.getExtraTile().get().getDirections().toArray(Direction[]::new)));
        arrowRight3.setOnAction(_ -> pushTileCommand(3, Direction.LEFT, vm.getExtraTile().get().getDirections().toArray(Direction[]::new)));
        arrowRight5.setOnAction(_ -> pushTileCommand(5, Direction.LEFT, vm.getExtraTile().get().getDirections().toArray(Direction[]::new)));
    }

    private void pushTileCommand(int index, Direction pushDirection, Direction[] extraTileEntrances) {
        if (vm.getGameBoardItem().lastPushProperty().get() != null) {
            if (! (vm.getGameBoardItem().lastPushProperty().get().getDirection().equals(pushDirection)
                    && vm.getGameBoardItem().getLastPush().getRowOrColIndex().equals(index))) {
                vm.pushTileCommand(index, pushDirection, extraTileEntrances);
            } else {
                showAlertModal("Invalid push", "Invalid push", "You cannot push the tile in the same direction as the last push!");
            }
        } else {
            vm.pushTileCommand(index, pushDirection, extraTileEntrances);
        }
    }



    //todo rework
    private void initNextTreasure() {
        /*
        nextTreasure.setStyle(
                "-fx-font-size: 24px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-text-fill: white; " +
                        "-fx-effect: dropshadow(three-pass-box, black, 10, 0, 0, 0);"
            );

         */
        nextTreasure.textProperty().bind(Bindings.createStringBinding(
                () -> {
                    System.out.println("nex treasure: " + vm.getNextTeasure().get().getName().get());
                    return  vm.getNextTeasure().get().getName().get();
                }
                , vm.getNextTeasure()
                ));
       // nextTreasurePane.getChildren().add(nextTreasure);
    }

    private void  initArrowAlignments() {

        topArrowBox.getChildren().clear();


        addSlotsToBox(topArrowBox, arrowTop1, arrowTop3, arrowTop5, true);


        bottomArrowBox.getChildren().clear();

        addSlotsToBox(bottomArrowBox, arrowBottom1, arrowBottom3, arrowBottom5, true);


        leftArrowBox.getChildren().clear();

        addSlotsToBox(leftArrowBox, arrowLeft1, arrowLeft3, arrowLeft5, false);


        rightArrowBox.getChildren().clear();

        addSlotsToBox(rightArrowBox, arrowRight1, arrowRight3, arrowRight5, false);
    }

    private void addSlotsToBox(Pane box, Button b1, Button b2, Button b3, boolean horizontal) {
        // Grid columns/rows: 0..6
        // Buttons are at indices 1, 3, 5
        for (int i = 0; i < 7; i++) {
            if (i == 1) addBoundItem(box, b1, horizontal);
            else if (i == 3) addBoundItem(box, b2, horizontal);
            else if (i == 5) addBoundItem(box, b3, horizontal);
            else {
                Region spacer = new Region();
                addBoundItem(box, spacer, horizontal);
            }
        }
    }
    private void addBoundItem(Pane box, Region item, boolean horizontal) {

        if (horizontal) {
            item.prefWidthProperty().bind(boardGrid.widthProperty().divide(7));

            if (item instanceof Button) {
                ((Button) item).setMaxWidth(Double.MAX_VALUE); // Fill slot? or keep size?
                // If we want the button to be exactly centered in the slot, the slot is the Region/Button.
                // By binding prefWidth, the Button BECOMES the slot size.
            }
        } else {
            item.prefHeightProperty().bind(boardGrid.heightProperty().divide(7));
            if (item instanceof Button) {
                 ((Button) item).setMaxHeight(Double.MAX_VALUE);
            }
        }
        box.getChildren().add(item);
    }



    private Label tempLabelImplforTreasure(TreasureItem treasureItem) {
        String treasureId = String.valueOf(treasureItem.getId().get());
        Label treasureNode = new Label(treasureId);
        // Style it to look clear on top of the tile
        treasureNode.setStyle(
                "-fx-font-size: 24px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-text-fill: white; " +
                        "-fx-effect: dropshadow(three-pass-box, black, 10, 0, 0, 0);"
        );
        return treasureNode;
    }

    private void initGameCounter() {
        //todo how is the set timer send from server?
    }


    private void showAlertModal(String title, String header, String content) {
        Runnable apply =
                () -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle(title);
                    alert.setHeaderText(header);
                    alert.setContentText(content);
                    alert.show();
                };
        if (Platform.isFxApplicationThread()) apply.run();
        else Platform.runLater(apply);
    }

    // Helper to get boxes
    private javafx.scene.layout.HBox getPlayerBoxByIndex(int index) {
        return switch (index) {
            case 0 -> player1Box;
            case 1 -> player2Box;
            case 2 -> player3Box;
            case 3 -> player4Box;
            default -> throw new IllegalArgumentException("Invalid index");
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