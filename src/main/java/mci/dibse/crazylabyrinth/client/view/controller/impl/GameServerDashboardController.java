package mci.dibse.crazylabyrinth.client.view.controller.impl;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import mci.dibse.crazylabyrinth.client.domain.model.enums.GameStatus;
import mci.dibse.crazylabyrinth.client.infrastructure.util.JavaFXUtils;
import mci.dibse.crazylabyrinth.client.view.viewmodel.GameServerDashboardViewModel;
import mci.dibse.crazylabyrinth.client.view.viewmodel.ViewCleanup;
import mci.dibse.crazylabyrinth.client.view.viewmodel.item.GameServerItem;
import mci.dibse.crazylabyrinth.client.view.viewmodel.navigation.NavigationStateViewModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Controller for the game server dashboard view that manages the display and interaction
 * with available game servers. This controller handles server list refresh, filtering,
 * display of server status, and server join functionality through a table-based interface.
 * It extends BaseViewCleanup to properly manage view resources during navigation.
 */
@Component
public class GameServerDashboardController extends BaseViewCleanup {


    private static final String SERVER_ERROR_LOADING_STATUS           = "Fehler beim Laden des Servers...";
    private static final String NO_SERVERS_FOUND_STATUS               = "Keine Server gefunden. Klicken Sie auf 'Aktualisieren', um das Verzeichnis abzufragen.";
    private static final String GENERAL_SERVER_ERROR_STATUS           = "Etwas ist schiefgelaufen. Bitte versuchen Sie es später noch einmal.";



    private GameServerDashboardViewModel viewModel;

    @FXML public Label          statusLabel;
    @FXML public Button         refreshButton;
    @FXML public CheckBox       showOnlyJoinable;
    @FXML public Label          serverCountLabel;

    @FXML public TableView<GameServerItem>              serversTable;
    @FXML public TableColumn<GameServerItem, String>    nameCol;
    @FXML public TableColumn<GameServerItem, String>    statusCol;
    @FXML public TableColumn<GameServerItem, String>    currentPlayersCol;
    @FXML public TableColumn<GameServerItem, String>    lastSeenCol;
    @FXML public TableColumn<GameServerItem, String>    actionCol;


    @Autowired
    public GameServerDashboardController(GameServerDashboardViewModel vm, NavigationStateViewModel navState) {
        super(navState);
        this.viewModel = vm;
    }


    @FXML
    public void initialize() {
        setupTableData();
        setupTableColumns();
        setupTableRowFactory();
        setupControls();
        setupErrorHandling();
        viewModel.refreshServerList();
    }


    //todo test with impl of issue #6
    @Override
    public void cleanup() {
        statusLabel.textProperty().unbind();
        serverCountLabel.textProperty().unbind();

        refreshButton.setOnAction(null);

        showOnlyJoinable.selectedProperty().unbindBidirectional(viewModel.onlyJoinableProperty());

        serversTable.setItems(null);
        serversTable.setRowFactory(null);

        nameCol.setCellValueFactory(null);
        statusCol.setCellValueFactory(null);
        currentPlayersCol.setCellValueFactory(null);

        lastSeenCol.setCellValueFactory(null);
        lastSeenCol.setCellFactory(null);

        actionCol.setCellFactory(null);

        viewModel.getSorted().comparatorProperty().unbind();
    }

    @Override
    protected boolean customCleanupCheckpoint(NavigationStateViewModel.ScreenId oldValue, NavigationStateViewModel.ScreenId newValue) {
        return oldValue != null && !newValue.equals(NavigationStateViewModel.ScreenId.MAIN);
    }


    private void setupTableData() {
        serversTable.setItems(viewModel.getSorted());
        viewModel.getSorted().comparatorProperty().bind(serversTable.comparatorProperty());
    }
    private void setupTableColumns() {
        nameCol.setCellValueFactory(cd -> cd.getValue().nameProperty());
        statusCol.setCellValueFactory(cd -> cd.getValue().statusProperty().asString());

        currentPlayersCol.setCellValueFactory(this::createPlayerCountValue);
        lastSeenCol.setCellValueFactory(cd -> cd.getValue().lastSeenProperty().asString());
        lastSeenCol.setCellFactory(col -> createLastSeenCell());
        actionCol.setCellFactory(col -> createActionCell());
    }

    private ObservableValue<String> createPlayerCountValue(TableColumn.CellDataFeatures<GameServerItem, String> cd) {
        var item = cd.getValue();
        return Bindings.format("%d / %d", item.currentPlayerCountProperty(), item.maxPlayersProperty());
    }

    private TableCell<GameServerItem, String> createLastSeenCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String iso, boolean empty) {
                super.updateItem(iso, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    return;
                }
                GameServerItem rowItem = getTableRow().getItem();
                Instant instant = rowItem.getLastSeen().getValue();
                setText(instant == null ? "" : JavaFXUtils.getLAST_SEEN_FORMATTER().format(instant));
            }
        };
    }

    private TableCell<GameServerItem, String> createActionCell() {
        return new TableCell<>() {
            private final Button joinBtn = new Button("Join");

            {
                joinBtn.setMinWidth(70);
                joinBtn.setOnAction(e -> {
                    GameServerItem item = getTableRow() == null ? null : getTableRow().getItem();
                    joinServer(item);
                });
            }

            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                GameServerItem item = getTableRow().getItem();
                joinBtn.setDisable(!isServerJoinable(item));
                setGraphic(joinBtn);
            }
        };
    }

    private void setupTableRowFactory() {
        serversTable.setRowFactory(tv -> {
            TableRow<GameServerItem> row = new TableRow<>();
            MenuItem rowJoinItem = new MenuItem("Join");
            rowJoinItem.setOnAction(e -> joinServer(row.getItem()));

            ContextMenu rowMenu = new ContextMenu(rowJoinItem);

            // Only show context menu on non-empty rows
            row.contextMenuProperty().bind(
                    Bindings.when(row.emptyProperty())
                            .then((ContextMenu) null)
                            .otherwise(rowMenu)
            );
            return row;
        });
    }

    private void setupControls() {
        showOnlyJoinable.selectedProperty().bindBidirectional(viewModel.onlyJoinableProperty());
        serverCountLabel.textProperty().bind(Bindings.size(viewModel.getSorted()).asString());

        refreshButton.setOnAction(e -> viewModel.refreshServerList());

        statusLabel.textProperty().bind(Bindings.createStringBinding(
                this::calculateStatusText,
                viewModel.getHasError(),
                viewModel.getErrorMessage(),
                viewModel.getIsRefreshing()
        ));
    }

    private String calculateStatusText() {
        if (viewModel.getHasError().get()) {
            return viewModel.getErrorMessage().get();
        } else if (viewModel.getIsRefreshing().get()) {
            return SERVER_ERROR_LOADING_STATUS;
        } else {
            return NO_SERVERS_FOUND_STATUS;
        }
    }

    private void setupErrorHandling() {
        viewModel.getHasError().addListener((obs, oldVal, newVal) -> {
            if (Boolean.TRUE.equals(newVal)) {
                showAlertModal(
                        SERVER_ERROR_LOADING_STATUS,
                        "",
                        GENERAL_SERVER_ERROR_STATUS
                );
            }
        });
    }

    private void joinServer(GameServerItem item) {
        if (item != null) {
            // TODO: call your actual join method here --> issue# 6
        }
    }

    private boolean isServerJoinable(GameServerItem item) {
        if (item == null) return false;
        boolean isLobby = item.getStatus().getValue() == GameStatus.LOBBY;
        boolean hasSpace = item.getCurrentPlayerCount().getValue() < item.getMaxPlayers().getValue();
        return isLobby && hasSpace;
    }

    private void showAlertModal(String title, String header, String content) {
        JavaFXUtils.runOnFxThread(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(header);
            alert.setContentText(content);
            alert.show();
        });
    }



}

