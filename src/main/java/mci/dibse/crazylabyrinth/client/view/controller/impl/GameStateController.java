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
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
import mci.dibse.crazylabyrinth.client.NavigationController;
import mci.dibse.crazylabyrinth.client.domain.context.PushTileCommandRecord;
import mci.dibse.crazylabyrinth.client.domain.model.enums.Theme;
import mci.dibse.crazylabyrinth.client.infrastructure.util.*;
import mci.dibse.crazylabyrinth.client.infrastructure.util.JavaFXUtils;
import mci.dibse.crazylabyrinth.client.infrastructure.util.PlayerPawnUtils;
import mci.dibse.crazylabyrinth.client.service.GameServerSocketService;
import mci.dibse.crazylabyrinth.client.service.animation.AnimationService;
import mci.dibse.crazylabyrinth.client.view.viewmodel.GameViewModel;
import mci.dibse.crazylabyrinth.client.view.viewmodel.item.*;
import mci.dibse.crazylabyrinth.client.view.viewmodel.navigation.NavigationStateViewModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import static labyrinth.contracts.models.CommandType.PUSH_TILE;

@Component
@Slf4j
public class GameStateController extends BaseViewCleanup {

    private static final List<String> THEME_STYLE_CLASSES = List.of(
            "theme-runehaven",
            "theme-glowing-caves",
            "theme-stone-catacombs",
            "theme-emberforge");

    private final NavigationController navigationController;
    @FXML public StackPane bottomBar;
    @FXML public StackPane topBar;
    @FXML public BorderPane gameContainer;
    @FXML public GridPane boardGrid;
    @FXML public HBox topArrowBox;
    @FXML public VBox rightArrowBox;
    @FXML public VBox leftArrowBox;
    @FXML public HBox bottomArrowBox;

    @FXML public VBox nextTreasurePaneWrapper;
    @FXML public ImageView nextTreasureImage;

    @FXML public StackPane extraTileContainer;
    @FXML public Label extraTileTreasureLabel;
    @FXML public Label extraTileBonusLabel;
    @FXML public VBox extraTileWrapper;
    @FXML public ImageView extraTile;
    @FXML public ImageView extraTileTreasure;
    @FXML public ImageView extraTileBonusImage;
    @FXML public StackPane bonusBadge;
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
    @FXML public StackPane swapBonusBadge;
    @FXML public ImageView swapBonusImage;
    @FXML public StackPane beamBonusBadge;
    @FXML public ImageView beamBonusImage;
    @FXML public StackPane pushTwiceBonusBadge;
    @FXML public ImageView pushTwiceBonusImage;
    @FXML public StackPane pushFixedBonusBadge;
    @FXML public ImageView pushFixedBonusImage;

    @FXML public Circle bonusSwapCircle;
    @FXML public Circle bonusSwapClip;
    @FXML public Circle bonusBeamCircle;
    @FXML public Circle bonusBeamClip;
    @FXML public Circle bonusPushTwiceCircle;
    @FXML public Circle bonusPushTwiceClip;
    @FXML public Circle bonusPushFixedCircle;
    @FXML public Circle bonusPushFixedClip;

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
    @FXML public ColumnConstraints leftGameGridColomn;
    @FXML public ColumnConstraints middleGameGridColomn;
    @FXML public ColumnConstraints rightGameGridColomn;

    private static final int GAP_BETWEEN_TILES = 2;
    private static final int MAX_PLAYERS = 4;
    private static final String TIMEOUT_LABEL = "Timeout aufgrund von Untätigkeit";
    private static final String GENERAL_ERROR_TITLE = "Achtung Fehler!";
    private static final PseudoClass ACTIVE_BONUS_BUTTON_PSEUDO_CLASS =
            PseudoClass.getPseudoClass("bonus-active");
    private static final PseudoClass AVAILABLE_BONUS_BUTTON_PSEUDO_CLASS =
            PseudoClass.getPseudoClass("bonus-available");

    private static final String QUIT_GAME_MODAL_TITLE = "Beenden";
    private static final String QUIT_GAME_MODAL_HEADER = "Spiel wirklich beenden?";
    private static final String QUIT_GAME_MODAL_CONTENT =
            "Das laufende Spiel wird beendet und Sie kehren zur Serverübersicht zurück";

    private final GameViewModel gameViewModel;
    private final Map<String, Label> idToPlayerLabel = new HashMap<>();
    private final Map<String, Label> idToPlayerStatusLabel = new HashMap<>();

    private Map<BonusType, ChangeListener<Boolean>> bonusAvailableListeners = new HashMap<>();
    private SimpleBooleanProperty pushTwicePendingAction = new SimpleBooleanProperty(false);

    private IntegerProperty remainingGameTimer = new SimpleIntegerProperty();
    private Timeline gameTimeline;

    private IntegerProperty remainingTurnTimer = new SimpleIntegerProperty();
    private Timeline turnTimeline;
    private ChangeListener<OffsetDateTime> turnTimeChangeListener;

    private ScreenResolutionProperties screenResolutionProperties;
    private final Map<String, ImageView> idToPlayerPawn = new HashMap<>();

    private final AnimationService animationService;

    @Autowired
    public GameStateController(
            NavigationStateViewModel vm,
            GameViewModel gameViewModel,
            ScreenResolutionProperties screenResolutionProperties,
            NavigationController navigationController) {
        super(vm);
        this.gameViewModel = gameViewModel;
        this.screenResolutionProperties = screenResolutionProperties;
        this.animationService = new AnimationService();
        this.navigationController = navigationController;
    }

    /**
     * Initialize bindings, listeners, and UI elements for the game screen.
     */
    @FXML
    public void initialize() {
        applyThemeStyleClass();

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

        initBoni();

        setupErrorHandling();

        initGameTimer();

        initNextTreasure();

        initViewToRatio();
        // delay init of scaling & gamegrid until several pulse runs
        viewport.sceneProperty()
                .addListener(
                        (obs, oldScene, newScene) -> {
                            if (newScene != null) {
                                Platform.runLater(
                                        () -> {
                                            root.applyCss();
                                            root.setMaxSize(
                                                    screenResolutionProperties.getWidth(),
                                                    screenResolutionProperties.getScreenHeight());
                                            root.setPrefSize(
                                                    screenResolutionProperties.getWidth(),
                                                    screenResolutionProperties.getScreenHeight());
                                            root.layout();
                                            viewport.layout();
                                            centerStackPane.layout();
                                            Platform.runLater(
                                                    () -> {
                                                        initUiScaling();
                                                        initGameGrid();
                                                    });
                                        });
                            }
                        });
    }

    private void applyThemeStyleClass() {
        Theme theme = FileNameMapper.getTheme();
        String themeClass = switch (theme) {
            case RUNEHAVEN_LABYRINTH -> "theme-runehaven";
            case GLOWING_CAVES -> "theme-glowing-caves";
            case STONE_CATACOMBS -> "theme-stone-catacombs";
            case EMBERFORGE_MAZE -> "theme-emberforge";
        };
        viewport.getStyleClass().removeAll(THEME_STYLE_CLASSES);
        if (!viewport.getStyleClass().contains(themeClass)) {
            viewport.getStyleClass().add(themeClass);
        }
    }



    /**
     * Initialize UI scaling based on screen resolution.
     */
    private void initUiScaling() {
        ScreenResolutionProperties.ResolutionProperties props =
                new ScreenResolutionProperties.ResolutionProperties(root, viewport);
        screenResolutionProperties.initUiScaling(props);
    }

    /**
     * Wait for running animations to finish, then rebuild the game grid.
     */
    private void waitForAnimationsAndRecreateGrid() {
        if (animationService.isAnimationInProgress()) {
            Platform.runLater(this::waitForAnimationsAndRecreateGrid);
        } else {
            initGameGrid();
        }
    }

    /**
     * Decide whether cleanup should run when leaving the game screen.
     *
     * @param oldValue previous screen
     * @param newValue new screen
     * @return true when cleanup should run
     */
    @Override
    protected boolean customCleanupCheckpoint(
            NavigationStateViewModel.ScreenId oldValue,
            NavigationStateViewModel.ScreenId newValue) {
        return newValue != null
                && NavigationStateViewModel.ScreenId.GAMEBOARD.equals(oldValue)
                && !NavigationStateViewModel.ScreenId.GAMEBOARD.equals(newValue);
    }

    /**
     * Clear bindings, listeners, and timers on view exit.
     */
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

        gameTimeline.stop();
        gameTimeline = null;

        turnTimeline.stop();
        turnTimeline = null;
    }

    /**
     * Handle locally published command events to reset bonus button states.
     *
     * @param event send command event
     */
    @EventListener
    public void handleSendCommandEvent(GameServerSocketService.SendCommand event) {
        CommandType command = event.commandType();
        switch (command) {
            case PUSH_TILE, USE_PUSH_FIXED ->  {
                toggleOrSetBonusButton(BONUS_TYPE.PUSH_FIXED, false);
                if(PUSH_TILE.equals(command)) {
                    if (gameViewModel.isPushTwiceTileBonusActive()) {
                        toggleOrSetBonusButton(BONUS_TYPE.PUSH_TWICE, false);
                    } else {
                        pushTwicePendingAction.set(false);
                    }
                }
            }
            case USE_BEAM -> toggleOrSetBonusButton(BONUS_TYPE.BEAM, false);
            case USE_SWAP -> toggleOrSetBonusButton(BONUS_TYPE.SWAP, false);
           case USE_PUSH_TWICE -> {
               pushTwicePendingAction.set(true);
           }
        }
    }

    /**
     * Resolve the player name label by index.
     *
     * @param index player index
     * @return label for the player slot
     */
    private Label getPlayerLabelByIndex(int index) {
        return switch (index) {
            case 0 -> player1;
            case 1 -> player2;
            case 2 -> player3;
            case 3 -> player4;
            default -> throw new IllegalArgumentException("Invalid player index: " + index);
        };
    }

    /**
     * Resolve the player indicator circle by index.
     *
     * @param index player index
     * @return circle for the player slot
     */
    private Circle getPlayerCircleByIndex(int index) {
        return switch (index) {
            case 0 -> player1Circle;
            case 1 -> player2Circle;
            case 2 -> player3Circle;
            case 3 -> player4Circle;
            default -> throw new IllegalArgumentException("Invalid player index: " + index);
        };
    }

    /**
     * Resolve the player status label by index.
     *
     * @param index player index
     * @return status label for the player slot
     */
    private Label getPlayerStatusLabelByIndex(int index) {
        return switch (index) {
            case 0 -> player1Status;
            case 1 -> player2Status;
            case 2 -> player3Status;
            case 3 -> player4Status;
            default -> throw new IllegalArgumentException("Invalid player index: " + index);
        };
    }

    /**
     * Initialize the player turn label and icon bindings.
     */
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

    /**
     * Initialize the turn countdown display and listener.
     */
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

    /**
     * Initialize the turn timer timeline.
     */
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

    /**
     * Initialize player labels, colors, and bindings.
     */
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
            idToPlayerStatusLabel.put(
                    playerStateItem.getPlayerIdAsString(), getPlayerStatusLabelByIndex(i));
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
                            Label playerStatusLabel =
                                    idToPlayerStatusLabel.get(playerState.getPlayerIdAsString());
                            bindPlayerLabel(playerLabel, playerState, false);
                            bindPlayerStatusLabel(playerStatusLabel, playerState);
                        });
    }

    /**
     * Bind the player status label to treasure counts and AI state.
     *
     * @param statusLabel label to bind
     * @param playerState player state item
     */
    private void bindPlayerStatusLabel(Label statusLabel, PlayerStateItem playerState) {
        String maxAvailableTreasurePerPlayer =
                String.valueOf(playerState.getRemainingTreasureCountAsInt());

        statusLabel.textProperty().unbind();
        statusLabel
                .textProperty()
                .bind(
                        Bindings.createStringBinding(
                                () -> {
                                    String playerStatusLabelString =
                                            "Schätze ("
                                                    + playerState.getTreasuresFoundAsInt()
                                                    + " / "
                                                    + maxAvailableTreasurePerPlayer
                                                    + ")";
                                    if (playerState.isPlayerAiControlled())
                                        playerStatusLabelString += " - (AI)";
                                    return playerStatusLabelString;
                                },
                                playerState.getPlayerInfo().get().isAiControlledProperty(),
                                playerState.getTreasuresFound()));
    }

    /**
     * Bind the player label to the player name with optional client prefix.
     *
     * @param label label to bind
     * @param player player state item
     * @param isClient true when the player is the client
     */
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

    /**
     * Initialize spare tile bindings and rotation controls.
     */
    private void initSpareTile() {
        extraTileWrapper.visibleProperty().set(true);

        ChangeListener<TileItem> spareTileListener =
                (obs, oldValue, newValue) -> {
                    if (oldValue != null) {
                        oldValue.dispose();
                    }
                    if (newValue != null) {
                        bindSpareTileImage(newValue);
                        bindSpareTileTreasureImage(newValue);
                        bindSpareTileBonusImage(newValue);
                    }
                };

        gameViewModel.addListenerToSpareTileProperty(spareTileListener);
        if (gameViewModel.getSpareTileItem() != null) {
            TileItem currentTile = gameViewModel.getSpareTileItem();
            bindSpareTileImage(currentTile);
            bindSpareTileTreasureImage(currentTile);
            bindSpareTileBonusImage(currentTile);
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

    /**
     * Bind the bonus image for the spare tile when present.
     *
     * @param currentTile current spare tile
     */
    private void bindSpareTileBonusImage(TileItem currentTile) {
        if (!currentTile.hasBonus()) {
            bonusBadge.setVisible(false);
            bonusBadge.setManaged(false);
            extraTileBonusImage.setVisible(false);
            extraTileBonusImage.setManaged(false);
            return;
        }
        Image image = ImageResourceLoader.loadImage(currentTile.getBonusType());
        extraTileBonusImage.setImage(image);

        bonusBadge.setVisible(true);
        bonusBadge.setManaged(true);
        extraTileBonusImage.setVisible(true);
        extraTileBonusImage.setManaged(true);
    }

    /**
     * Bind the treasure image for the spare tile when present.
     *
     * @param currentTile current spare tile
     */
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

          //  ImageViewStyler.applyTreasureStylingToImageView(extraTileTreasure);

        extraTileTreasure.setRotate(currentTile.getRotation().get().doubleValue());

        extraTileTreasure.fitWidthProperty().bind(extraTileWrapper.widthProperty().multiply(0.2));
        extraTileTreasure.setVisible(true);
        extraTileTreasure.setManaged(true);
    }

    /**
     * Bind the spare tile image and rotation.
     *
     * @param currentTile current spare tile
     */
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

    /**
     * Adjust layout widths for different screen ratios.
     */
    private void initViewToRatio() {
        Ratio screenRation = screenResolutionProperties.getAspectRatio();
        switch (screenRation) {
            case FOUR_THREE -> {
                System.out.println("FOUR_THREE");
                leftGameGridColomn.setPercentWidth(18);
                middleGameGridColomn.setPercentWidth(64);
                rightGameGridColomn.setPercentWidth(18);
                rightSidebar.getStyleClass().add("sidebar-right");
                leftSidebar.getStyleClass().add("left-sidebar");
                setupStyleBonusBadge(screenRation);
            }
            case SIXTEEN_NINE -> {
                leftGameGridColomn.setPercentWidth(23);
                middleGameGridColomn.setPercentWidth(54);
                rightGameGridColomn.setPercentWidth(23);
                rightSidebar.getStyleClass().add("sidebar-right-wide");
                leftSidebar.getStyleClass().add("sidebar-left-wide");
                nextTreasurePaneWrapper.getStyleClass().add("nextTreasurePaneWrapper-wide");
                setupStyleBonusBadge(screenRation);
            }
        }
    }

    /**
     * Configures the visual style and dimensions of the bonus badges based on the screen ratio.
     *
     * @param screenRation the current screen aspect ratio
     */
    private void setupStyleBonusBadge(Ratio screenRation) {
        System.out.println("setupStyleBonusBadge");
        int size = (screenRation == Ratio.FOUR_THREE) ? 14 : 20;
        String styleClass = (screenRation == Ratio.FOUR_THREE) ? "bonus-label-image" : "bonus-label-image-wide";

        updateBadgeStyle(swapBonusBadge, swapBonusImage, bonusSwapCircle, bonusSwapClip, size, styleClass);
        updateBadgeStyle(beamBonusBadge, beamBonusImage, bonusBeamCircle, bonusBeamClip, size, styleClass);
        updateBadgeStyle(pushTwiceBonusBadge, pushTwiceBonusImage, bonusPushTwiceCircle, bonusPushTwiceClip, size, styleClass);
        updateBadgeStyle(pushFixedBonusBadge, pushFixedBonusImage, bonusPushFixedCircle, bonusPushFixedClip, size, styleClass);

    }


    /**
     * Updates a specific bonus badge's dimensions, clipping, and CSS styling.
     */
    private void updateBadgeStyle(StackPane badge, ImageView image, Circle circle, Circle clip, int size, String styleClass) {
        double radius = size / 2.0;

        badge.setPrefSize(size, size);
        badge.setMaxSize(size, size);

        // Update style classes carefully to avoid duplicates
       // image.getStyleClass().removeAll("bonus-label-image", "bonus-label-image-wide");
        image.getStyleClass().add(styleClass);

        // Update background circle
        circle.setCenterX(radius);
        circle.setCenterY(radius);
        circle.setRadius(radius);

        // Update clip circle
        clip.setCenterX(radius);
        clip.setCenterY(radius);
        clip.setRadius(radius);
    }


    /**
     * Build the game grid UI from the current board state.
     */
    private void initGameGrid() {
        GameBoardItem gameBoardItem = gameViewModel.getGameBoardItem();
        int rows = gameBoardItem.getRows();
        int cols = gameBoardItem.getCols();
        Runnable apply =
                () -> {


                    //change style for 9x9 game board
                    if (rows == 9) {
                        topBar.prefHeight(30);
                        topBar.getStyleClass().add("top-bar-9x9");

                        bottomBar.prefHeight(30);
                        bottomBar.getStyleClass().add("timer-bar-9x9");
                    }



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

                        var squareSizeContainer =
                                Bindings.min(parent.widthProperty(), parent.heightProperty());


                        gameContainer.prefWidthProperty().bind(squareSizeContainer);
                        gameContainer.minWidthProperty().set(squareSizeContainer.doubleValue());
                        gameContainer.minHeightProperty().set(squareSizeContainer.doubleValue());
                        gameContainer.maxWidthProperty().set(squareSizeContainer.doubleValue());
                        gameContainer.maxHeightProperty().set(squareSizeContainer.doubleValue());
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
                                ImageView treasureImage =
                                        new ImageView(treasureItem.getTreasureImage());
                                treasureImage.setPreserveRatio(true);
                                double scale = 0.5;

                                treasureImage
                                        .fitWidthProperty()
                                        .bind(cellContainer.widthProperty().multiply(scale));
                                treasureImage
                                        .fitHeightProperty()
                                        .bind(cellContainer.heightProperty().multiply(scale));
                                treasureImage.setRotate(tile.getRotation().get().doubleValue());
                                ImageViewStyler.applyTreasureStylingToImageView(treasureImage);
                                cellContainer.getChildren().add(treasureImage);
                            }

                            if (tile.hasBonus()) {
                                // Label bonusNode = tempLabelImplforBonus(tile.getBonus().get());
                                StackPane bonusNode = getBonusNodeForTile(tile.getBonusType());
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

                                PlayerColor playerColor = playerStateItem.getPlayerColor();

                                ImageView playerImage = new ImageView();
                                Image image = ImageResourceLoader.loadImage(playerColor);
                                playerImage.setPreserveRatio(true);
                                playerImage.setImage(image);
                                ImageViewStyler.applyPlayerStylingToImageView(
                                        playerImage, playerColor);

                                if (tile.hasTreasure())
                                    offsetPlayerPawnOnTreasurePresence(tile, playerImage);
                                double scale = 0.45;
                                playerImage
                                        .fitWidthProperty()
                                        .bind(cellContainer.widthProperty().multiply(scale));
                                playerImage
                                        .fitHeightProperty()
                                        .bind(cellContainer.heightProperty().multiply(scale));
                                idToPlayerPawn.put(
                                        playerStateItem.getPlayerIdAsString(), playerImage);
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
        // JavaFXUtils.runOnFxThread(apply);
    }

    /**
     * Offset a player pawn when a treasure is also present in the tile.
     *
     * @param tile tile containing the pawn
     * @param playerImage player image view
     */
    private void offsetPlayerPawnOnTreasurePresence(TileItem tile, ImageView playerImage) {
        List<Direction> directions = tile.getDirections();
        for (Direction direction : directions) {
            if (Direction.DOWN.equals(direction)) continue;
            switch (direction) {
                case UP -> playerImage.setTranslateY(-30);
                case LEFT -> playerImage.setTranslateX(-25);
                case RIGHT -> playerImage.setTranslateX(25);
            }
            break;
        }
    }

    /**
     * Mark a cell as a player's home base if applicable.
     *
     * @param row row index
     * @param col column index
     * @param cellContainer cell container to decorate
     */
    private void markPlayerHomeBase(int row, int col, StackPane cellContainer) {
        gameViewModel.getPlayerStatesSorted().stream()
                .filter(
                        player ->
                                player.getHomePosition().get().getColumnAsInt() == col
                                        && player.getHomePosition().get().getRowAsInt() == row)
                .findFirst()
                .ifPresent(
                        playerStateItem ->
                                addStyleToPlayerHomeBase(cellContainer, playerStateItem));
    }

    /**
     * Add a colored border to indicate a player's home base.
     *
     * @param cellContainer cell container to decorate
     * @param playerStateItem player state item
     */
    private void addStyleToPlayerHomeBase(
            StackPane cellContainer, PlayerStateItem playerStateItem) {

        String styleColor = playerStateItem.getPlayerColor().toString().toLowerCase();

        Rectangle borderOverlay = new Rectangle();

        borderOverlay.widthProperty().bind(cellContainer.widthProperty());
        borderOverlay.heightProperty().bind(cellContainer.heightProperty());

        borderOverlay.setFill(Color.TRANSPARENT);

        Color baseColor =
                Color.web(styleColor); // web() handles standard color names like "red", "blue"
        // Set the border color and thickness
        Color softColor =
                new Color(
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

    /**
     * Initialize push tile buttons around the board.
     */
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

    /**
     * Build a horizontal bar of push buttons and spacers.
     *
     * @param bar target bar
     * @param dir push direction
     * @param size board size
     */
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

    /**
     * Build a vertical bar of push buttons and spacers.
     *
     * @param bar target bar
     * @param dir push direction
     * @param size board size
     */
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

    /**
     * Bind an item's width to a board grid column width.
     *
     * @param item UI element to bind
     * @param size board size
     * @return true when the index is valid for pushing
     */
    private void bindSlotWidth(Region item, int size) {
        item.prefWidthProperty().bind(boardGrid.widthProperty().divide(size));
    }

    /**
     * Bind an item's height to a board grid row height.
     *
     * @param item UI element to bind
     * @param size board size
     */
    private void bindSlotHeight(Region item, int size) {
        item.prefHeightProperty().bind(boardGrid.heightProperty().divide(size));
    }

    /**
     * Create a base arrow button with direction styles.
     *
     * @param text button text
     * @param dir arrow direction
     * @return configured button
     */
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

    /**
     * Create a push tile button for a valid push index.
     *
     * @param text button text
     * @param index row/column index
     * @param dir push direction
     * @return configured button
     */
    private Button createArrowButton(String text, int index, Direction dir) {
        Button b = createBaseArrowButton(text, dir);
        b.setOnAction(_ -> choosePushTileCommand(index, dir, PUSH_TILE));
        b.disableProperty().bind(shouldPushTileButtonBeDisabled());
        return b;
    }

    /**
     * Create a push fixed bonus button for a valid index.
     *
     * @param text button text
     * @param index row/column index
     * @param dir push direction
     * @return configured button
     */
    private Button createPushFixedBonusArrowButton(String text, int index, Direction dir) {
        Button b = createBaseArrowButton(text, dir);
        b.getStyleClass().add("arrow-button-fixed");
        b.setOnAction(_ -> choosePushTileCommand(index, dir, CommandType.USE_PUSH_FIXED));

        b.disableProperty().bind(shouldPushFixedTileButtonBeDisabled());
        return b;
    }

    /**
     * Check if a row/column index is valid for a standard push.
     *
     * @param index row/column index
     * @param size board size
     * @return true when the index is valid for pushing
     */
    private boolean isValidatePushIndex(int index, int size) {
        return (index % 2 == 1) && (index <= size - 2);
    }

    /**
     * Check if a row/column index is valid for a push-fixed bonus.
     *
     * @param index row/column index
     * @param size board size
     * @return true when a push-fixed bonus can be used
     */
    private boolean isBonusEnabledAndValidPushFixedBonusIndex(int index, int size) {
        return gameViewModel.isBonusEnabled()
                && (index % 2 == 0)
                && (index <= size - 2)
                && (index > 0);
    }

    /**
     * Return a binding indicating it's not the client's turn or they timed out.
     *
     * @return binding for turn/timeout state
     */
    private BooleanBinding isNotMyTurnAndTimeout() {
        return gameViewModel
                .getCurrentPlayerIdProperty()
                .isNotEqualTo(gameViewModel.getClientPlayer().getId())
                .or(gameViewModel.getTurnTimeoutProperty());
    }

    /**
     * Return a binding indicating push actions should be disabled.
     *
     * @return binding for push phase availability
     */
    private BooleanBinding isNotMyTurnAndTimeoutAndPushPhase() {
        return isNotMyTurnAndTimeout()
                .or(
                        gameViewModel
                                .getCurrentTurnStateProperty()
                                .isNotEqualTo(TurnState.WAITING_FOR_PUSH));
    }

    /**
     * Return a binding indicating move actions should be disabled.
     *
     * @return binding for move phase availability
     */
    private BooleanBinding isNotMyTurnAndTimeoutAndMovePhase() {
        return isNotMyTurnAndTimeout()
                .or(
                        gameViewModel
                                .getCurrentTurnStateProperty()
                                .isNotEqualTo(TurnState.WAITING_FOR_MOVE));
    }

    /**
     * Determine whether a tile should be disabled for mouse interaction.
     *
     * @param playerOnTile optional player on the tile
     * @return binding indicating whether the tile should be disabled
     */
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

    /**
     * Determine whether standard push buttons should be disabled.
     *
     * @return binding for push button availability
     */
    private BooleanBinding shouldPushTileButtonBeDisabled() {
        return isNotMyTurnAndTimeoutAndPushPhase()
                .or(gameViewModel.getIsPushFixedTileBonusActiveProperty())
                .or(gameViewModel.getIsBeamTileBonusActiveProperty())
                .or(gameViewModel.getIsSwapBonusActiveProperty());
    }

    /**
     * Determine whether push-fixed bonus buttons should be disabled.
     *
     * @return binding for push-fixed button availability
     */
    private BooleanBinding shouldPushFixedTileButtonBeDisabled() {
        return isNotMyTurnAndTimeoutAndPushPhase()
                .or(gameViewModel.getIsPushFixedTileBonusActiveProperty().not());
    }

    /**
     * Determine whether a bonus button should be disabled.
     *
     * @param bonusType bonus type to evaluate
     * @return binding for bonus button availability
     */
    private BooleanBinding shouldBonusButtonBeDisabled(BonusType bonusType) {
        return isNotMyTurnAndTimeoutAndPushPhase()
                .or(
                        Bindings.createBooleanBinding(
                                () -> gameViewModel.isAnyBonusActive(bonusType, pushTwicePendingAction),
                                gameViewModel.getIsPushFixedTileBonusActiveProperty(),
                                gameViewModel.getIsSwapBonusActiveProperty(),
                                gameViewModel.getIsBeamTileBonusActiveProperty(),
                                pushTwicePendingAction));
    }

    /**
     * Build and send a push tile command based on the input.
     *
     * @param index row/column index
     * @param dir push direction
     * @param type command type
     */
    private void choosePushTileCommand(int index, Direction dir, CommandType type) {
        Direction[] extraTileEntrances =
                gameViewModel.getSpareTileItem().getDirections().toArray(Direction[]::new);
        pushTileCommand(new PushTileCommandRecord(index, dir, extraTileEntrances, type));
    }

    /**
     * Send a push tile command if it does not revert the last push.
     *
     * @param data push tile command payload
     */
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

    /**
     * Check whether a push would revert the last push.
     *
     * @param index row/column index
     * @param pushDirection push direction
     * @return true when the push would revert the last push
     */
    private boolean checkIfPushRevertsLastPush(int index, Direction pushDirection) {
        GameBoardItem gameBoardItem = gameViewModel.getGameBoardItem();
        ObjectProperty<PushActionInfo> lastPush = gameBoardItem.lastPushProperty();
        if (lastPush.isNull().get()) return false;
        if (!lastPush.get().getRowOrColIndex().equals(index)) return false;

        Direction lastPushDirection = lastPush.get().getDirection();
        return isOppositeDirection(pushDirection, lastPushDirection);
    }

    /**
     * Check whether two directions are opposites.
     *
     * @param pushDirection push direction
     * @param lastPushDirection last push direction
     * @return true when directions are opposites
     */
    private boolean isOppositeDirection(Direction pushDirection, Direction lastPushDirection) {
        return switch (pushDirection) {
            case UP -> lastPushDirection == Direction.DOWN;
            case DOWN -> lastPushDirection == Direction.UP;
            case LEFT -> lastPushDirection == Direction.RIGHT;
            case RIGHT -> lastPushDirection == Direction.LEFT;
        };
    }

    /**
     * Send a move or bonus command depending on active bonuses.
     *
     * @param finalRow target row
     * @param finalCol target column
     * @param playerStateItemOptional optional player at the target
     */
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

    /**
     * Bind the next treasure image to the view model.
     */
    private void initNextTreasure() {
            nextTreasureImage.imageProperty().bind(Bindings.createObjectBinding(
                    () -> {
                          try {
                            return   gameViewModel.getNextTreasureImageProperty().get();
                          } catch (NullPointerException e) {
                              if (! gameViewModel.isNextTreasureMightNotBeAvailableYet())  throw e;
                              log.warn("Treasure image is not yet available return emtpy Image");
                              return new javafx.scene.image.WritableImage(1, 1);
                          }
                      }, gameViewModel.getNextTreasureItemProperty()
            ));
       // ImageViewStyler.applyTreasureStylingToImageView(nextTreasureImage);
    }

    /**
     * Create a bonus badge node for a tile.
     *
     * @param bonusType bonus type
     * @return bonus badge node
     */
    private StackPane getBonusNodeForTile(BonusType bonusType) {
        Image bonus = ImageResourceLoader.loadImage(bonusType);
        ImageView bonusImage = new ImageView(bonus);
        bonusImage.setFitWidth(18);
        bonusImage.setFitHeight(18);
        bonusImage.setPreserveRatio(false); // ensure 30x30, no letterbox
        Circle clip = new Circle(9, 9, 9);

        bonusImage.setClip(clip);

        // visible border:
        Circle border = new Circle(10, 10, 10);
        border.setFill(Color.web("#fffbd5"));
        border.setStroke(Color.BLACK);
        border.setStrokeWidth(1);

        StackPane bonusBadge = new StackPane(border, bonusImage);
        bonusBadge.setPrefSize(20, 20);
        bonusBadge.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane.setMargin(bonusBadge, new Insets(2, 2, 0, 0));
        StackPane.setAlignment(bonusBadge, Pos.TOP_RIGHT);
        return bonusBadge;
    }

    /**
     * Initialize the game counter UI (placeholder).
     */
    private void initGameCounter() {
        // todo how is the set timer send from server?
    }

    /**
     * Initialize bonus UI bindings and listeners.
     */
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

        setupBonusImages();

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
                                this::checkIfPushTwiceBoxShouldBeVisible,
                                gameViewModel.getClientPlayerState().getAvailableBonusTypes()));
        bonusPushTwiceBox
                .managedProperty()
                .bind(
                        Bindings.createBooleanBinding(
                                this::checkIfPushTwiceBoxShouldBeVisible,
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

         bonusPushTwiceButton.disableProperty().bind(shouldBonusButtonBeDisabled(BonusType.PUSH_TWICE));

        ChangeListener<Boolean> bonusPushTwiceButtonAvailableListener =
                (ov, oldVal, newVal) -> {
                    bonusPushTwiceButton.pseudoClassStateChanged(
                            AVAILABLE_BONUS_BUTTON_PSEUDO_CLASS,
                            shouldBonusButtonBeDisabled(BonusType.PUSH_TWICE).not().get());
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

    private Boolean checkIfPushTwiceBoxShouldBeVisible() {
        return getAvailableBonusCountForTypeAndClient(BonusType.PUSH_TWICE) > 0
                ||  gameViewModel.isPushTwiceTileBonusActive();
    }

    /**
     * Set the bonus images for bonus buttons.
     */
    private void setupBonusImages() {
        swapBonusImage.setImage(ImageResourceLoader.loadImage(BonusType.SWAP));
        beamBonusImage.setImage(ImageResourceLoader.loadImage(BonusType.BEAM));
        pushTwiceBonusImage.setImage(ImageResourceLoader.loadImage(BonusType.PUSH_TWICE));
        pushFixedBonusImage.setImage(ImageResourceLoader.loadImage(BonusType.PUSH_FIXED));
    }

    /**
     * Return the available bonus count for the client player.
     *
     * @param type bonus type
     * @return available count
     */
    private int getAvailableBonusCountForTypeAndClient(BonusType type) {
        return gameViewModel.getClientPlayerState().getAvailableBonusCountForType(type);
    }

    /**
     * Bind a row width to the bonus box width.
     *
     * @param row row region
     */
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

    /**
     * Show a JavaFX alert modal with error styling.
     *
     * @param title modal title
     * @param header modal header
     * @param content modal content
     */
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

    /**
     * Resolve the player box by index.
     *
     * @param index player index
     * @return player box
     */
    private VBox getPlayerBoxByIndex(int index) {
        return switch (index) {
            case 0 -> player1Box;
            case 1 -> player2Box;
            case 2 -> player3Box;
            case 3 -> player4Box;
            default -> throw new IllegalArgumentException("Invalid index");
        };
    }

    /**
     * Handle swap bonus button click.
     *
     * @param actionEvent button event
     */
    public void onBonusSwapButtonClick(ActionEvent actionEvent) {
        toggleOrSetBonusButton(BONUS_TYPE.SWAP, null);
    }

    /**
     * Handle beam bonus button click.
     *
     * @param actionEvent button event
     */
    public void onBonusBeamButtonClick(ActionEvent actionEvent) {
        toggleOrSetBonusButton(BONUS_TYPE.BEAM, null);
    }

    /**
     * Handle push twice bonus button click.
     *
     * @param actionEvent button event
     */
    public void onBonusPushTwiceButtonClick(ActionEvent actionEvent) {
        toggleOrSetBonusButton(BONUS_TYPE.PUSH_TWICE, null);
        gameViewModel.sendUsePushTwiceCommand();
    }

    /**
     * Handle push fixed bonus button click.
     *
     * @param actionEvent button event
     */
    public void onBonusPushFixedButtonClick(ActionEvent actionEvent) {
        toggleOrSetBonusButton(BONUS_TYPE.PUSH_FIXED, null);
    }

    /**
     * Toggle or set a bonus button active state.
     *
     * @param type bonus type
     * @param set optional explicit state
     */
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

    /**
     * Set up error handling to show modal errors.
     */
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

    /**
     * Determine whether errors should be shown as modal dialogs.
     *
     * @return true when the error should show a modal
     */
    private boolean shouldErrorBeShownAsModal() {
        if (gameViewModel.getErrorCodeProperty().get() == null) return false;

        return switch (gameViewModel.getErrorCodeProperty().get()) {
            case INVALID_MOVE -> false;
            default -> true;
        };
    }

    /**
     * Handle leave game action and show confirmation.
     *
     * @param actionEvent button event
     */
    public void onLeaveGameButtonClick(ActionEvent actionEvent) {
        log.debug("onLeaveGameButtonClick");
        Runnable onConfirm = gameViewModel::leaveGame;
        JavaFXUtils.showConfirmationModal(
                QUIT_GAME_MODAL_TITLE, QUIT_GAME_MODAL_HEADER, QUIT_GAME_MODAL_CONTENT, onConfirm);
    }

    public void onFullscreenToggleButtonClick(ActionEvent actionEvent) {
        navigationController.toogleFullScreen();
    }

    private enum BONUS_TYPE {
        SWAP,
        BEAM,
        PUSH_TWICE,
        PUSH_FIXED;
    }

    /**
     * Initialize the game timer countdown display.
     */
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

    /**
     * Initialize the game countdown timeline.
     *
     * @param gameTimeSeconds starting game time in seconds
     */
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

    /**
     * Set up listener to animate tile pushes on updates.
     */
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

    /**
     * Set up listeners to animate player movement updates.
     */
    private void initPlayerMovementListener() {
        gameViewModel
                .getPlayerStatesSorted()
                .forEach(
                        playerStateItem -> {
                            playerStateItem
                                    .getCurrentPosition()
                                    .addListener(
                                            (obs, oldPos, newPos) -> {
                                                if (animationService.isTilePushAnimationActive()) {
                                                    log.debug(
                                                            "Skipping player movement animation"
                                                                    + " during tile push");
                                                    return;
                                                }

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
                                                } else {
                                                    animationService.animateBeam(
                                                            idToPlayerPawn.get(
                                                                    playerStateItem
                                                                            .getPlayerIdAsString()),
                                                            oldPos,
                                                            newPos,
                                                            boardGrid);
                                                }
                                            });
                        });
    }
}
