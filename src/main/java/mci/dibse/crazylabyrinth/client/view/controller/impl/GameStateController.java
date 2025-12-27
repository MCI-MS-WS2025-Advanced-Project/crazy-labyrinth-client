package mci.dibse.crazylabyrinth.client.view.controller.impl;

import jakarta.annotation.Nullable;
import java.time.OffsetDateTime;
import java.util.*;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;
import javafx.util.Duration;
import labyrinth.contracts.models.*;
import lombok.extern.slf4j.Slf4j;
import mci.dibse.crazylabyrinth.client.domain.context.PushTileCommandRecord;
import mci.dibse.crazylabyrinth.client.infrastructure.util.*;
import mci.dibse.crazylabyrinth.client.service.GameServerSocketService;
import mci.dibse.crazylabyrinth.client.infrastructure.util.JavaFXUtils;
import mci.dibse.crazylabyrinth.client.infrastructure.util.PlayerPawnUtils;
import mci.dibse.crazylabyrinth.client.service.AnimationService;
import mci.dibse.crazylabyrinth.client.view.viewmodel.GameViewModel;
import mci.dibse.crazylabyrinth.client.view.viewmodel.item.*;
import mci.dibse.crazylabyrinth.client.view.viewmodel.navigation.NavigationStateViewModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class GameStateController extends BaseViewCleanup {

    @FXML public BorderPane gameContainer;
    @FXML public GridPane boardGrid;
    @FXML public HBox topArrowBox;
    @FXML public VBox rightArrowBox;
    @FXML public VBox leftArrowBox;
    @FXML public HBox bottomArrowBox;

    @FXML public ImageView nextTreasureImage;

    @FXML public StackPane extraTileContainer;
    @FXML public Label extraTileTreasureLabel;
    @FXML public Label extraTileBonusLabel;
    @FXML public VBox extraTileWrapper;
    @FXML public ImageView extraTile;
    @FXML public ImageView extraTileTreasure;
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
    @FXML public VBox player1Box;
    @FXML public Label player1Status;
    @FXML public Label player2;
    @FXML public Circle player2Circle;
    @FXML public VBox player2Box;
    @FXML public Label player2Status;
    @FXML public Label player3;
    @FXML public Circle player3Circle;
    @FXML public VBox player3Box;
    @FXML public Label player3Status;
    @FXML public Label player4;
    @FXML public Circle player4Circle;
    @FXML public VBox player4Box;
    @FXML public Label player4Status;

    @FXML public Label gameCounterLabel;
    @FXML public StackPane centerWrapper;
    @FXML public StackPane centerStackPane;
    @FXML public VBox leftSidebar;
    @FXML public VBox rightSidebar;
    @FXML public VBox playerLabelsWrapper;

    @FXML public HBox turnCounterBox;
    @FXML public Label turnCounterLabel;

    @FXML public BorderPane root;
    @FXML public Rectangle rootClip;
    @FXML public StackPane viewport;
    @FXML public Group contentGroup;
    @FXML public GridPane mainLayoutGrid;


    private static final int GAP_BETWEEN_TILES = 2;
    private static final int MAX_PLAYERS = 4;
    private static final String TIMEOUT_LABEL = "Timeout aufgrund von Untätigkeit";
    private static final String GENERAL_ERROR_TITLE = "Achtung Fehler!";
    private static final PseudoClass ACTIVE_BONUS_BUTTON_PSEUDO_CLASS =
            PseudoClass.getPseudoClass("bonus-active");
    private static final PseudoClass AVAILABLE_BONUS_BUTTON_PSEUDO_CLASS =
            PseudoClass.getPseudoClass("bonus-available");


    private static final String QUIT_GAME_MODAL_TITLE   = "Beenden";
    private static final String QUIT_GAME_MODAL_HEADER  = "Spiel wirklich beenden?";
    private static final String QUIT_GAME_MODAL_CONTENT = "Das laufende Spiel wird beendet und Sie kehren zur Serverübersicht zurück";


    private final GameViewModel gameViewModel;
    private final Map<String, Label> idToPlayerLabel = new HashMap<>();
    private final Map<String, Label> idToPlayerStatusLabel = new HashMap<>();


    private Map<BonusType, ChangeListener<Boolean>> bonusAvailableListeners = new HashMap<>();

    private IntegerProperty remainingGameTimer = new SimpleIntegerProperty();
    private Timeline gameTimeline;

    private IntegerProperty remainingTurnTimer = new SimpleIntegerProperty();
    private Timeline turnTimeline;
    private ChangeListener<OffsetDateTime> turnTimeChangeListener;

    private ScreenResolutionProperties screenResolutionProperties;
    private final Map<String, ImageView> idToPlayerPawn = new HashMap<>();

    private final AnimationService animationService;

    @Autowired
    public GameStateController(NavigationStateViewModel vm, GameViewModel gameViewModel, ScreenResolutionProperties screenResolutionProperties) {
        super(vm);
        this.gameViewModel = gameViewModel;
        this.screenResolutionProperties = screenResolutionProperties;
        this.animationService = new AnimationService();
    }


    @FXML
    public void initialize() {

        initPlayerTurnLabel();

        initPlayerLabels();

        initSpareTile();

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

        initBoni();

        setupErrorHandling();

        initGameTimer();


        // delay init of scaling & gamegrid until several pulse runs
        viewport.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {

                Platform.runLater(() -> {
                    root.applyCss();
                    root.layout();
                    Platform.runLater(() -> {
                        initUiScaling();
                        initGameGrid();
                    });
                });
            }
        });
    }

    private void initUiScaling() {
            ScreenResolutionProperties.ResolutionProperties props = new ScreenResolutionProperties.ResolutionProperties(root,
                    viewport);
            screenResolutionProperties.initUiScaling(props);
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
        extraTile.imageProperty().unbind();
        extraTile.rotateProperty().unbind();
        extraTile.fitWidthProperty().unbind();
        rotateRightButton.visibleProperty().unbind();

        nextTreasureImage.imageProperty().unbind();
        extraTileTreasure.fitWidthProperty().unbind();
        extraTileTreasure.imageProperty().unbind();
        extraTile.fitHeightProperty().unbind();
        bonusBox.visibleProperty().unbind();
        bonusBox.managedProperty().unbind();

        bonusSwapBox.visibleProperty().unbind();
        bonusSwapBox.managedProperty().unbind();
        bonusSwapCount.textProperty().unbind();
        bonusSwapButton.disableProperty().unbind();
        bonusSwapButton
                .disableProperty()
                .removeListener(bonusAvailableListeners.get(BonusType.SWAP));

        bonusBeamBox.visibleProperty().unbind();
        bonusBeamBox.managedProperty().unbind();
        bonusBeamCount.textProperty().unbind();
        bonusBeamButton.disableProperty().unbind();
        bonusBeamButton
                .disableProperty()
                .removeListener(bonusAvailableListeners.get(BonusType.BEAM));

        bonusPushTwiceBox.visibleProperty().unbind();
        bonusPushTwiceBox.managedProperty().unbind();
        bonusPushTwiceCount.textProperty().unbind();
        bonusPushTwiceButton.disableProperty().unbind();
        bonusPushTwiceButton
                .disableProperty()
                .removeListener(bonusAvailableListeners.get(BonusType.PUSH_TWICE));

        bonusPushFixedBox.visibleProperty().unbind();
        bonusPushFixedBox.managedProperty().unbind();
        bonusPushFixedCount.textProperty().unbind();
        bonusPushFixedButton.disableProperty().unbind();
        bonusPushFixedButton
                .disableProperty()
                .removeListener(bonusAvailableListeners.get(BonusType.PUSH_FIXED));

        gameCounterLabel.textProperty().unbind();
    }

    @EventListener
    public void handleSendCommandEvent(GameServerSocketService.SendCommand event) {
        switch (event.commandType()) {
            case PUSH_TILE, USE_PUSH_FIXED -> toggleOrSetBonusButton(BONUS_TYPE.PUSH_FIXED, false);
            case USE_BEAM -> toggleOrSetBonusButton(BONUS_TYPE.BEAM, false);
            case USE_SWAP -> toggleOrSetBonusButton(BONUS_TYPE.SWAP, false);
            case USE_PUSH_TWICE -> toggleOrSetBonusButton(BONUS_TYPE.PUSH_TWICE, false);
        }
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

    private Label getPlayerStatusLabelByIndex(int index) {
        return switch (index) {
            case 0 -> player1Status;
            case 1 -> player2Status;
            case 2 -> player3Status;
            case 3 -> player4Status;
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

                                    if (gameViewModel.isClientPlayerTimeout()) return TIMEOUT_LABEL;

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
                                gameViewModel.getCurrentTurnStateProperty(),
                                gameViewModel.getTurnTimeoutProperty()));
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

        initTurnCounter();
    }

    private void initTurnCounter() {

        initTurnCounterTimeline();
        turnCounterLabel
                .textProperty()
                .bind(
                        Bindings.createStringBinding(
                                () -> {
                                    int totalSeconds = remainingTurnTimer.get();
                                    long minutes = totalSeconds / 60;
                                    long seconds = totalSeconds % 60;
                                    return String.format("%02d:%02d", minutes, seconds);
                                },
                                remainingTurnTimer));
        ChangeListener<OffsetDateTime> turnTimeChangeListener =
                (obs, oldValue, newValue) -> {
                    turnTimeline.stop();
                    Long turnTimeInSeconds = gameViewModel.getRemainingTurnTimeInSeconds();
                    log.info("Turn time in seconds: {}", turnTimeInSeconds);
                    remainingTurnTimer.set(Math.toIntExact(turnTimeInSeconds));
                    turnTimeline.playFromStart();
                };
        gameViewModel.addListenerToCurrentTurnEndTime(turnTimeChangeListener);
        if (gameViewModel.isCurrentTurnTimeSet()) turnTimeChangeListener.changed(null, null, null);
    }

    private void initTurnCounterTimeline() {
        turnTimeline =
                new Timeline(
                        new KeyFrame(
                                Duration.seconds(1),
                                e -> {
                                    remainingTurnTimer.set(remainingTurnTimer.get() - 1);
                                    if (remainingTurnTimer.get() <= 0
                                            || gameViewModel.isClientPlayerTimeout()) {
                                        turnTimeline.stop();
                                    }
                                }));
        turnTimeline.setCycleCount(Animation.INDEFINITE);
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

            VBox box = getPlayerBoxByIndex(i);
            box.setVisible(true);
            box.setManaged(true);

            PlayerStateItem playerStateItem = playerStates.get(i);
            getPlayerCircleByIndex(i)
                    .setFill(
                            PlayerPawnUtils.mapPlayerColorToFxColor(
                                    playerStateItem.getPlayerColor()));
            idToPlayerLabel.put(playerStateItem.getPlayerIdAsString(), getPlayerLabelByIndex(i));
            idToPlayerStatusLabel.put(playerStateItem.getPlayerIdAsString(), getPlayerStatusLabelByIndex(i));
        }

        // clientPlayer always first
        Label clientLabel = idToPlayerLabel.get(clientPlayerId);
        PlayerStateItem clientState = gameViewModel.getClientPlayerState();
        bindPlayerLabel(clientLabel, clientState, true);
        Label clientStatusLabel = idToPlayerStatusLabel.get(clientPlayerId);
        bindPlayerStatusLabel(clientStatusLabel, clientState);

        playerStates
                .subList(1, playerStates.size())
                .forEach(
                        playerState -> {
                            Label playerLabel =
                                    idToPlayerLabel.get(playerState.getPlayerIdAsString());
                            Label playerStatusLabel = idToPlayerStatusLabel.get(playerState.getPlayerIdAsString());
                            bindPlayerLabel(playerLabel, playerState, false);
                            bindPlayerStatusLabel(playerStatusLabel, playerState);
                        });
    }

    private void bindPlayerStatusLabel(Label statusLabel, PlayerStateItem playerState) {
        String maxAvailableTreasurePerPlayer =
                String.valueOf(playerState.getRemainingTreasureCountAsInt());

        statusLabel.textProperty().unbind();
        statusLabel.textProperty().bind(
                Bindings.createStringBinding(
                        () -> {
                            String playerStatusLabelString =
                                    "Schätze ("
                                            + playerState.getTreasuresFoundAsInt()
                                            + " / "
                                            + maxAvailableTreasurePerPlayer
                                            + ")";
                            if (playerState.isPlayerAiControlled()) playerStatusLabelString += " - (AI)";
                            return playerStatusLabelString;
                        },playerState.getPlayerInfo().get().isAiControlledProperty(),
                        playerState.getTreasuresFound()));

    }

    private void bindPlayerLabel(Label label, PlayerStateItem player, boolean isClient) {
        label.textProperty().unbind();
        label.textProperty()
                .bind(
                        Bindings.createStringBinding(
                                () -> {
                                    String prefix = isClient ? "Du: " : "";

                                    return prefix + player.getPlayerNameAsString();
                                },
                                player.getPlayerInfo()));
    }

    private void initSpareTile() {
        extraTileWrapper.visibleProperty().set(true);




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
                    if (newValue != null) {
                        bindSpareTileImage(newValue);
                        bindSpareTileTreasureImage(newValue);
                    }
                };

        gameViewModel.addListenerToSpareTileProperty(spareTileListener);
        if (gameViewModel.getSpareTileItem() != null) {
            TileItem currentTile = gameViewModel.getSpareTileItem();
            bindSpareTileImage(currentTile);
            bindSpareTileTreasureImage(currentTile);
        }

        rotateRightButton
                .visibleProperty()
                .bind(
                        Bindings.createBooleanBinding(
                                () -> {
                                    if (gameViewModel.isPendingRotation()
                                            || gameViewModel.isClientPlayerTimeout()) return false;
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
                                gameViewModel.getTurnTimeoutProperty(),
                                gameViewModel.getPendingRotationProperty()));

        rotateRightButton.setOnAction(
                _ -> {
                    //  gameViewModel.getSpareTileItem().onRightRotation();
                    gameViewModel.getPendingRotationProperty().set(true);
                    gameViewModel.onRightRotationOfSpareTile();
                });
    }

    private void bindSpareTileTreasureImage(TileItem currentTile) {
        extraTileTreasure.fitWidthProperty().unbind();
        extraTileTreasure.imageProperty().unbind();

        if (!currentTile.hasTreasure()) {
            extraTileTreasure.setVisible(false);
            extraTileTreasure.setManaged(false);
            return;
        }


        extraTileTreasure.setPreserveRatio(true);

        extraTileTreasure.imageProperty().bind(
                Bindings.createObjectBinding(
                        () -> currentTile.getTreasureItem().getTreasureImage(),
                        gameViewModel.getSpareTileProperty()));
        extraTileTreasure.getStyleClass().add("treasure-image");

        extraTileTreasure.setRotate(currentTile.getRotation().get().doubleValue());

        extraTileTreasure.fitWidthProperty().bind(extraTileWrapper.widthProperty().multiply(0.2));
        extraTileTreasure.setVisible(true);
        extraTileTreasure.setManaged(true);
    }

    private void bindSpareTileImage(TileItem currentTile) {

        extraTile.imageProperty().unbind();
        extraTile.rotateProperty().unbind();
        extraTile.fitWidthProperty().unbind();
        extraTile.setPreserveRatio(true);
        extraTile
                .imageProperty()
                .bind(
                        Bindings.createObjectBinding(
                                () -> currentTile.getTileImage().get(),
                                gameViewModel.getSpareTileProperty(),
                                currentTile.getRotation()));
        extraTile.rotateProperty().bindBidirectional(currentTile.getRotation());
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
                    GridPane.setMargin(boardGrid, Insets.EMPTY);

                    idToPlayerPawn.clear();

                    if (gameContainer.getParent() instanceof Region parent) {

                        var squareSize =
                                Bindings.min(parent.widthProperty(), parent.heightProperty());


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
                            imageView.setPreserveRatio(true);

                            cellContainer.getChildren().add(imageView);


                            if (tile.hasTreasure()) {

                                TreasureItem treasureItem = tile.getTreasureItem();
                                ImageView treasureImage = new ImageView(treasureItem.getTreasureImage());
                                treasureImage.setPreserveRatio(true);
                                double scale = 0.4;

                                treasureImage.fitWidthProperty().bind(cellContainer.widthProperty().multiply(scale));
                                treasureImage.fitHeightProperty().bind(cellContainer.heightProperty().multiply(scale));
                                treasureImage.setRotate(tile.getRotation().get().doubleValue());

                                cellContainer.getChildren().add(treasureImage);


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
                                PlayerStateItem playerStateItem = playerStateItemOptional.get();

                                PlayerColor playerColor =
                                        playerStateItem.getPlayerColor();


                                ImageView playerImage = new ImageView();
                                Image image = ImageResourceLoader.loadPlayerImage(playerColor);
                                playerImage.setPreserveRatio(true);
                                playerImage.setImage(image);
                                ImageViewStyler.applyPlayerStylingToImageView(playerImage, playerColor);

                                if(tile.hasTreasure()) offsetPlayerPawnOnTreasurePresence(tile, playerImage);
                                double scale = 0.45;
                                playerImage.fitWidthProperty().bind(cellContainer.widthProperty().multiply(scale));
                                playerImage.fitHeightProperty().bind(cellContainer.heightProperty().multiply(scale));
                                idToPlayerPawn.put(playerStateItem.getPlayerIdAsString(), playerImage);
                                cellContainer.getChildren().add(playerImage);

                            }

                            markPlayerHomeBase(row, col, cellContainer);

                            // set mouse clicks tuff for move Phase
                            cellContainer.setOnMouseClicked(
                                    event -> {
                                        sendMoveOrBonusCommand(
                                                finalRow, finalCol, playerStateItemOptional);
                                    });

                            boardGrid.add(cellContainer, col, row);
                            cellContainer
                                    .mouseTransparentProperty()
                                    .bind(shouldMouseOverTileBeDisabled(playerStateItemOptional));
                            cellContainer
                                    .cursorProperty()
                                    .bind(
                                            Bindings.when(
                                                            shouldMouseOverTileBeDisabled(
                                                                            playerStateItemOptional)
                                                                    .not())
                                                    .then(Cursor.HAND)
                                                    .otherwise(Cursor.DEFAULT));
                        }
                    }
                };
        Platform.runLater(apply);
        //JavaFXUtils.runOnFxThread(apply);
    }

    private void offsetPlayerPawnOnTreasurePresence(TileItem tile, ImageView playerImage) {
        List<Direction> directions = tile.getDirections();
        for( Direction direction : directions) {
            if (Direction.DOWN.equals(direction)) continue;
            switch (direction) {
                case UP -> playerImage.setTranslateY(-30);
                case LEFT -> playerImage.setTranslateX(-25);
                case RIGHT -> playerImage.setTranslateX(25);
            }
            break;
        }
    }

    private void markPlayerHomeBase(int row, int col, StackPane cellContainer) {
        gameViewModel.getPlayerStatesSorted().stream()
                .filter(
                        player ->
                                player.getHomePosition().get().getColumnAsInt() == col
                                        && player.getHomePosition().get().getRowAsInt() == row)
                .findFirst()
                .ifPresent(playerStateItem -> addStyleToPlayerHomeBase(cellContainer, playerStateItem));
    }

    private void addStyleToPlayerHomeBase(StackPane cellContainer, PlayerStateItem playerStateItem) {

        String styleColor = playerStateItem.getPlayerColor().toString().toLowerCase();


        Rectangle borderOverlay = new Rectangle();


        borderOverlay.widthProperty().bind(cellContainer.widthProperty());
        borderOverlay.heightProperty().bind(cellContainer.heightProperty());


        borderOverlay.setFill(Color.TRANSPARENT);

        Color baseColor = Color.web(styleColor); // web() handles standard color names like "red", "blue"
        // Set the border color and thickness
        Color softColor = new Color(
                baseColor.getRed(),
                baseColor.getGreen(),
                baseColor.getBlue(),
                0.5 // Reduce opacity to make it less intense
        );

        borderOverlay.setStroke(softColor);
        borderOverlay.setStrokeWidth(1.5);
        borderOverlay.setStrokeType(StrokeType.INSIDE);


        borderOverlay.setMouseTransparent(true);


        cellContainer.getChildren().add(borderOverlay);

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

        for (int col = 0; col < size; col++) {
            boolean isValidPushIndex = isValidatePushIndex(col, size);
            boolean isValidPushTwiceBonusIndex =
                    isBonusEnabledAndValidPushFixedBonusIndex(col, size);

            Region item;
            if (isValidPushIndex) {
                Button b = createArrowButton(dir == Direction.DOWN ? "↓" : "↑", col, dir);
                b.setMaxWidth(Region.USE_PREF_SIZE);
                item = b;
            } else if (isValidPushTwiceBonusIndex) {
                Button b =
                        createPushFixedBonusArrowButton(
                                dir == Direction.DOWN ? "↓" : "↑", col, dir);

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
            boolean isValidPushIndex = isValidatePushIndex(row, size);
            boolean isValidPushFixedBonusIndex =
                    isBonusEnabledAndValidPushFixedBonusIndex(row, size);
            Region item;
            if (isValidPushIndex) {
                Button b = createArrowButton(dir == Direction.RIGHT ? "→" : "←", row, dir);
                item = b;
            } else if (isValidPushFixedBonusIndex) {
                Button b =
                        createPushFixedBonusArrowButton(
                                dir == Direction.RIGHT ? "→" : "←", row, dir);
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

    private Button createBaseArrowButton(String text, Direction dir) {
        Button b = new Button(text);
        b.getStyleClass().add("arrow-button");
        if (Direction.DOWN.equals(dir) || Direction.UP.equals(dir)) {
            b.getStyleClass().add("arrow-button-horizontal");
        } else {
            b.getStyleClass().add("arrow-button-vertical");
        }
        return b;
    }

    private Button createArrowButton(String text, int index, Direction dir) {
        Button b = createBaseArrowButton(text, dir);
        b.setOnAction(_ -> choosePushTileCommand(index, dir, CommandType.PUSH_TILE));
        b.disableProperty().bind(shouldPushTileButtonBeDisabled());
        return b;
    }

    private Button createPushFixedBonusArrowButton(String text, int index, Direction dir) {
        Button b = createBaseArrowButton(text, dir);
        b.getStyleClass().add("arrow-button-fixed");
        b.setOnAction(_ -> choosePushTileCommand(index, dir, CommandType.USE_PUSH_FIXED));

        b.disableProperty().bind(shouldPushFixedTileButtonBeDisabled());
        return b;
    }

    private boolean isValidatePushIndex(int index, int size) {
        return (index % 2 == 1) && (index <= size - 2);
    }

    private boolean isBonusEnabledAndValidPushFixedBonusIndex(int index, int size) {
        return gameViewModel.isBonusEnabled()
                && (index % 2 == 0)
                && (index <= size - 2)
                && (index > 0);
    }

    private BooleanBinding isNotMyTurnAndTimeout() {
        return gameViewModel
                .getCurrentPlayerIdProperty()
                .isNotEqualTo(gameViewModel.getClientPlayer().getId())
                .or(gameViewModel.getTurnTimeoutProperty());
    }

    private BooleanBinding isNotMyTurnAndTimeoutAndPushPhase() {
        return isNotMyTurnAndTimeout()
                .or(
                        gameViewModel
                                .getCurrentTurnStateProperty()
                                .isNotEqualTo(TurnState.WAITING_FOR_PUSH));
    }

    private BooleanBinding isNotMyTurnAndTimeoutAndMovePhase() {
        return isNotMyTurnAndTimeout()
                .or(
                        gameViewModel
                                .getCurrentTurnStateProperty()
                                .isNotEqualTo(TurnState.WAITING_FOR_MOVE));
    }

    private BooleanBinding shouldMouseOverTileBeDisabled(Optional<PlayerStateItem> playerOnTile) {
        BooleanBinding isMyTurn = isNotMyTurnAndTimeout().not();
        BooleanBinding isMovePhase =
                gameViewModel.getCurrentTurnStateProperty().isEqualTo(TurnState.WAITING_FOR_MOVE);

        BooleanBinding isSwapAllowed =
                gameViewModel
                        .getIsSwapBonusActiveProperty()
                        .and(
                                Bindings.createBooleanBinding(
                                        () ->
                                                playerOnTile.isPresent()
                                                        && !playerOnTile
                                                                .get()
                                                                .getPlayerIdAsString()
                                                                .equals(
                                                                        gameViewModel
                                                                                .getClientPlayer()
                                                                                .getId()),
                                        gameViewModel.getIsSwapBonusActiveProperty()));
        BooleanBinding isPushPhaseAndBonusActive =
                gameViewModel
                        .getCurrentTurnStateProperty()
                        .isEqualTo(TurnState.WAITING_FOR_PUSH)
                        .and(gameViewModel.getIsBeamTileBonusActiveProperty().or(isSwapAllowed));

        BooleanBinding isTileEnabled = isMyTurn.and(isMovePhase.or(isPushPhaseAndBonusActive));

        return isTileEnabled.not();
    }

    private BooleanBinding shouldPushTileButtonBeDisabled() {
        return isNotMyTurnAndTimeoutAndPushPhase()
                .or(gameViewModel.getIsPushFixedTileBonusActiveProperty())
                .or(gameViewModel.getIsBeamTileBonusActiveProperty())
                .or(gameViewModel.getIsSwapBonusActiveProperty());
    }

    private BooleanBinding shouldPushFixedTileButtonBeDisabled() {
        return isNotMyTurnAndTimeoutAndPushPhase()
                .or(gameViewModel.getIsPushFixedTileBonusActiveProperty().not());
    }

    private BooleanBinding shouldBonusButtonBeDisabled(BonusType bonusType) {
        return isNotMyTurnAndTimeoutAndPushPhase()
                .or(
                        Bindings.createBooleanBinding(
                                () -> gameViewModel.isAnyBonusActive(bonusType),
                                gameViewModel.getIsPushFixedTileBonusActiveProperty(),
                                gameViewModel.getIsSwapBonusActiveProperty(),
                                gameViewModel.getIsBeamTileBonusActiveProperty()));
    }

    private void choosePushTileCommand(int index, Direction dir, CommandType type) {
        Direction[] extraTileEntrances =
                gameViewModel.getSpareTileItem().getDirections().toArray(Direction[]::new);
        pushTileCommand(new PushTileCommandRecord(index, dir, extraTileEntrances, type));
    }

    private void pushTileCommand(PushTileCommandRecord data) {
        int index = data.index();
        Direction pushDirection = data.pushDirection();

        if (!checkIfPushRevertsLastPush(index, pushDirection)) {
            // Send command to server - animation will be triggered by server response
            gameViewModel.pushTileCommand(data);
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

    private void sendMoveOrBonusCommand(
            int finalRow, int finalCol, Optional<PlayerStateItem> playerStateItemOptional) {
        if (gameViewModel.isBeamTileBonusActive()) {
            gameViewModel.sendUseBeamCommand(finalRow, finalCol);
        } else if (gameViewModel.isSwapBonusActive() && playerStateItemOptional.isPresent()) {
            String playerId = playerStateItemOptional.get().getPlayerIdAsString();
            gameViewModel.sendUseSwapCommand(playerId);
        } else {
            gameViewModel.movePawnCommand(finalRow, finalCol);
        }
    }


    private void initNextTreasure() {
        nextTreasureImage.imageProperty().bind(Bindings.createObjectBinding(
                () -> gameViewModel.getNextTreasureImageProperty().get(),
                        gameViewModel.getNextTreasureItemProperty()
                ));


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

        bonusSwapButton.disableProperty().bind(shouldBonusButtonBeDisabled(BonusType.SWAP));

        ChangeListener<Boolean> bonusSwapButtonAvailableListener =
                (ov, oldVal, newVal) -> {
                    bonusSwapButton.pseudoClassStateChanged(
                            AVAILABLE_BONUS_BUTTON_PSEUDO_CLASS,
                            shouldBonusButtonBeDisabled(BonusType.SWAP).not().get());
                };
        bonusSwapButton.disableProperty().addListener(bonusSwapButtonAvailableListener);
        bonusAvailableListeners.put(BonusType.SWAP, bonusSwapButtonAvailableListener);

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

        bonusBeamButton.disableProperty().bind(shouldBonusButtonBeDisabled(BonusType.BEAM));

        ChangeListener<Boolean> bonusBeamButtonAvailableListener =
                (ov, oldVal, newVal) -> {
                    bonusBeamButton.pseudoClassStateChanged(
                            AVAILABLE_BONUS_BUTTON_PSEUDO_CLASS,
                            shouldBonusButtonBeDisabled(BonusType.BEAM).not().get());
                };
        bonusBeamButton.disableProperty().addListener(bonusBeamButtonAvailableListener);
        bonusAvailableListeners.put(BonusType.BEAM, bonusBeamButtonAvailableListener);

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

        bonusPushTwiceButton.disableProperty().bind(isNotMyTurnAndTimeoutAndMovePhase());

        ChangeListener<Boolean> bonusPushTwiceButtonAvailableListener =
                (ov, oldVal, newVal) -> {
                    bonusPushTwiceButton.pseudoClassStateChanged(
                            AVAILABLE_BONUS_BUTTON_PSEUDO_CLASS,
                            isNotMyTurnAndTimeoutAndMovePhase().not().get());
                };
        bonusPushTwiceButton.disableProperty().addListener(bonusPushTwiceButtonAvailableListener);
        bonusAvailableListeners.put(BonusType.PUSH_TWICE, bonusPushTwiceButtonAvailableListener);

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

        bonusPushFixedButton
                .disableProperty()
                .bind(shouldBonusButtonBeDisabled(BonusType.PUSH_FIXED));

        ChangeListener<Boolean> bonusPushFixedButtonAvailableListener =
                (ov, oldVal, newVal) -> {
                    bonusPushFixedButton.pseudoClassStateChanged(
                            AVAILABLE_BONUS_BUTTON_PSEUDO_CLASS,
                            shouldBonusButtonBeDisabled(BonusType.PUSH_FIXED).not().get());
                };
        bonusPushFixedButton.disableProperty().addListener(bonusPushFixedButtonAvailableListener);
        bonusAvailableListeners.put(BonusType.PUSH_FIXED, bonusPushFixedButtonAvailableListener);
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

    private VBox getPlayerBoxByIndex(int index) {
        return switch (index) {
            case 0 -> player1Box;
            case 1 -> player2Box;
            case 2 -> player3Box;
            case 3 -> player4Box;
            default -> throw new IllegalArgumentException("Invalid index");
        };
    }

    public void onBonusSwapButtonClick(ActionEvent actionEvent) {
        toggleOrSetBonusButton(BONUS_TYPE.SWAP, null);
    }

    public void onBonusBeamButtonClick(ActionEvent actionEvent) {
        toggleOrSetBonusButton(BONUS_TYPE.BEAM, null);
    }

    public void onBonusPushTwiceButtonClick(ActionEvent actionEvent) {
        toggleOrSetBonusButton(BONUS_TYPE.PUSH_TWICE, null);
        gameViewModel.sendUsePushTwiceCommand();
    }

    public void onBonusPushFixedButtonClick(ActionEvent actionEvent) {
        toggleOrSetBonusButton(BONUS_TYPE.PUSH_FIXED, null);
    }

    private void toggleOrSetBonusButton(BONUS_TYPE type, @Nullable Boolean set) {
        switch (type) {
            case SWAP -> {
                boolean flag = set != null ? set : !gameViewModel.isSwapBonusActive();
                gameViewModel.getIsSwapBonusActiveProperty().set(flag);
                bonusSwapButton.pseudoClassStateChanged(ACTIVE_BONUS_BUTTON_PSEUDO_CLASS, flag);
            }
            case BEAM -> {
                boolean flag = set != null ? set : !gameViewModel.isBeamTileBonusActive();
                gameViewModel.getIsBeamTileBonusActiveProperty().set(flag);
                bonusBeamButton.pseudoClassStateChanged(ACTIVE_BONUS_BUTTON_PSEUDO_CLASS, flag);
            }
            case PUSH_TWICE -> {
                boolean flag = set != null ? set : !gameViewModel.isPushTwiceTileBonusActive();
                gameViewModel.getIsPushTwiceTileBonusActiveProperty().set(flag);
                bonusPushTwiceButton.pseudoClassStateChanged(
                        ACTIVE_BONUS_BUTTON_PSEUDO_CLASS, flag);
            }
            case PUSH_FIXED -> {
                boolean flag = set != null ? set : !gameViewModel.isPushFixedTileBonusActive();
                gameViewModel.getIsPushFixedTileBonusActiveProperty().set(flag);
                bonusPushFixedButton.pseudoClassStateChanged(
                        ACTIVE_BONUS_BUTTON_PSEUDO_CLASS, flag);
            }
        }
    }

    private void setupErrorHandling() {
        gameViewModel
                .getHasError()
                .addListener(
                        (obs, oldVal, newVal) -> {
                            if (Boolean.TRUE.equals(newVal) && shouldErrorBeShownAsModal()) {
                                JavaFXUtils.showAlertErrorModal(
                                        GENERAL_ERROR_TITLE,
                                        "",
                                        gameViewModel.getErrorMessage().get(),
                                        gameViewModel::clearError);
                            }
                        });
    }

    private boolean shouldErrorBeShownAsModal() {
        if (gameViewModel.getErrorCodeProperty().get() == null) return false;

        return switch (gameViewModel.getErrorCodeProperty().get()) {
            case INVALID_MOVE -> false;
            default -> true;
        };
    }

    public void onLeaveGameButtonClick(ActionEvent actionEvent) {
        log.debug("onLeaveGameButtonClick");
        Runnable onConfirm = gameViewModel::leaveGame;
        JavaFXUtils.showConfirmationModal(
                QUIT_GAME_MODAL_TITLE, QUIT_GAME_MODAL_HEADER, QUIT_GAME_MODAL_CONTENT, onConfirm);
    }

    private enum BONUS_TYPE {
        SWAP,
        BEAM,
        PUSH_TWICE,
        PUSH_FIXED;
    }

    private void initGameTimer() {
        Long gameTimeSeconds = gameViewModel.getRemainingGameTimeInSeconds();
        remainingGameTimer.set(Math.toIntExact(gameTimeSeconds));

        initCountdownTimeline(gameTimeSeconds);

        gameCounterLabel
                .textProperty()
                .bind(
                        Bindings.createStringBinding(
                                () -> {
                                    int totalSeconds = remainingGameTimer.get();
                                    long minutes = totalSeconds / 60;
                                    long seconds = totalSeconds % 60;
                                    return String.format("%02d:%02d", minutes, seconds);
                                },
                                remainingGameTimer // dependency: re-calculate when this property
                                // changes
                                ));

        gameTimeline.playFromStart();
    }

    private void initCountdownTimeline(Long gameTimeSeconds) {
        gameTimeline =
                new Timeline(
                        new KeyFrame(
                                javafx.util.Duration.seconds(1),
                                e -> {
                                    remainingGameTimer.set(remainingGameTimer.get() - 1);
                                    if (remainingGameTimer.get() <= 0
                                            || gameViewModel.isClientPlayerTimeout()) {
                                        gameTimeline.stop();
                                    }
                                }));
        gameTimeline.setCycleCount(Animation.INDEFINITE); // we control stopping via remainingTimer
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
