package mci.dibse.crazylabyrinth.client.view.controller.impl;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import labyrinth.contracts.models.PlayerColor;
import mci.dibse.crazylabyrinth.client.domain.model.record.StartGameRecord;
import mci.dibse.crazylabyrinth.client.infrastructure.util.JavaFXUtils;
import mci.dibse.crazylabyrinth.client.view.viewmodel.LobbyViewModel;
import mci.dibse.crazylabyrinth.client.view.viewmodel.item.PlayerInfoItem;
import mci.dibse.crazylabyrinth.client.view.viewmodel.navigation.NavigationStateViewModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LobbyController extends BaseViewCleanup {

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
    @FXML private ListView<String> playerList;

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

    private Runnable cleanUpTask;

    @Autowired
    public LobbyController(NavigationStateViewModel navState, LobbyViewModel lobbyViewModel) {
        super(navState);
        this.lobbyViewModel = lobbyViewModel;
    }

    @FXML
    public void initialize() {
        playerTable.setItems(lobbyViewModel.getPlayersListProperty());

        // Clip/cleanup
        cleanUpTask = JavaFXUtils.setupTableClip(playerTable, 30);

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

                    playerTable.getItems().clear();

                    boardSizeCombo.getItems().clear();

                    countdownTimeline = null;
                });
        cleanUpTask.run();
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
        Timeline timeline = new Timeline();

        timeline.getKeyFrames().add(
                new KeyFrame(
                        Duration.seconds(1),
                        e -> {
                            remainingTimer.set(remainingTimer.get() - 1);
                            if (remainingTimer.get() <= 0) {
                                timeline.stop();
                                if (lobbyViewModel.isClientAdmin()) startGame();
                            }
                        }));

        timeline.setCycleCount(
                Animation.INDEFINITE);
        this.countdownTimeline = timeline;
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
                        JavaFXUtils.showAlertModal(
                                ERROR_MODAL_TITLE, "", msg, lobbyViewModel::clearError);
                    }
                };
        lobbyViewModel.getHasError().addListener(errorListener);
    }
}
