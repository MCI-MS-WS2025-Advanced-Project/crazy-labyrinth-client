package mci.dibse.crazylabyrinth.client.view.controller.impl;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;
import javafx.util.Duration;
import labyrinth.contracts.models.*;
import lombok.extern.slf4j.Slf4j;
import labyrinth.contracts.models.RankingEntry;
import mci.dibse.crazylabyrinth.client.domain.model.record.StartGameRecord;
import mci.dibse.crazylabyrinth.client.infrastructure.util.JavaFXUtils;
import mci.dibse.crazylabyrinth.client.infrastructure.util.ScreenResolutionProperties;
import mci.dibse.crazylabyrinth.client.service.AchievementService;
import mci.dibse.crazylabyrinth.client.view.viewmodel.GameViewModel;
import mci.dibse.crazylabyrinth.client.view.viewmodel.LobbyViewModel;
import mci.dibse.crazylabyrinth.client.view.viewmodel.item.PlayerInfoItem;
import mci.dibse.crazylabyrinth.client.view.viewmodel.navigation.NavigationStateViewModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class LobbyController extends BaseViewCleanup {

    private final GameViewModel gameViewModel;
    private final AchievementService achievementService;
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

    @FXML public StackPane viewport;
    @FXML public Rectangle rootClip;
    @FXML public Group contentGroup;
    @FXML public BorderPane root;

    // Admin Stuff
    @FXML public CheckBox bonusEnabledCheck;
    @FXML public ComboBox<String> boardSizeCombo;
    @FXML public Spinner<Integer> treasureCardsSpinner;
    @FXML private Button startGameButton;

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

    @Autowired
    public LobbyController(NavigationStateViewModel navState, LobbyViewModel lobbyViewModel, ScreenResolutionProperties screenResolutionProperties, GameViewModel gameViewModel, AchievementService achievementService) {
        super(navState);
        this.lobbyViewModel = lobbyViewModel;
        this.screenResolutionProperties = screenResolutionProperties;
        this.gameViewModel = gameViewModel;
        this.achievementService = achievementService;
    }

    @FXML
    public void initialize() {


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
        ListChangeListener<PlayerInfoItem> listener = (change) -> {
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

        initUiScaling();
    }

    private void initUiScaling() {
        ScreenResolutionProperties.ResolutionProperties props = new ScreenResolutionProperties.ResolutionProperties(root,
                viewport);
        screenResolutionProperties.initUiScaling(props);

    }

    private void setupResultsTable() {
        log.info("setupResultsTable");
        // Clip/cleanup


        ChangeListener<Boolean> resultsListener = (obs, oldVal, newVal) -> {
            Runnable task = () -> {
                ObservableList<RankingEntry> rankingEntriesSorted = lobbyViewModel.getSortedRankingEntries();
                resultsTable.setItems(rankingEntriesSorted);
                lobbyViewModel.bindSortedRankingsComparator(resultsTable.comparatorProperty());




                int numberEntries = rankingEntriesSorted.size();
                // numberEntries
                cleanUpTaskResultsTable = JavaFXUtils.setupTableClip(resultsTable, 30.0);

                resultNameColumn.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getPlayerId()));
                resultNameColumn.setCellFactory(
                        column -> {
                            return new TableCell<RankingEntry, String>() {
                                @Override
                                protected void updateItem(String item, boolean empty) {
                                    super.updateItem(item, empty);
                                    if (empty) {
                                        setText(null);
                                        return;
                                    }
                                    String name = lobbyViewModel.getPlayerNameForId(item);
                                    if (lobbyViewModel.getWinnerId().equals(item)) name += " (Gewinner)";
                                    setText(name);
                                }
                            };
                        }
                );

                resultScoreColumn.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getScore().toString()));

                achievements.setCellValueFactory(cd -> new SimpleObjectProperty<RankingEntry>(cd.getValue()));
                achievements.setCellFactory(
                        column -> {
                            return new TableCell<RankingEntry, RankingEntry>() {
                                @Override
                                protected void updateItem(RankingEntry item, boolean empty) {
                                    super.updateItem(item, empty);
                                    if (empty) {
                                        setText(null);
                                        return;
                                    }

                                    //  setGraphic(createAchievementGraphicForPlayer(item));
                                    String achievements = getAchievementResultsStringForPlayer(item);
                                    setText(achievements);
                                }
                            };
                        }
                );

                resultsBox.setVisible(true);
                resultsBox.setManaged(true);


            };
            JavaFXUtils.runOnFxThread(task);
        };

        lobbyViewModel.addListenerToResultsAvailableProperty(resultsListener);

    }

    public void onleaveLobbyButtonClick(ActionEvent actionEvent) {
        lobbyViewModel.leaveLobby();
    }

    @FXML
    public void onStartButtonClick() {
        isStartButtonLocked.set(true);
        PauseTransition pause = new PauseTransition(Duration.seconds(3));
        pause.setOnFinished(e -> isStartButtonLocked.set(false));
        pause.play();
        cancelCountdown();

        startGame();
    }

    @Override
    protected boolean customCleanupCheckpoint(
            NavigationStateViewModel.ScreenId oldValue,
            NavigationStateViewModel.ScreenId newValue) {
        return newValue != null
                && NavigationStateViewModel.ScreenId.LOBBY.equals(oldValue)
                && !NavigationStateViewModel.ScreenId.LOBBY.equals(newValue);
    }

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
                });
        cleanUpTaskPlayerTable.run();
        if (cleanUpTaskResultsTable != null)  cleanUpTaskResultsTable.run();
    }

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

    private void cancelCountdown() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }
        remainingTimer.set(DEFAULT_START_GAME_TIMER);
    }

    private void startCountdownIfNeeded() {
        if (countdownTimeline == null) {
            initCountdownTimeline();
        }
        if (countdownTimeline.getStatus() != Animation.Status.RUNNING) {
            remainingTimer.set(DEFAULT_START_GAME_TIMER);
            countdownTimeline.playFromStart();
        }
    }

    private void setUpTableColumns() {
        playerNameColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        colorColumn.setCellValueFactory(cellData -> cellData.getValue().colorProperty());
        adminColumn.setCellValueFactory(cellData -> cellData.getValue().isAdminProperty());

        // Add custom cell factory to display the text in the actual color
        colorColumn.setCellFactory(
                column ->
                        new TableCell<PlayerInfoItem, PlayerColor>() {

                            @Override
                            protected void updateItem(PlayerColor playerColor, boolean empty) {
                                super.updateItem(playerColor, empty);
                                if (empty) {
                                    setText(null);
                                    setTextFill(Color.BLACK);
                                    return;
                                }

                                setText(playerColor != null ? playerColor.toString() : "");
                                setTextFill(
                                        playerColor != null
                                                ? convertPlayerColorToFxColor(playerColor)
                                                : Color.BLACK);
                            }
                        });
    }

    private void startGame() {
        StartGameRecord gameStartRecord = gatherStartGameData();
        lobbyViewModel.startGame(gameStartRecord);
    }

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

    private String mapBoardSizeToLabel(int boardSize) {
        return switch (boardSize) {
            case 5 -> SMALL_GAME_BOARD_SIZE;
            case 7 -> MEDIUM_GAME_BOARD_SIZE;
            case 9 -> LARGE_GAME_BOARD_SIZE;
            default -> DEFAULT_GAME_BOARD_SIZE_LABEL;
        };
    }

    private int mapLabelToBoardSize(String label) {
        return switch (label) {
            case SMALL_GAME_BOARD_SIZE -> 5;
            case MEDIUM_GAME_BOARD_SIZE -> 7;
            case LARGE_GAME_BOARD_SIZE -> 9;
            default -> lobbyViewModel.getDefaultBoardSize();
        };
    }

    private int minutesToSeconds(int minutes) {
        return minutes * 60;
    }

    private int secondsToMinutes(int seconds) {
        return seconds / 60;
    }

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


    private String getAchievementResultsStringForPlayer(RankingEntry entry) {
        String id = entry.getPlayerId();
        List<AchievementUnlockedEventPayload> achievements = achievementService.getAchievementEventsForPLayerId(id);
        return achievements.stream()
                .map(event -> {
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
