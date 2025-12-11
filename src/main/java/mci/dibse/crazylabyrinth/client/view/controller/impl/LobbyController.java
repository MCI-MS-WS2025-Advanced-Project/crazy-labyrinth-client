package mci.dibse.crazylabyrinth.client.view.controller.impl;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import labyrinth.contracts.models.PlayerColor;

import mci.dibse.crazylabyrinth.client.domain.model.record.StartGameRecord;
import mci.dibse.crazylabyrinth.client.view.viewmodel.LobbyViewModel;
import mci.dibse.crazylabyrinth.client.view.viewmodel.item.PlayerInfoItem;
import mci.dibse.crazylabyrinth.client.view.ViewCleanup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LobbyController implements ViewCleanup {


    private final String SMALL_GAME_BOARD_SIZE = "Klein (5x5)";
    private final String MEDIUM_GAME_BOARD_SIZE = "Normal (7x7)";
    private final String LARGE_GAME_BOARD_SIZE = "Groß (9x9)";

    private final String DEFAULT_GAME_BOARD_SIZE_LABEL = MEDIUM_GAME_BOARD_SIZE;


    private LobbyViewModel lobbyViewModel;

    @FXML public TableView<PlayerInfoItem> playerTable;
    @FXML public TableColumn<PlayerInfoItem, String> playerNameColumn;
    @FXML public TableColumn<PlayerInfoItem, String> colorColumn;
    @FXML public TableColumn<PlayerInfoItem, Boolean> adminColumn;
    @FXML public VBox adminSettings;
    @FXML public Label gameNameLabel;
    @FXML public Label maxPlayers;
    @FXML public Label connectedPlayers;
    @FXML public Spinner<Integer> gameTimerSpinner;
    @FXML public CheckBox timerEnabledCheck;
    @FXML public HBox timerSettings;
    
    //Admin Stuff
    @FXML public CheckBox bonusEnabledCheck;
    @FXML public ComboBox<String> boardSizeCombo;
    @FXML public Spinner<Integer> treasureCardsSpinner;


    //TODO where to handle countdown?
    @FXML public Label countdown;



    public BooleanProperty clientAdmin = new SimpleBooleanProperty(false);
    public BooleanProperty timerEnabled = new SimpleBooleanProperty(false);

    @FXML private ListView<String> playerList;
    @FXML private Button startGameButton;


    @Autowired
    public LobbyController(LobbyViewModel lobbyViewModel) {
        this.lobbyViewModel = lobbyViewModel;
    }


    @FXML
    public void initialize() {
        System.out.println("LobbyController initialized");
        playerTable.setItems((ObservableList<PlayerInfoItem>) lobbyViewModel.getPlayers());
        setUpTableColumns();
        startGameButton.setDisable(false);

        clientAdmin.set(lobbyViewModel.isClientAdmin());
        adminSettings.visibleProperty().bind(clientAdmin);
       // adminSettings.managedProperty().bind(clientAdmin);

        maxPlayers.textProperty().bind(lobbyViewModel.getMaxPlayersProperty());
        connectedPlayers.textProperty().bind(lobbyViewModel.getPlayerCountProperty());

        gameNameLabel.textProperty().bind(lobbyViewModel.getGameNameProperty());

        boardSizeCombo.setValue(mapBoardSizeToLabel(lobbyViewModel.getDefaultBoardSize()));

        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(2, 6, 6);
        treasureCardsSpinner.setValueFactory(valueFactory);

        //TODO min max??
        timerEnabledCheck.selectedProperty().set(lobbyViewModel.isDefaultTimerEnabled());
        timerSettings.visibleProperty().bind(timerEnabledCheck.selectedProperty());

        int defaultTimerInMinutes = secondsToMinutes(lobbyViewModel.getDefaultTimer());
        gameTimerSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 120, defaultTimerInMinutes));
    }

    @Override
    public void cleanup() {
        // 1. Clear property bindings
        // 2. Clear event handlers
        // 3. Clear table cell factories (breaks reference chains)
        // 4. Clear table data
        // 5. Clear combo box items
        maxPlayers.textProperty().unbind();
        connectedPlayers.textProperty().unbind();
        gameNameLabel.textProperty().unbind();
        adminSettings.visibleProperty().unbind();
        timerSettings.visibleProperty().unbind();

        startGameButton.setOnAction(null);
        timerEnabledCheck.setOnAction(null);


        playerNameColumn.setCellValueFactory(null);
        colorColumn.setCellFactory(null);
        adminColumn.setCellValueFactory(null);

        playerTable.getItems().clear();

        boardSizeCombo.getItems().clear();
    }



    /*
        int treasureCardCount,
        int totalBonusCount,
        int gameDurationInSeconds,
        int boardSiz
     */
    @FXML
    public void onStartButtonClick() {
        startGameButton.setDisable(true);
        int treasureCardCount = treasureCardsSpinner.getValue();
        treasureCardCount = treasureCardCount * Integer.parseInt(connectedPlayers.getText());
        //TODO impl
        int totalBonusCount = bonusEnabledCheck.isSelected() ? 1 : 0;
        //TODO rework after API specs & Lastenheft
        int gameDurationInSeconds = minutesToSeconds(gameTimerSpinner.getValue());
        int boardSize = mapLabelToBoardSize(boardSizeCombo.getValue());

        StartGameRecord gameStartRecord = new StartGameRecord(treasureCardCount, totalBonusCount, gameDurationInSeconds, boardSize);
        lobbyViewModel.startGame(gameStartRecord);
        //TODO counter
    }
    //todo auto start after 2 players joined

    @FXML
    public void onleaveLobbyButtonClick() {
        cleanup();
        lobbyViewModel.leaveLobby();
    }

    private void setUpTableColumns(){
        playerNameColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        colorColumn.setCellValueFactory(cellData -> cellData.getValue().colorProperty().asString());
        adminColumn.setCellValueFactory(cellData -> cellData.getValue().isAdminProperty());


        // Add custom cell factory to display the text in the actual color
        colorColumn.setCellFactory(column -> new TableCell<PlayerInfoItem, String>() {
            @Override
            protected void updateItem(String colorString, boolean empty) {
                super.updateItem(colorString, empty);

                if (empty || colorString == null) {
                    setText(null);
                    setTextFill(null);
                } else {
                    setText(colorString);

                    // Get the actual PlayerColor object from the row item
                    PlayerInfoItem playerInfoItem = getTableRow().getItem();
                    if (playerInfoItem != null && playerInfoItem.colorProperty().get() != null) {
                        PlayerColor playerColor = playerInfoItem.colorProperty().get();

                        // Set text color to match the actual color
                        Color fxColor = convertPlayerColorToFxColor(playerColor);
                        setTextFill(fxColor);
                    }
                }
            }
        });

    }
    // Helper method to convert PlayerColor to JavaFX Color
    private Color convertPlayerColorToFxColor(PlayerColor playerColor) {
        // You'll need to implement this based on your PlayerColor enum
        switch (playerColor) {
            case RED: return Color.RED;
            case BLUE: return Color.BLUE;
            case GREEN: return Color.GREEN;
            case YELLOW: return Color.YELLOW;
            // Add other colors as needed
            default: return Color.BLACK;
        }
    }

    //TODO
    private String mapBoardSizeToLabel(int boardSize){
        return switch (boardSize) {
            case 5 -> SMALL_GAME_BOARD_SIZE;
            case 7 -> MEDIUM_GAME_BOARD_SIZE;
            case 9 -> LARGE_GAME_BOARD_SIZE;
            default -> DEFAULT_GAME_BOARD_SIZE_LABEL;
        };
    }
    private int mapLabelToBoardSize(String label){
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
}
