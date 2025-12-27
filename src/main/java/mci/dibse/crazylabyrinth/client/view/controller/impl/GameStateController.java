package mci.dibse.crazylabyrinth.client.view.controller.impl;

import java.util.*;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import labyrinth.contracts.models.*;
import lombok.extern.slf4j.Slf4j;
import mci.dibse.crazylabyrinth.client.infrastructure.util.JavaFXUtils;
import mci.dibse.crazylabyrinth.client.infrastructure.util.PlayerPawnUtils;
import mci.dibse.crazylabyrinth.client.service.AnimationService;
import mci.dibse.crazylabyrinth.client.view.viewmodel.GameViewModel;
import mci.dibse.crazylabyrinth.client.view.viewmodel.item.*;
import mci.dibse.crazylabyrinth.client.view.viewmodel.navigation.NavigationStateViewModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class GameStateController extends BaseViewCleanup {

    private static final int GAP_BETWEEN_TILES = 2;
    private static final int MAX_PLAYERS = 4;

    @FXML public BorderPane gameContainer;
    @FXML public GridPane boardGrid;
    @FXML public HBox topArrowBox;
    @FXML public VBox rightArrowBox;
    @FXML public VBox leftArrowBox;
    @FXML public HBox bottomArrowBox;

    @FXML public StackPane nextTreasurePane;
    // todo rework
    // @FXML public ImageView nextTreasure;
    @FXML public Label nextTreasure;

    @FXML public StackPane extraTileContainer;
    @FXML public Label extraTileTreasureLabel;
    @FXML public Label extraTileBonusLabel;
    @FXML public VBox extraTileWrapper;
    @FXML public ImageView extraTile;
    // @FXML public Button rotateLeftButton;
    @FXML public Button rotateRightButton;

    @FXML public VBox bonusBox;
    @FXML public HBox bonusSwapBox;
    @FXML public Label bonusSwapCount;
    @FXML public Button bonusSwapButton;
    @FXML public HBox bonusBeamBox;
    @FXML public Label bonusBeamCount;
    @FXML public Button bonusBeamButton;
    @FXML public HBox bonusPushTwiceBox;
    @FXML public Label bonusPushTwiceCount;
    @FXML public Button bonusPushTwiceButton;
    @FXML public HBox bonusPushFixedBox;
    @FXML public Label bonusPushFixedCount;
    @FXML public Button bonusPushFixedButton;

    @FXML public Label playerTurnLabel;
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

    @FXML public Label gameCounterLabel;
    @FXML public StackPane centerWrapper;

    private final GameViewModel gameViewModel;
    private final Map<String, Label> idToPlayerLabel = new HashMap<>();
    private final Map<String, Circle> idToPlayerPawn = new HashMap<>();

    private final AnimationService animationService;

    @Autowired
    public GameStateController(NavigationStateViewModel vm, GameViewModel gameViewModel) {
        super(vm);
        this.gameViewModel = gameViewModel;
        this.animationService = new AnimationService();
    }

    @FXML
    public void initialize() {

        initPlayerTurnLabel();

        initPlayerLabels();

        initSpareTile();

        initGameGrid();

        ChangeListener<TurnState> turnStateChangeListener =
                (observable, oldValue, newValue) -> {
                    if (animationService.isAnimationInProgress()) {
                        Platform.runLater(this::waitForAnimationsAndRecreateGrid);
                    } else {
                        initGameGrid();
                    }
                };
        gameViewModel.addListenerToCurrentTurnStateProperty(turnStateChangeListener);

        initPlayerMovementListener();

        initTilePushListener();

        initPushTileButtons();

        initNextTreasure();

        initGameCounter();

        initBoni();
    }

    private void waitForAnimationsAndRecreateGrid() {
        if (animationService.isAnimationInProgress()) {
            Platform.runLater(this::waitForAnimationsAndRecreateGrid);
        } else {
            initGameGrid();
        }
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
    public void cleanup() {
        // todo
        playerTurnLabel.textProperty().unbind();
        playerTurnLabel.graphicProperty().unbind();
        idToPlayerLabel.values().forEach(l -> l.textProperty().unbind());
        extraTileTreasureLabel.textProperty().unbind();
        extraTile.imageProperty().unbind();
        extraTile.rotateProperty().unbind();
        rotateRightButton.visibleProperty().unbind();

        nextTreasure.textProperty().unbind();

        bonusBox.visibleProperty().unbind();
        bonusBox.managedProperty().unbind();

        bonusSwapBox.visibleProperty().unbind();
        bonusSwapBox.managedProperty().unbind();
        bonusSwapCount.textProperty().unbind();

        bonusBeamBox.visibleProperty().unbind();
        bonusBeamBox.managedProperty().unbind();
        bonusBeamCount.textProperty().unbind();

        bonusPushTwiceBox.visibleProperty().unbind();
        bonusPushTwiceBox.managedProperty().unbind();
        bonusPushTwiceCount.textProperty().unbind();

        bonusPushFixedBox.visibleProperty().unbind();
        bonusPushFixedBox.managedProperty().unbind();
        bonusPushFixedCount.textProperty().unbind();
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

    private void initPlayerTurnLabel() {

        playerTurnLabel
                .textProperty()
                .bind(
                        Bindings.createStringBinding(
                                () -> {
                                    String name = "";
                                    String turnLabel = "";
                                    String currentId = gameViewModel.getCurrentPlayerIdAsString();
                                    Optional<PlayerStateItem> playerStateTurnOptional =
                                            gameViewModel.getPlayerStatesSorted().stream()
                                                    .filter(
                                                            playerItem ->
                                                                    playerItem
                                                                            .getPlayerIdAsString()
                                                                            .equals(currentId))
                                                    .findFirst();
                                    if (playerStateTurnOptional.isPresent()) {
                                        PlayerStateItem playerStateTurn =
                                                playerStateTurnOptional.get();
                                        name = playerStateTurn.getPlayerNameAsString();
                                        turnLabel =
                                                gameViewModel.getCurrentTurnStateLabelAsString();
                                    }
                                    return name + ": " + turnLabel;
                                },
                                gameViewModel.getCurrentPlayerIdProperty(),
                                gameViewModel.getCurrentTurnStateProperty()));
        playerTurnLabel
                .graphicProperty()
                .bind(
                        Bindings.createObjectBinding(
                                () -> {
                                    String currentId = gameViewModel.getCurrentPlayerIdAsString();
                                    PlayerStateItem playerStateTurn = null;
                                    if (currentId != null) {
                                        playerStateTurn =
                                                gameViewModel.getPlayerStatesSorted().stream()
                                                        .filter(
                                                                playerItem ->
                                                                        playerItem
                                                                                .getPlayerIdAsString()
                                                                                .equals(currentId))
                                                        .findFirst()
                                                        .orElse(null);
                                    }

                                    if (playerStateTurn != null) {
                                        return new Circle(
                                                6,
                                                PlayerPawnUtils.mapPlayerColorToFxColor(
                                                        playerStateTurn.getPlayerColor()));
                                    }
                                    return null;
                                },
                                gameViewModel.getCurrentPlayerIdProperty(),
                                gameViewModel.getCurrentTurnStateProperty()));
    }

    private void initPlayerLabels() {

        player1Box.setVisible(false);
        player1Box.setManaged(false);
        player2Box.setVisible(false);
        player2Box.setManaged(false);
        player3Box.setVisible(false);
        player3Box.setManaged(false);
        player4Box.setVisible(false);
        player4Box.setManaged(false);

        List<PlayerStateItem> playerStates = gameViewModel.getPlayerStatesSorted();
        String clientPlayerId = gameViewModel.getClientPlayer().getId();

        for (int i = 0; i < playerStates.size(); i++) {

            HBox box = getPlayerBoxByIndex(i);
            box.setVisible(true);
            box.setManaged(true);

            PlayerStateItem playerStateItem = playerStates.get(i);
            getPlayerCircleByIndex(i)
                    .setFill(
                            PlayerPawnUtils.mapPlayerColorToFxColor(
                                    playerStateItem.getPlayerColor()));
            idToPlayerLabel.put(playerStateItem.getPlayerIdAsString(), getPlayerLabelByIndex(i));
        }

        // clientPlayer always first
        Label clientLabel = idToPlayerLabel.get(clientPlayerId);
        PlayerStateItem clientState = gameViewModel.getClientPlayerState();
        bindPlayerLabel(clientLabel, clientState, true);

        playerStates
                .subList(1, playerStates.size())
                .forEach(
                        playerState -> {
                            Label playerLabel =
                                    idToPlayerLabel.get(playerState.getPlayerIdAsString());
                            bindPlayerLabel(playerLabel, playerState, false);
                        });
    }

    private void bindPlayerLabel(Label label, PlayerStateItem player, boolean isClient) {
        String maxAvailableTreasurePerPlayer =
                String.valueOf(player.getRemainingTreasureCountAsInt());
        label.textProperty().unbind();
        label.textProperty()
                .bind(
                        Bindings.createStringBinding(
                                () -> {
                                    String prefix = isClient ? "Du: " : "";
                                    String name = player.getPlayerNameAsString();
                                    int found = player.getTreasuresFound().size();
                                    return prefix
                                            + name
                                            + " ("
                                            + found
                                            + " / "
                                            + maxAvailableTreasurePerPlayer
                                            + ")";
                                },
                                player.getTreasuresFound()));
    }

    private void initSpareTile() {
        extraTileWrapper.visibleProperty().set(true);
        extraTileTreasureLabel.visibleProperty().set(true);

        extraTileTreasureLabel
                .textProperty()
                .bind(
                        Bindings.createStringBinding(
                                () -> {
                                    TileItem tile = gameViewModel.getSpareTileItem();
                                    if (tile != null && tile.getTreasureItem() != null) {
                                        return String.valueOf(tile.getTreasure().get().getId());
                                    } else {
                                        return "";
                                    }
                                },
                                gameViewModel.getGameBoardItem().getSpareTileProperty()));

        extraTileBonusLabel.visibleProperty().set(true);
        extraTileBonusLabel
                .textProperty()
                .bind(
                        Bindings.createStringBinding(
                                () -> {
                                    TileItem tile = gameViewModel.getSpareTileItem();
                                    if (tile != null && tile.getBonusType() != null) {
                                        return tile.getBonusType().getValue().toLowerCase();
                                    } else {
                                        return "";
                                    }
                                },
                                gameViewModel.getGameBoardItem().getSpareTileProperty()));

        ChangeListener<TileItem> spareTileListener =
                (obs, oldValue, newValue) -> {
                    if (oldValue != null) {
                        oldValue.dispose();
                    }
                    extraTile.imageProperty().unbind();
                    extraTile.rotateProperty().unbind();
                    // Add null check for newValue
                    if (newValue != null) {
                        extraTile
                                .imageProperty()
                                .bind(
                                        Bindings.createObjectBinding(
                                                () -> newValue.getTileImage().get(),
                                                gameViewModel.getSpareTileProperty(),
                                                newValue.getRotation()));
                        extraTile.rotateProperty().bindBidirectional(newValue.getRotation());
                    }
                };

        gameViewModel.addListenerToSpareTileProperty(spareTileListener);
        if (gameViewModel.getSpareTileItem() != null) {
            TileItem currentTile = gameViewModel.getSpareTileItem();
            extraTile
                    .imageProperty()
                    .bind(
                            Bindings.createObjectBinding(
                                    () -> currentTile.getTileImage().get(),
                                    gameViewModel.getSpareTileProperty(),
                                    currentTile.getRotation()));
            extraTile.rotateProperty().bindBidirectional(currentTile.getRotation());
        }

        /*
               rotateLeftButton.visibleProperty().bind(Bindings.createBooleanBinding(
                       () -> {
                           String playerId = playerStateClient.getPlayerIdAsString();
                               return  (playerId.equals(gameViewModel.getCurrentPlayerIdAsString())
                                       && gameViewModel.getCurrentTurnState().equals(TurnState.WAITING_FOR_PUSH));
                       }, gameViewModel.getCurrentPlayerIdProperty(), gameViewModel.getCurrentTurnStateProperty()
               ));
        */

        rotateRightButton
                .visibleProperty()
                .bind(
                        Bindings.createBooleanBinding(
                                () -> {
                                    if (gameViewModel.isPendingRotation()) return false;
                                    String playerId =
                                            gameViewModel
                                                    .getClientPlayerState()
                                                    .getPlayerIdAsString();
                                    return (playerId.equals(
                                                    gameViewModel.getCurrentPlayerIdAsString())
                                            && gameViewModel
                                                    .getCurrentTurnState()
                                                    .equals(TurnState.WAITING_FOR_PUSH));
                                },
                                gameViewModel.getCurrentPlayerIdProperty(),
                                gameViewModel.getCurrentTurnStateProperty(),
                                gameViewModel.getPendingRotation()));

        rotateRightButton.setOnAction(
                _ -> {
                    //  gameViewModel.getSpareTileItem().onRightRotation();
                    gameViewModel.getPendingRotation().set(true);
                    gameViewModel.onRightRotationOfSpareTile();
                });
    }

    private void initGameGrid() {
        GameBoardItem gameBoardItem = gameViewModel.getGameBoardItem();
        int rows = gameBoardItem.getRows();
        int cols = gameBoardItem.getCols();
        Runnable apply =
                () -> {
                    boardGrid.getChildren().clear();
                    boardGrid.getColumnConstraints().clear();
                    boardGrid.getRowConstraints().clear();

                    boardGrid.setGridLinesVisible(false);
                    boardGrid.setHgap(GAP_BETWEEN_TILES);
                    boardGrid.setVgap(GAP_BETWEEN_TILES);
                    boardGrid.setSnapToPixel(true);
                    boardGrid.setPadding(Insets.EMPTY);

                    idToPlayerPawn.clear();

                    if (gameContainer.getParent() instanceof Region parent) {
                        var squareSize =
                                Bindings.min(parent.widthProperty(), parent.heightProperty())
                                        .subtract(20); // padding

                        gameContainer.minWidthProperty().bind(squareSize);
                        gameContainer.minHeightProperty().bind(squareSize);
                        gameContainer.maxWidthProperty().bind(squareSize);
                        gameContainer.maxHeightProperty().bind(squareSize);
                    }

                    for (int i = 0; i < rows; i++) {
                        ColumnConstraints cc = new ColumnConstraints();
                        cc.setPercentWidth(100.0 / cols);
                        cc.setHgrow(Priority.ALWAYS);
                        boardGrid.getColumnConstraints().add(cc);

                        RowConstraints rc = new RowConstraints();
                        rc.setPercentHeight(100.0 / rows);
                        rc.setVgrow(Priority.ALWAYS);
                        boardGrid.getRowConstraints().add(rc);
                    }

                    for (int row = 0; row < rows; row++) {
                        for (int col = 0; col < cols; col++) {

                            StackPane cellContainer = new StackPane();

                            cellContainer.setMinSize(0, 0);
                            cellContainer.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
                            // Tell GridPane this child may grow to fill its cell:
                            GridPane.setHgrow(cellContainer, Priority.ALWAYS);
                            GridPane.setVgrow(cellContainer, Priority.ALWAYS);

                            TileItem[][] tiles = gameBoardItem.getTiles();

                            TileItem tile = tiles[row][col];
                            ImageView imageView = new ImageView(tile.getTileImage().get());
                            imageView.setRotate(tile.getRotation().get().doubleValue());

                            imageView.fitWidthProperty().bind(cellContainer.widthProperty());
                            imageView.fitHeightProperty().bind(cellContainer.heightProperty());
                            imageView.setPreserveRatio(false);

                            cellContainer.getChildren().add(imageView);

                            // todo use decicated treasure image
                            //    ImageView treasureImage = new ImageView(new Image());
                            if (tile.getTreasure().get() != null) {
                                Label treasureNode =
                                        tempLabelImplforTreasure(tile.getTreasure().get());
                                cellContainer.getChildren().add(treasureNode);
                            }

                            if (tile.getBonus().get() != null) {
                                Label bonusNode = tempLabelImplforBonus(tile.getBonus().get());
                                cellContainer.getChildren().add(bonusNode);
                            }

                            int finalRow = row;
                            int finalCol = col;
                            Optional<PlayerStateItem> playerStateItemOptional =
                                    gameViewModel.getPlayerStatesSorted().stream()
                                            .filter(
                                                    player ->
                                                            player.getPlayerCurrentRowAsInt()
                                                                            == finalRow
                                                                    && player
                                                                                    .getPlayerCurrentColumnAsInt()
                                                                            == finalCol)
                                            .findFirst();
                            if (playerStateItemOptional.isPresent()) {
                                PlayerColor playerColor =
                                        playerStateItemOptional.get().getPlayerColor();
                                Circle playerPawn =
                                        new Circle(
                                                10,
                                                PlayerPawnUtils.mapPlayerColorToFxColor(
                                                        playerColor));
                                playerPawn.setOpacity(0.7);
                                String playerId =
                                        playerStateItemOptional.get().getPlayerIdAsString();
                                idToPlayerPawn.put(playerId, playerPawn);
                                cellContainer.getChildren().add(playerPawn);
                            }
                            cellContainer.setOnMouseClicked(
                                    event -> {
                                        gameViewModel.movePawnCommand(finalRow, finalCol);
                                    });

                            BooleanBinding isMyTurnAndMovePhase =
                                    Bindings.createBooleanBinding(
                                            () -> {
                                                return gameViewModel
                                                                .getCurrentPlayerIdAsString()
                                                                .equals(
                                                                        gameViewModel
                                                                                .getClientPlayer()
                                                                                .getId())
                                                        && (gameViewModel.getCurrentTurnState()
                                                                        != null
                                                                && gameViewModel
                                                                        .getCurrentTurnState()
                                                                        .equals(
                                                                                TurnState
                                                                                        .WAITING_FOR_MOVE));
                                            },
                                            gameViewModel.getCurrentPlayerIdProperty());
                            boardGrid.add(cellContainer, col, row);
                            cellContainer
                                    .mouseTransparentProperty()
                                    .bind(isMyTurnAndMovePhase.not());
                            cellContainer
                                    .cursorProperty()
                                    .bind(
                                            Bindings.when(isMyTurnAndMovePhase)
                                                    .then(Cursor.HAND)
                                                    .otherwise(Cursor.DEFAULT));
                        }
                    }
                };
        JavaFXUtils.runOnFxThread(apply);
    }

    private void initPushTileButtons() {
        int size = gameViewModel.getGameBoardItem().getCols(); // square, odd

        topArrowBox.getChildren().clear();
        bottomArrowBox.getChildren().clear();
        leftArrowBox.getChildren().clear();
        rightArrowBox.getChildren().clear();

        topArrowBox.setSpacing(0);
        bottomArrowBox.setSpacing(0);
        leftArrowBox.setSpacing(0);
        rightArrowBox.setSpacing(0);

        buildHorizontalBar(topArrowBox, Direction.DOWN, size);
        buildHorizontalBar(bottomArrowBox, Direction.UP, size);
        buildVerticalBar(leftArrowBox, Direction.RIGHT, size);
        buildVerticalBar(rightArrowBox, Direction.LEFT, size);
    }

    private void buildHorizontalBar(HBox bar, Direction dir, int size) {
        // Leading spacer absorbs extra width on the left
        Region lead = new Region();
        lead.setMinWidth(0);
        lead.setPrefWidth(leftArrowBox.getPrefHeight());
        HBox.setHgrow(lead, Priority.ALWAYS);
        bar.getChildren().add(lead);

        // One child per column: button at odd indices, spacer at even
        for (int col = 0; col < size; col++) {
            boolean isValidPushIndex = (col % 2 == 1) && (col <= size - 2);
            Region item;
            if (isValidPushIndex) {
                Button b = createArrowButton(dir == Direction.DOWN ? "↓" : "↑", col, dir);
                // Do NOT HGrow the slot items; keep exact width = one column
                b.setMaxWidth(Region.USE_PREF_SIZE);
                item = b;
            } else {
                Region spacer = new Region();
                spacer.setMaxWidth(Region.USE_PREF_SIZE);
                item = spacer;
            }
            bindSlotWidth(item, size); // width = boardGrid.width / size
            bar.getChildren().add(item);
        }

        // Trailing spacer absorbs extra width on the right
        Region trail = new Region();
        trail.setMinWidth(0);
        trail.setPrefWidth(leftArrowBox.getPrefHeight());
        HBox.setHgrow(trail, Priority.ALWAYS);
        bar.getChildren().add(trail);
    }

    private void buildVerticalBar(VBox bar, Direction dir, int size) {

        // One child per row: button at odd indices, spacer at even
        for (int row = 0; row < size; row++) {
            boolean isValidPushIndex = (row % 2 == 1) && (row <= size - 2);
            Region item;
            if (isValidPushIndex) {
                Button b = createArrowButton(dir == Direction.RIGHT ? "→" : "←", row, dir);
                item = b;
            } else {
                Region spacer = new Region();
                item = spacer;
            }
            bindSlotHeight(item, size); // height = boardGrid.height / size
            bar.getChildren().add(item);
        }
    }

    private void bindSlotWidth(Region item, int size) {
        item.prefWidthProperty().bind(boardGrid.widthProperty().divide(size));
    }

    private void bindSlotHeight(Region item, int size) {
        item.prefHeightProperty().bind(boardGrid.heightProperty().divide(size));
    }

    private Button createArrowButton(String text, int index, Direction dir) {
        Button b = new Button(text);
        b.getStyleClass().add("arrow-button");
        if (Direction.DOWN.equals(dir) || Direction.UP.equals(dir)) {
            b.getStyleClass().add("arrow-button-horizontal");
        } else {
            b.getStyleClass().add("arrow-button-vertical");
        }
        b.setOnAction(
                _ ->
                        pushTileCommand(
                                index,
                                dir,
                                gameViewModel
                                        .getSpareTileItem()
                                        .getDirections()
                                        .toArray(Direction[]::new)));
        b.disableProperty().bind(isNotMyTurnAndPushPhase());
        return b;
    }

    private BooleanBinding isNotMyTurnAndPushPhase() {
        return Bindings.createBooleanBinding(
                () -> {
                    return !(gameViewModel
                                    .getCurrentPlayerIdAsString()
                                    .equals(gameViewModel.getClientPlayer().getId())
                            && TurnState.WAITING_FOR_PUSH.equals(
                                    gameViewModel.getCurrentTurnState()));
                },
                gameViewModel.getCurrentPlayerIdProperty(),
                gameViewModel.getCurrentTurnStateProperty());
    }

    private void pushTileCommand(
            int index, Direction pushDirection, Direction[] extraTileEntrances) {
        if (!checkIfPushRevertsLastPush(index, pushDirection)) {
            // Send command to server - animation will be triggered by server response
            gameViewModel.pushTileCommand(index, pushDirection, extraTileEntrances);
        } else {
            // todo rework error display
            showAlertModal(
                    "Invalid push",
                    "Invalid push",
                    "You cannot push the tile in the same direction as the last push!");
        }
    }

    private boolean checkIfPushRevertsLastPush(int index, Direction pushDirection) {
        GameBoardItem gameBoardItem = gameViewModel.getGameBoardItem();
        ObjectProperty<PushActionInfo> lastPush = gameBoardItem.lastPushProperty();
        if (lastPush.isNull().get()) return false;
        if (!lastPush.get().getRowOrColIndex().equals(index)) return false;

        Direction lastPushDirection = lastPush.get().getDirection();
        return isOppositeDirection(pushDirection, lastPushDirection);
    }

    private boolean isOppositeDirection(Direction pushDirection, Direction lastPushDirection) {
        return switch (pushDirection) {
            case UP -> lastPushDirection == Direction.DOWN;
            case DOWN -> lastPushDirection == Direction.UP;
            case LEFT -> lastPushDirection == Direction.RIGHT;
            case RIGHT -> lastPushDirection == Direction.LEFT;
        };
    }

    // todo rework
    private void initNextTreasure() {
        nextTreasure
                .textProperty()
                .bind(
                        Bindings.createStringBinding(
                                gameViewModel::getNextTreasureNameAsString,
                                gameViewModel.getNextTreasure()));
    }

    private Label tempLabelImplforTreasure(TreasureItem treasureItem) {
        String treasureId = String.valueOf(treasureItem.getId());
        Label treasureNode = new Label(treasureId);
        // Style it to look clear on top of the tile
        treasureNode.setStyle(
                "-fx-font-size: 24px; "
                        + "-fx-font-weight: bold; "
                        + "-fx-text-fill: white; "
                        + "-fx-effect: dropshadow(three-pass-box, black, 10, 0, 0, 0);");
        return treasureNode;
    }

    private Label tempLabelImplforBonus(BonusType bonusType) {
        String bonusName = bonusType.name().toLowerCase();
        Label bonusNode = new Label(bonusName);
        bonusNode.setTranslateY(-20);
        bonusNode.setStyle(
                "-fx-font-size: 10px; "
                        + "-fx-text-fill: black; "
                        + "-fx-effect: dropshadow(three-pass-box, white, 10, 0, 0, 0);");
        return bonusNode;
    }

    private void initGameCounter() {
        // todo how is the set timer send from server?
    }

    private void initBoni() {

        // bonusBox.setVisible(true);
        bonusBox.setManaged(true);

        bonusSwapBox.setVisible(false);
        bonusSwapBox.setManaged(false);
        bindRowWidth(bonusSwapBox);

        bonusBeamBox.setVisible(false);
        bonusBeamBox.setManaged(false);
        bindRowWidth(bonusBeamBox);

        bonusPushTwiceBox.setVisible(false);
        bonusPushTwiceBox.setManaged(false);
        bindRowWidth(bonusPushTwiceBox);

        bonusPushFixedBox.setVisible(false);
        bonusPushFixedBox.setManaged(false);
        bindRowWidth(bonusPushFixedBox);

        BooleanBinding anyVisible =
                bonusSwapBox
                        .visibleProperty()
                        .or(bonusBeamBox.visibleProperty())
                        .or(bonusPushTwiceBox.visibleProperty())
                        .or(bonusPushFixedBox.visibleProperty());

        bonusBox.visibleProperty().bind(anyVisible);
        bonusBox.managedProperty().bind(bonusBox.visibleProperty());

        bonusSwapBox
                .visibleProperty()
                .bind(
                        Bindings.createBooleanBinding(
                                () -> getAvailableBonusCountForTypeAndClient(BonusType.SWAP) > 0,
                                gameViewModel.getClientPlayerState().getAvailableBonusTypes()));
        bonusSwapBox
                .managedProperty()
                .bind(
                        Bindings.createBooleanBinding(
                                () -> getAvailableBonusCountForTypeAndClient(BonusType.SWAP) > 0,
                                gameViewModel.getClientPlayerState().getAvailableBonusTypes()));
        bonusSwapCount
                .textProperty()
                .bind(
                        Bindings.createStringBinding(
                                () -> {
                                    return String.valueOf(
                                            getAvailableBonusCountForTypeAndClient(BonusType.SWAP));
                                },
                                gameViewModel.getClientPlayerState().getAvailableBonusTypes()));
        // todo impl bonus usage
        bonusSwapButton.setDisable(true);

        bonusBeamBox
                .visibleProperty()
                .bind(
                        Bindings.createBooleanBinding(
                                () -> getAvailableBonusCountForTypeAndClient(BonusType.BEAM) > 0,
                                gameViewModel.getClientPlayerState().getAvailableBonusTypes()));
        bonusBeamBox
                .managedProperty()
                .bind(
                        Bindings.createBooleanBinding(
                                () -> getAvailableBonusCountForTypeAndClient(BonusType.BEAM) > 0,
                                gameViewModel.getClientPlayerState().getAvailableBonusTypes()));
        bonusBeamCount
                .textProperty()
                .bind(
                        Bindings.createStringBinding(
                                () -> {
                                    return String.valueOf(
                                            getAvailableBonusCountForTypeAndClient(BonusType.BEAM));
                                },
                                gameViewModel.getClientPlayerState().getAvailableBonusTypes()));
        // todo impl bonus usage
        bonusBeamButton.setDisable(true);

        bonusPushTwiceBox
                .visibleProperty()
                .bind(
                        Bindings.createBooleanBinding(
                                () ->
                                        getAvailableBonusCountForTypeAndClient(BonusType.PUSH_TWICE)
                                                > 0,
                                gameViewModel.getClientPlayerState().getAvailableBonusTypes()));
        bonusPushTwiceBox
                .managedProperty()
                .bind(
                        Bindings.createBooleanBinding(
                                () ->
                                        getAvailableBonusCountForTypeAndClient(BonusType.PUSH_TWICE)
                                                > 0,
                                gameViewModel.getClientPlayerState().getAvailableBonusTypes()));
        bonusPushTwiceCount
                .textProperty()
                .bind(
                        Bindings.createStringBinding(
                                () -> {
                                    return String.valueOf(
                                            getAvailableBonusCountForTypeAndClient(
                                                    BonusType.PUSH_TWICE));
                                },
                                gameViewModel.getClientPlayerState().getAvailableBonusTypes()));
        // todo impl bonus usage
        bonusPushTwiceButton.setDisable(true);

        bonusPushFixedBox
                .visibleProperty()
                .bind(
                        Bindings.createBooleanBinding(
                                () ->
                                        getAvailableBonusCountForTypeAndClient(BonusType.PUSH_FIXED)
                                                > 0,
                                gameViewModel.getClientPlayerState().getAvailableBonusTypes()));
        bonusPushFixedBox
                .managedProperty()
                .bind(
                        Bindings.createBooleanBinding(
                                () ->
                                        getAvailableBonusCountForTypeAndClient(BonusType.PUSH_FIXED)
                                                > 0,
                                gameViewModel.getClientPlayerState().getAvailableBonusTypes()));
        bonusPushFixedCount
                .textProperty()
                .bind(
                        Bindings.createStringBinding(
                                () -> {
                                    return String.valueOf(
                                            getAvailableBonusCountForTypeAndClient(
                                                    BonusType.PUSH_FIXED));
                                },
                                gameViewModel.getClientPlayerState().getAvailableBonusTypes()));
        // todo impl bonus usage
        bonusPushFixedButton.setDisable(true);
    }

    private int getAvailableBonusCountForTypeAndClient(BonusType type) {
        return gameViewModel.getClientPlayerState().getAvailableBonusCountForType(type);
    }

    private void bindRowWidth(Region row) {
        row.setMaxWidth(Double.MAX_VALUE);
        row.prefWidthProperty()
                .bind(
                        Bindings.createDoubleBinding(
                                () ->
                                        bonusBox.getWidth()
                                                - bonusBox.getInsets().getLeft()
                                                - bonusBox.getInsets().getRight(),
                                bonusBox.widthProperty(),
                                bonusBox.insetsProperty()));
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

    private HBox getPlayerBoxByIndex(int index) {
        return switch (index) {
            case 0 -> player1Box;
            case 1 -> player2Box;
            case 2 -> player3Box;
            case 3 -> player4Box;
            default -> throw new IllegalArgumentException("Invalid index");
        };
    }

    public void onBonusSwapButtonClick(ActionEvent actionEvent) {
        // todo
    }

    public void onBonusBeamButtonClick(ActionEvent actionEvent) {
        // todo
    }

    public void onBonusPushTwiceButtonClick(ActionEvent actionEvent) {
        // todo
    }

    public void onBonusPushFixedButtonClick(ActionEvent actionEvent) {
        // todo
    }

    private void initTilePushListener() {
        gameViewModel
                .getGameBoardItem()
                .lastPushProperty()
                .addListener(
                        (obs, oldPush, newPush) -> {
                            if (newPush != null && !Objects.equals(oldPush, newPush)) {
                                int index = newPush.getRowOrColIndex();
                                Direction direction = newPush.getDirection();

                                log.debug(
                                        "Triggering tile push animation for all clients: index={},"
                                                + " direction={}",
                                        index,
                                        direction);

                                animationService.animateTilePush(index, direction, boardGrid);
                            }
                        });
    }

    private void initPlayerMovementListener() {
        gameViewModel
                .getPlayerStatesSorted()
                .forEach(
                        playerStateItem ->
                                playerStateItem
                                        .getCurrentPosition()
                                        .addListener(
                                                (obs, oldPos, newPos) -> {
                                                    if (gameViewModel
                                                            .getCurrentTurnState()
                                                            .equals(TurnState.WAITING_FOR_MOVE)) {
                                                        animationService.animatePlayerPath(
                                                                gameViewModel.getGameBoardItem(),
                                                                idToPlayerPawn.get(
                                                                        playerStateItem
                                                                                .getPlayerIdAsString()),
                                                                oldPos,
                                                                newPos,
                                                                boardGrid);
                                                    }
                                                }));
    }
}
