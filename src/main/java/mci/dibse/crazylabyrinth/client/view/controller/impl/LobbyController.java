package mci.dibse.crazylabyrinth.client.view.controller.impl;

import java.util.List;
import java.util.stream.Collectors;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import labyrinth.contracts.models.*;
import labyrinth.contracts.models.RankingEntry;
import lombok.extern.slf4j.Slf4j;
import mci.dibse.crazylabyrinth.client.NavigationController;
import mci.dibse.crazylabyrinth.client.domain.context.ApplicationEvents;
import mci.dibse.crazylabyrinth.client.domain.model.enums.Theme;
import mci.dibse.crazylabyrinth.client.domain.model.record.StartGameRecord;
import mci.dibse.crazylabyrinth.client.infrastructure.util.*;
import mci.dibse.crazylabyrinth.client.service.AchievementService;
import mci.dibse.crazylabyrinth.client.view.viewmodel.GameViewModel;
import mci.dibse.crazylabyrinth.client.view.viewmodel.LobbyViewModel;
import mci.dibse.crazylabyrinth.client.view.viewmodel.item.PlayerInfoItem;
import mci.dibse.crazylabyrinth.client.view.viewmodel.navigation.NavigationStateViewModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LobbyController extends BaseViewCleanup {

    private final GameViewModel gameViewModel;
    private final AchievementService achievementService;
    private final NavigationController navigationController;
    public BooleanProperty clientAdmin = new SimpleBooleanProperty(false);

    // todo add results
    @FXML public VBox resultsBox;

    @FXML public TableView<PlayerInfoItem> playerTable;
    @FXML public TableColumn<PlayerInfoItem, String> playerNameColumn;
    @FXML public TableColumn<PlayerInfoItem, PlayerColor> colorColumn;
    @FXML public TableColumn<PlayerInfoItem, Boolean> adminColumn;
    @FXML public VBox adminSettings;
    @FXML public Label gameNameLabel;
    @FXML public Label maxPlayers;
    @FXML public Label connectedPlayers;
    @FXML public Spinner<Integer> gameTimerSpinner;
    @FXML public CheckBox timerEnabledCheck;
    @FXML public HBox timerSettings;
    @FXML public HBox bonusSetting;
    @FXML public Spinner<Integer> bonusCountSpinner;
    @FXML public Button leaveLobby;
    @FXML public TableView<RankingEntry> resultsTable;
    @FXML public TableColumn<RankingEntry, String> resultNameColumn;
    @FXML public TableColumn<RankingEntry, String> resultScoreColumn;
    @FXML public TableColumn<RankingEntry, RankingEntry> achievements;
    @FXML private ListView<String> playerList;
    @FXML public VBox leftBar;
    @FXML public VBox rightBar;

    @FXML public StackPane viewport;
    @FXML public Rectangle rootClip;
    @FXML public Group contentGroup;
    @FXML public BorderPane root;

    // Admin Stuff

    @FXML public CheckBox bonusEnabledCheck;
    @FXML public ComboBox<String> boardSizeCombo;
    @FXML public Spinner<Integer> treasureCardsSpinner;
    @FXML private Button startGameButton;
    @FXML public ComboBox<Theme> boardTheme;

    @FXML public Label countdown;
    @FXML public HBox countDownBox;

    private static final String ERROR_MODAL_TITLE = "Error";
    private static final String SMALL_GAME_BOARD_SIZE = "Klein (5x5)";
    private static final String MEDIUM_GAME_BOARD_SIZE = "Normal (7x7)";
    private static final String LARGE_GAME_BOARD_SIZE = "Groß (9x9)";
    private static final String DEFAULT_GAME_BOARD_SIZE_LABEL = MEDIUM_GAME_BOARD_SIZE;
    private static final int MIN_PLAYERS_FOR_GAME_START = 1;
    private static final int MIN_TREASURE_PER_PLAYER = 1;
    private static final int MAX_TREASURE_PER_PLAYER = 6;
    private static final int DEFAULT_TREASURE_PER_PLAYER = 6;
    private static final int MIN_GAME_TIMER_MINUTES = 1;
    private static final int MAX_GAME_TIMER_MINUTES = 120;
    private static final int MIN_BONUS_COUNT = 0;
    private static final int MAX_BONUS_COUNT = 20;
    private static final int DEFAULT_BONUS_COUNT = 0;
    private static final int DEFAULT_START_GAME_TIMER = 60;

    private final BooleanProperty isStartButtonLocked = new SimpleBooleanProperty(false);

    private IntegerProperty remainingTimer = new SimpleIntegerProperty(DEFAULT_START_GAME_TIMER);
    private Timeline countdownTimeline;

    private final LobbyViewModel lobbyViewModel;

    private Runnable cleanUpTaskPlayerTable;
    private Runnable cleanUpTaskResultsTable;

    private ScreenResolutionProperties screenResolutionProperties;
    ChangeListener<Theme> themeChangeListener;

    @Autowired
    public LobbyController(NavigationStateViewModel navState, LobbyViewModel lobbyViewModel, ScreenResolutionProperties screenResolutionProperties, GameViewModel gameViewModel, AchievementService achievementService, NavigationController navigationController) {
        super(navState);
        this.lobbyViewModel = lobbyViewModel;
        this.screenResolutionProperties = screenResolutionProperties;
        this.gameViewModel = gameViewModel;
        this.achievementService = achievementService;
        this.navigationController = navigationController;
    }

    /**
     * Initialize bindings, table configuration, and listeners.
     */
    @FXML
    public void initialize() {

        initViewToRatio();

        playerTable.setItems(lobbyViewModel.getPlayersListProperty());

        // Clip/cleanup
        cleanUpTaskPlayerTable = JavaFXUtils.setupTableClip(playerTable, 30.0);

        setUpTableColumns();

        var tooFewPlayers =
                Bindings.size(lobbyViewModel.getPlayersListProperty())
                        .lessThanOrEqualTo(MIN_PLAYERS_FOR_GAME_START);
        startGameButton.disableProperty().bind(tooFewPlayers.or(isStartButtonLocked));

        clientAdmin.set(lobbyViewModel.isClientAdmin());
        adminSettings.visibleProperty().bind(clientAdmin);
        ListChangeListener<PlayerInfoItem> listener =
                (change) -> {
                    clientAdmin.set(lobbyViewModel.isClientAdmin());
                };
        lobbyViewModel.addListenerToPlayersListProperty(listener);

        maxPlayers.textProperty().bind(lobbyViewModel.getMaxPlayersProperty());
        connectedPlayers.textProperty().bind(lobbyViewModel.getPlayerCountProperty());
        gameNameLabel.textProperty().bind(lobbyViewModel.getGameNameProperty());

        boardSizeCombo.setValue(mapBoardSizeToLabel(lobbyViewModel.getDefaultBoardSize()));

        SpinnerValueFactory<Integer> valueFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(
                        MIN_TREASURE_PER_PLAYER,
                        MAX_TREASURE_PER_PLAYER,
                        DEFAULT_TREASURE_PER_PLAYER);
        treasureCardsSpinner.setValueFactory(valueFactory);

        // TODO min max??
        timerEnabledCheck.selectedProperty().set(lobbyViewModel.isDefaultTimerEnabled());
        timerSettings.visibleProperty().bind(timerEnabledCheck.selectedProperty());
        timerSettings.managedProperty().bind(timerEnabledCheck.selectedProperty());

        int defaultTimerInMinutes = secondsToMinutes(lobbyViewModel.getDefaultTimer());
        SpinnerValueFactory<Integer> timerFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(
                        MIN_GAME_TIMER_MINUTES, MAX_GAME_TIMER_MINUTES, defaultTimerInMinutes);
        gameTimerSpinner.setValueFactory(timerFactory);

        bonusSetting.visibleProperty().bind(bonusEnabledCheck.selectedProperty());
        bonusSetting.managedProperty().bind(bonusEnabledCheck.selectedProperty());

        SpinnerValueFactory<Integer> bonusFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(
                        MIN_BONUS_COUNT, MAX_BONUS_COUNT, DEFAULT_BONUS_COUNT);
        bonusCountSpinner.setValueFactory(bonusFactory);

        setupCountDownTimer();

        setupErrorHandling();

        setupResultsTable();

        setupThemeComboBox();

        viewport.sceneProperty()
                .addListener(
                        (obs, oldScene, newScene) -> {
                            if (newScene != null) {
                                // First pulse: attach
                                Platform.runLater(
                                        () -> {
                                            root.applyCss();
                                            root.layout(); // ensures layoutBounds valid now
                                            initUiScaling(); // sets up bindings only (no pref in
                                            // lambdas)
                                        });
                            }
                        });
    }

    /**
     * Adjust side bar widths based on screen ratio.
     */
    private void initViewToRatio() {
        Ratio ratio = screenResolutionProperties.getAspectRatio();
        switch (ratio) {
            case FOUR_THREE -> {
                leftBar.setPrefWidth(225);
                rightBar.setPrefWidth(200);
            }
            case SIXTEEN_NINE -> {
                leftBar.setPrefWidth(250);
                rightBar.setPrefWidth(250);
            }
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
     * Configure the results table when results become available.
     */
    private void setupResultsTable() {
        log.info("setupResultsTable");

        ChangeListener<Boolean> resultsListener =
                (obs, oldVal, newVal) -> {
                    Runnable task =
                            () -> {
                                ObservableList<RankingEntry> rankingEntriesSorted =
                                        lobbyViewModel.getSortedRankingEntries();
                                resultsTable.setItems(rankingEntriesSorted);
                                lobbyViewModel.bindSortedRankingsComparator(
                                        resultsTable.comparatorProperty());

                                int numberEntries = rankingEntriesSorted.size();
                                // numberEntries
                                cleanUpTaskResultsTable =
                                        JavaFXUtils.setupTableClip(resultsTable, 30.0);

                                resultNameColumn.setCellValueFactory(
                                        cd ->
                                                new SimpleStringProperty(
                                                        cd.getValue().getPlayerId()));
                                resultNameColumn.setCellFactory(
                                        column -> {
                                            return new TableCell<RankingEntry, String>() {
                                                @Override
                                                protected void updateItem(
                                                        String item, boolean empty) {
                                                    super.updateItem(item, empty);
                                                    if (empty) {
                                                        setText(null);
                                                        return;
                                                    }
                                                    String name =
                                                            lobbyViewModel.getPlayerNameForId(item);
                                                    if (lobbyViewModel.getWinnerId().equals(item))
                                                        name += " (Gewinner)";
                                                    setText(name);
                                                }
                                            };
                                        });

                                resultScoreColumn.setCellValueFactory(
                                        cd ->
                                                new SimpleStringProperty(
                                                        cd.getValue().getScore().toString()));

                                achievements.setCellValueFactory(
                                        cd ->
                                                new SimpleObjectProperty<RankingEntry>(
                                                        cd.getValue()));
                                achievements.setCellFactory(
                                        column -> {
                                            return new TableCell<RankingEntry, RankingEntry>() {
                                                @Override
                                                protected void updateItem(
                                                        RankingEntry item, boolean empty) {
                                                    super.updateItem(item, empty);
                                                    if (empty) {
                                                        setText(null);
                                                        return;
                                                    }

                                                    //
                                                    // setGraphic(createAchievementGraphicForPlayer(item));
                                                    String achievements =
                                                            getAchievementResultsStringForPlayer(
                                                                    item);
                                                    setText(achievements);
                                                }
                                            };
                                        });

                                resultsBox.setVisible(true);
                                resultsBox.setManaged(true);
                            };
                    JavaFXUtils.runOnFxThread(task);
                };

        lobbyViewModel.addListenerToResultsAvailableProperty(resultsListener);
    }

    /**
     * Handle the leave lobby button click.
     *
     * @param actionEvent button event
     */
    public void onleaveLobbyButtonClick(ActionEvent actionEvent) {
        lobbyViewModel.leaveLobby();
    }

    /**
     * Handle the start game button click and trigger countdown lock.
     */
    @FXML
    public void onStartButtonClick() {
        isStartButtonLocked.set(true);
        PauseTransition pause = new PauseTransition(Duration.seconds(3));
        pause.setOnFinished(e -> isStartButtonLocked.set(false));
        pause.play();
        cancelCountdown();
        startGame();
    }

    /**
     * Cancel countdown when a disconnect event occurs in the lobby.
     *
     * @param event application event
     */
    @EventListener
    public void handleDisconnectEvent(ApplicationEvents event) {
        if (NavigationStateViewModel.ScreenId.LOBBY.equals(
                navigationStateViewModel.getCurrentScreenId())) {
            try {
                cancelCountdown();
            } catch (Exception e) {
            }
        }
    }

    /**
     * Decide whether cleanup should run when leaving the lobby.
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
                && NavigationStateViewModel.ScreenId.LOBBY.equals(oldValue)
                && !NavigationStateViewModel.ScreenId.LOBBY.equals(newValue);
    }

    /**
     * Clear bindings, listeners, and table state on view exit.
     */
    @Override
    public void cleanup() {
        /*
              1. Clear property bindings
              2. Clear event handlers
              3. Clear table cell factories (breaks reference chains)
              4. Clear table data
              5. Clear combo box items

        */
        JavaFXUtils.runOnFxThread(
                () -> {
                    maxPlayers.textProperty().unbind();
                    connectedPlayers.textProperty().unbind();
                    gameNameLabel.textProperty().unbind();
                    adminSettings.visibleProperty().unbind();
                    timerSettings.visibleProperty().unbind();
                    timerSettings.managedProperty().unbind();
                    startGameButton.disableProperty().unbind();
                    bonusSetting.visibleProperty().unbind();
                    bonusSetting.managedProperty().unbind();
                    countdown.textProperty().unbind();
                    countDownBox.visibleProperty().unbind();
                    countDownBox.managedProperty().unbind();

                    startGameButton.setOnAction(null);

                    timerEnabledCheck.setOnAction(null);

                    playerNameColumn.setCellValueFactory(null);
                    colorColumn.setCellFactory(null);
                    adminColumn.setCellValueFactory(null);

                    playerTable.setItems(null);

                    resultsTable.setItems(null);

                    boardSizeCombo.getItems().clear();

                    countdownTimeline = null;

                    resultsBox.setVisible(false);
                    resultsBox.setManaged(false);

                    boardTheme
                            .getSelectionModel()
                            .selectedItemProperty()
                            .removeListener(themeChangeListener);
                });
        cleanUpTaskPlayerTable.run();
        if (cleanUpTaskResultsTable != null) cleanUpTaskResultsTable.run();
    }

    /**
     * Initialize countdown timer bindings and listeners.
     */
    private void setupCountDownTimer() {
        initCountdownTimeline();

        initListener();

        countdown.textProperty().bind(remainingTimer.asString().concat(" s"));
        BooleanBinding isRunning =
                Bindings.createBooleanBinding(
                        () ->
                                countdownTimeline
                                        .statusProperty()
                                        .isEqualTo(Animation.Status.RUNNING)
                                        .get(),
                        countdownTimeline.statusProperty());

        countDownBox.visibleProperty().bind(isRunning);
        countDownBox.managedProperty().bind(isRunning);

        if (lobbyViewModel.isClientAdmin() && lobbyViewModel.getPlayersListProperty().size() > 1) {
            startCountdownIfNeeded();
        } else if (lobbyViewModel.isClientAdmin()) {
            cancelCountdown();
        }
    }

    /**
     * Initialize the countdown timeline used for auto-start.
     */
    private void initCountdownTimeline() {
        countdownTimeline =
                new Timeline(
                        new KeyFrame(
                                Duration.seconds(1),
                                e -> {
                                    remainingTimer.set(remainingTimer.get() - 1);
                                    if (remainingTimer.get() <= 0 && countdownTimeline != null) {
                                        countdownTimeline.stop();
                                        if (lobbyViewModel.isClientAdmin()) startGame();
                                    }
                                }));
        countdownTimeline.setCycleCount(
                Animation.INDEFINITE); // we control stopping via remainingTimer
    }

    /**
     * Register listeners that manage the countdown based on player count.
     */
    private void initListener() {
        ListChangeListener<PlayerInfoItem> countDownTimerListener =
                (change) -> {
                    while (change.next()) {
                        int size = change.getList().size();
                        if (lobbyViewModel.isClientAdmin() && size > 1) {
                            startCountdownIfNeeded();
                        } else if (lobbyViewModel.isClientAdmin()) {
                            cancelCountdown();
                        }
                    }
                };
        lobbyViewModel.addListenerToPlayersListProperty(countDownTimerListener);
    }

    /**
     * Stop the countdown and reset timer to the default value.
     */
    private void cancelCountdown() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }
        remainingTimer.set(DEFAULT_START_GAME_TIMER);
    }

    /**
     * Start the countdown when not already running.
     */
    private void startCountdownIfNeeded() {
        if (countdownTimeline == null) {
            initCountdownTimeline();
        }
        if (countdownTimeline.getStatus() != Animation.Status.RUNNING) {
            remainingTimer.set(DEFAULT_START_GAME_TIMER);
            countdownTimeline.playFromStart();
        }
    }

    /**
     * Configure the player table columns and cell factories.
     */
    private void setUpTableColumns() {
        playerNameColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        colorColumn.setCellValueFactory(cellData -> cellData.getValue().colorProperty());
        adminColumn.setCellValueFactory(cellData -> cellData.getValue().isAdminProperty());

        // Add custom cell factory to display a square block as the color
        colorColumn.setCellFactory(
                column ->
                        new TableCell<PlayerInfoItem, PlayerColor>() {
                            private final Rectangle colorBox = new Rectangle(18, 18);

                            {
                                colorBox.setStroke(Color.rgb(0, 0, 0, 0.3));
                                colorBox.setStrokeWidth(1);
                                colorBox.setArcWidth(4);
                                colorBox.setArcHeight(4);
                            }

                            @Override
                            protected void updateItem(PlayerColor playerColor, boolean empty) {
                                super.updateItem(playerColor, empty);
                                if (empty || playerColor == null) {
                                    setGraphic(null);
                                    setText(null);
                                } else {
                                    colorBox.setFill(convertPlayerColorToFxColor(playerColor));
                                    setGraphic(colorBox);
                                    setText(null);
                                }
                            }
                        });
    }

    /**
     * Configure the theme combo box.
     */
    private void setupThemeComboBox() {
        boardTheme.setItems(FXCollections.observableArrayList(Theme.values()));
        boardTheme.getSelectionModel().select(Theme.RUNEHAVEN_LABYRINTH);
        boardTheme.setCellFactory(
                cb ->
                        new ListCell<Theme>() {
                            @Override
                            protected void updateItem(Theme item, boolean empty) {
                                super.updateItem(item, empty);
                                if (empty || item == null) {
                                    setText(null);
                                } else {
                                    setText(item.getDisplayName());
                                }
                            }
                        });
        boardTheme.setButtonCell(
                new ListCell<Theme>() {
                    @Override
                    protected void updateItem(Theme item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty || item == null ? null : item.getDisplayName());
                    }
                });

        themeChangeListener =
                (obs, oldVal, newVal) -> {
                    setThemeForGameBoard(newVal);
                };
        boardTheme.getSelectionModel().selectedItemProperty().addListener(themeChangeListener);
    }

    /**
     * Trigger a start game command with current settings.
     */
    private void startGame() {
        StartGameRecord gameStartRecord = gatherStartGameData();
        lobbyViewModel.startGame(gameStartRecord);
    }

    /**
     * Collect settings for starting a game.
     *
     * @return start game record
     */
    private StartGameRecord gatherStartGameData() {
        int treasureCardCount = treasureCardsSpinner.getValue();
        treasureCardCount = treasureCardCount * Integer.parseInt(connectedPlayers.getText());

        int totalBonusCount = bonusEnabledCheck.isSelected() ? bonusCountSpinner.getValue() : 0;

        int gameDurationInSeconds = minutesToSeconds(gameTimerSpinner.getValue());
        int boardSize = mapLabelToBoardSize(boardSizeCombo.getValue());

        return new StartGameRecord(
                treasureCardCount, totalBonusCount, gameDurationInSeconds, boardSize);
    }

    // Helper method to convert PlayerColor to JavaFX Color
    /**
     * Convert player colors to JavaFX colors.
     *
     * @param playerColor player color enum
     * @return JavaFX color
     */
    private Color convertPlayerColorToFxColor(PlayerColor playerColor) {
        switch (playerColor) {
            case RED:
                return Color.RED;
            case BLUE:
                return Color.BLUE;
            case GREEN:
                return Color.GREEN;
            case YELLOW:
                return Color.YELLOW;
            // Add other colors as needed
            default:
                throw new IllegalStateException("Unkown Player Color");
        }
    }

    /**
     * Map a board size to the display label.
     *
     * @param boardSize board size value
     * @return display label
     */
    private String mapBoardSizeToLabel(int boardSize) {
        return switch (boardSize) {
            case 5 -> SMALL_GAME_BOARD_SIZE;
            case 7 -> MEDIUM_GAME_BOARD_SIZE;
            case 9 -> LARGE_GAME_BOARD_SIZE;
            default -> DEFAULT_GAME_BOARD_SIZE_LABEL;
        };
    }

    /**
     * Map a board size label to the numeric size.
     *
     * @param label size label
     * @return board size
     */
    private int mapLabelToBoardSize(String label) {
        return switch (label) {
            case SMALL_GAME_BOARD_SIZE -> 5;
            case MEDIUM_GAME_BOARD_SIZE -> 7;
            case LARGE_GAME_BOARD_SIZE -> 9;
            default -> lobbyViewModel.getDefaultBoardSize();
        };
    }

    /**
     * Convert minutes to seconds.
     *
     * @param minutes minutes value
     * @return seconds value
     */
    private int minutesToSeconds(int minutes) {
        return minutes * 60;
    }

    /**
     * Convert seconds to minutes.
     *
     * @param seconds seconds value
     * @return minutes value
     */
    private int secondsToMinutes(int seconds) {
        return seconds / 60;
    }

    /**
     * Bind error handling to show alert modals.
     */
    private void setupErrorHandling() {
        ChangeListener<Boolean> errorListener =
                (obs, oldVal, newVal) -> {
                    if (Boolean.TRUE.equals(newVal)) {
                        String msg = lobbyViewModel.getErrorMessage().get();
                        JavaFXUtils.showAlertErrorModal(
                                ERROR_MODAL_TITLE, "", msg, lobbyViewModel::clearError);
                    }
                };
        lobbyViewModel.getHasError().addListener(errorListener);
    }

    /**
     * Build an achievements summary string for a ranking entry.
     *
     * @param entry ranking entry
     * @return achievements summary string
     */
    private String getAchievementResultsStringForPlayer(RankingEntry entry) {
        String id = entry.getPlayerId();
        List<AchievementUnlockedEventPayload> achievements =
                achievementService.getAchievementEventsForPLayerId(id);
        return achievements.stream()
                .map(
                        event -> {
                            AchievementType type = event.getAchievement();
                            return switch (type) {
                                case RUNNER -> "Runner (" + entry.getStats().getStepsTaken() + ")";
                                case PUSHER -> "Pusher (" + entry.getStats().getTilesPushed() + ")";
                                // Note: Assuming Pusher refers to tiles pushed, not treasures
                                default -> type.name();
                            };
                        })
                .collect(Collectors.joining(", "));
    }

    /**
     * Update the theme used for game board assets.
     *
     * @param theme selected theme
     */
    private void setThemeForGameBoard(Theme theme) {
        FileNameMapper.setTheme(theme);
    }

    public void onFullscreenToggleButtonClick(ActionEvent actionEvent) {
        navigationController.toogleFullScreen();
    }


    /*
    private Node createAchievementGraphicForPlayer(RankingEntry entry) {
        String id = entry.getPlayerId();
        List<AchievementUnlockedEventPayload> unlocked = achievementService.getAchievementEventsForPLayerId(id);

        if (unlocked == null || unlocked.isEmpty()) {
            return new Label("");
        }

        HBox container = new HBox(10);
        container.setAlignment(Pos.CENTER_LEFT);
        for (AchievementUnlockedEventPayload event : unlocked) {
            AchievementType type = event.getAchievement();

            // Badge container: Icon + Text
            HBox badge = new HBox(4);
            badge.setAlignment(Pos.CENTER_LEFT);

            Node icon = getIconForAchievementType(type);
            Label text = new Label(getLabelForAchievementType(type, entry.getStats()));

            badge.getChildren().addAll(icon, text);
            container.getChildren().add(badge);
        }
        return container;
    }


    private Node getIconForAchievementType(AchievementType type) {
        // You can replace these Shapes with new ImageView(new Image("..."))
        return switch (type) {
            case RUNNER ->  new ImageView(new Image("..."))
            case PUSHER ->  new ImageView(new Image("..."))
        };
    }

    private String getLabelForAchievementType(AchievementType type, PlayerGameStats stats) {
        return switch (type) {
            case RUNNER -> "Runner (" + stats.getStepsTaken() + ")";
            case PUSHER -> "Pusher (" + stats.getTilesPushed() + ")";
            default -> type.name();
        };
    }

     */

}