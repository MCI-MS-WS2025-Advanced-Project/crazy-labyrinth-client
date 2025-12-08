package mci.dibse.crazylabyrinth.client.view.controller.impl;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import labyrinth.contracts.models.PlayerColor;
import lombok.Getter;
import lombok.Setter;
import mci.dibse.crazylabyrinth.client.view.viewmodel.LobbyViewModel;
import mci.dibse.crazylabyrinth.client.view.viewmodel.item.PlayerItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LobbyController {


    private LobbyViewModel lobbyViewModel;

    @FXML public TableView<PlayerItem> playerTable;
    @FXML public TableColumn<PlayerItem, String> playerNameColumn;
    @FXML public TableColumn<PlayerItem, String> colorColumn;
    @FXML public TableColumn<PlayerItem, Boolean> adminColumn;
    @FXML public VBox adminSettings;
    @FXML public Label gameNameLabel;
    @FXML public Label maxPlayers;
    @FXML public Label connectedPlayers;
    
    //Admin Stuff
    @FXML public CheckBox bonusEnabledCheck;
    @FXML public ComboBox<Integer> boardSizeCombo = new ComboBox<>(FXCollections.observableArrayList(5, 7, 9));


    //TODO where to handle countdown?
    @FXML public Label countdown;



    public BooleanProperty clientAdmin = new SimpleBooleanProperty(false);

    @FXML
    private ListView<String> playerList;
    @FXML
    private Button startGameButton;


    @Autowired
    public LobbyController(LobbyViewModel lobbyViewModel) {
        this.lobbyViewModel = lobbyViewModel;
    }


    @FXML
    public void initialize() {
        System.out.println("LobbyController initialized");
        playerTable.setItems((ObservableList<PlayerItem>) lobbyViewModel.getPlayers());
        setUpTableColumns();
        startGameButton.setDisable(false);


        clientAdmin.set(lobbyViewModel.isClientAdmin());
        adminSettings.visibleProperty().bind(clientAdmin);
       // adminSettings.managedProperty().bind(clientAdmin);

        maxPlayers.textProperty().bind(lobbyViewModel.getMaxPlayersProperty());
        connectedPlayers.textProperty().bind(lobbyViewModel.getPlayerCountProperty());

        gameNameLabel.textProperty().bind(lobbyViewModel.getGameNameProperty());
        boardSizeCombo.setValue(7);
        

    }


    @FXML
    private void onStartButtonClick() {
        startGameButton.setDisable(true);
        lobbyViewModel.startGame();
    }

    private void setUpTableColumns(){
        playerNameColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        colorColumn.setCellValueFactory(cellData -> cellData.getValue().colorProperty().asString());
        adminColumn.setCellValueFactory(cellData -> cellData.getValue().isAdminProperty());


        // Add custom cell factory to display the text in the actual color
        colorColumn.setCellFactory(column -> new TableCell<PlayerItem, String>() {
            @Override
            protected void updateItem(String colorString, boolean empty) {
                super.updateItem(colorString, empty);

                if (empty || colorString == null) {
                    setText(null);
                    setTextFill(null);
                } else {
                    setText(colorString);

                    // Get the actual PlayerColor object from the row item
                    PlayerItem playerItem = getTableRow().getItem();
                    if (playerItem != null && playerItem.colorProperty().get() != null) {
                        PlayerColor playerColor = playerItem.colorProperty().get();

                        // Set text color to match the actual color
                        javafx.scene.paint.Color fxColor = convertPlayerColorToFxColor(playerColor);
                        setTextFill(fxColor);
                    }
                }
            }
        });

    }
    // Helper method to convert PlayerColor to JavaFX Color
    private javafx.scene.paint.Color convertPlayerColorToFxColor(PlayerColor playerColor) {
        // You'll need to implement this based on your PlayerColor enum
        switch (playerColor) {
            case RED: return javafx.scene.paint.Color.RED;
            case BLUE: return javafx.scene.paint.Color.BLUE;
            case GREEN: return javafx.scene.paint.Color.GREEN;
            case YELLOW: return javafx.scene.paint.Color.YELLOW;
            // Add other colors as needed
            default: return javafx.scene.paint.Color.BLACK;
        }
    }


    
}
