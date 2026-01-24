package mci.dibse.crazylabyrinth.client.view.controller.impl;

import static mci.dibse.crazylabyrinth.client.infrastructure.util.JavaFXUtils.runOnFxThread;

import java.time.Instant;
import java.util.Optional;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import mci.dibse.crazylabyrinth.client.NavigationController;
import mci.dibse.crazylabyrinth.client.domain.model.record.GameInfoRecord;
import mci.dibse.crazylabyrinth.client.infrastructure.util.JavaFXUtils;
import mci.dibse.crazylabyrinth.client.infrastructure.util.ScreenResolutionProperties;
import mci.dibse.crazylabyrinth.client.service.PlayerConnectedAckListener;
import mci.dibse.crazylabyrinth.client.view.viewmodel.GameServerDashboardViewModel;
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

    private final NavigationController navigationController;
    @FXML public Label statusLabel;
    @FXML public Button refreshButton;
    @FXML public CheckBox showOnlyJoinable;
    @FXML public Label serverCountLabel;

    @FXML public TableView<GameServerItem> serversTable;
    @FXML public TableColumn<GameServerItem, String> nameCol;
    @FXML public TableColumn<GameServerItem, String> statusCol;
    @FXML public TableColumn<GameServerItem, String> currentPlayersCol;
    @FXML public TableColumn<GameServerItem, String> lastSeenCol;
    @FXML public TableColumn<GameServerItem, String> actionCol;


    @FXML public StackPane viewport;
    @FXML public Rectangle rootClip;
    @FXML public Group contentGroup;
    @FXML public BorderPane root;




    private static final String SERVER_ERROR_LOADING_STATUS = "Fehler beim Laden des Servers...";
    private static final String SERVER_ERROR_TITLE          = "Fehler";
    private static final String NO_SERVERS_FOUND_STATUS =
            "Keine Server gefunden. Klicken Sie auf 'Aktualisieren', um das Verzeichnis"
                    + " abzufragen.";
    private static final String GENERAL_SERVER_ERROR_STATUS =
            "Etwas ist schiefgelaufen. Bitte versuchen Sie es später noch einmal.";



    private GameServerDashboardViewModel viewModel;
    private Runnable cleanUpTask;

    private ScreenResolutionProperties screenResolutionProperties;

    @Autowired
    public GameServerDashboardController(
            GameServerDashboardViewModel vm, NavigationStateViewModel navState, ScreenResolutionProperties screenResolutionProperties, NavigationController navigationController) {
        super(navState);
        this.viewModel = vm;
        this.screenResolutionProperties = screenResolutionProperties;
        this.navigationController = navigationController;
    }

    /**
     * Initialize table configuration, bindings, and listeners.
     */
    @FXML
    public void initialize() {
        setupTableData();
        setupTableColumns();
        setupTableRowFactory();
        setupControls();
        setupErrorHandling();
        viewModel.refreshServerList();
        viewport.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                Platform.runLater(() -> {
                    root.applyCss();
                    root.layout();
                    initUiScaling();
                });
            }
        });
    }

    /**
     * Initialize UI scaling based on screen resolution.
     */
    private void initUiScaling() {
        ScreenResolutionProperties.ResolutionProperties props = new ScreenResolutionProperties.ResolutionProperties(root,
                viewport);
        screenResolutionProperties.initUiScaling(props);
    }

    // todo test with impl of issue #6
    /**
     * Clean up bindings and table state on view exit.
     */
    @Override
    public void cleanup() {
        cleanUpTask.run();

        Runnable cleanup =
                () -> {
                    statusLabel.textProperty().unbind();
                    serverCountLabel.textProperty().unbind();

                    refreshButton.setOnAction(null);

                    showOnlyJoinable
                            .selectedProperty()
                            .unbindBidirectional(viewModel.onlyJoinableProperty());

                    serversTable.setItems(null);
                    serversTable.setRowFactory(null);

                    nameCol.setCellValueFactory(null);
                    statusCol.setCellValueFactory(null);
                    currentPlayersCol.setCellValueFactory(null);

                    lastSeenCol.setCellValueFactory(null);
                    lastSeenCol.setCellFactory(null);

                    actionCol.setCellFactory(null);

                    viewModel.getSorted().comparatorProperty().unbind();
                };
        runOnFxThread(cleanup);
    }

    /**
     * Exit the application after user confirmation.
     *
     * @param actionEvent button event
     */
    public void onExitButtonClick(ActionEvent actionEvent) {
        JavaFXUtils.showConfirmationModal(
                "Beenden", "Wollen Sie das Programm beenden?", "", Platform::exit);

    }

    public void onFullscreenToggleButtonClick(ActionEvent actionEvent) {
        navigationController.toogleFullScreen();
    }

    /**
     * Decide whether cleanup should run when leaving the main screen.
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
                && NavigationStateViewModel.ScreenId.MAIN.equals(oldValue)
                && !NavigationStateViewModel.ScreenId.MAIN.equals(newValue);
    }

    /**
     * Bind the table data to the view model.
     */
    private void setupTableData() {
        serversTable.setItems(viewModel.getSorted());
        viewModel.getSorted().comparatorProperty().bind(serversTable.comparatorProperty());

        serversTable.setFixedCellSize(34);
        cleanUpTask = JavaFXUtils.setupTableClip(serversTable, 34.0);
    }

    /**
     * Configure table columns and cell factories.
     */
    private void setupTableColumns() {
        nameCol.setCellValueFactory(cd -> cd.getValue().nameProperty());
        statusCol.setCellValueFactory(cd -> cd.getValue().statusProperty().asString());

        currentPlayersCol.setCellValueFactory(this::createPlayerCountValue);
        lastSeenCol.setCellValueFactory(cd -> cd.getValue().lastSeenProperty().asString());
        lastSeenCol.setCellFactory(col -> createLastSeenCell());
        actionCol.setCellFactory(col -> createActionCell());
    }

    /**
     * Format player count values for the table.
     *
     * @param cd cell data features
     * @return formatted player count value
     */
    private ObservableValue<String> createPlayerCountValue(
            TableColumn.CellDataFeatures<GameServerItem, String> cd) {
        var item = cd.getValue();
        return Bindings.format(
                "%d / %d", item.currentPlayerCountProperty(), item.maxPlayersProperty());
    }

    /**
     * Create a table cell that formats the last seen timestamp.
     *
     * @return configured table cell
     */
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
                setText(
                        instant == null
                                ? ""
                                : JavaFXUtils.getLAST_SEEN_FORMATTER().format(instant));
            }
        };
    }

    /**
     * Create a table cell with the join action button.
     *
     * @return configured table cell
     */
    private TableCell<GameServerItem, String> createActionCell() {
        return new TableCell<>() {
            private final Button joinBtn = new Button("Join");

            {
                joinBtn.setMinWidth(70);
                joinBtn.setPrefHeight(28);
                joinBtn.setMinHeight(28);
                joinBtn.getStyleClass().add("action-button");
                joinBtn.setOnAction(
                        e -> {
                            GameServerItem item =
                                    getTableRow() == null ? null : getTableRow().getItem();
                            showPLayerNameInputModalAndJoinServer(item);
                        });
            }

            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    joinBtn.disableProperty().unbind();
                    return;
                }
                GameServerItem item = getTableRow().getItem();
                joinBtn.disableProperty().unbind();
                joinBtn.disableProperty().bind(item.getJoinable().not());
                setGraphic(joinBtn);
            }
        };
    }

    /**
     * Configure row context menus for the servers table.
     */
    private void setupTableRowFactory() {
        serversTable.setRowFactory(
                tv -> {
                    TableRow<GameServerItem> row = new TableRow<>();
                    MenuItem rowJoinItem = new MenuItem("Join");
                    rowJoinItem.setOnAction(
                            e -> showPLayerNameInputModalAndJoinServer(row.getItem()));

                    ContextMenu rowMenu = new ContextMenu(rowJoinItem);

                    // Only show context menu on non-empty rows
                    row.contextMenuProperty()
                            .bind(
                                    Bindings.when(row.emptyProperty())
                                            .then((ContextMenu) null)
                                            .otherwise(rowMenu));
                    return row;
                });
    }

    /**
     * Bind UI controls to view model properties.
     */
    private void setupControls() {
        showOnlyJoinable.selectedProperty().bindBidirectional(viewModel.onlyJoinableProperty());
        serverCountLabel.textProperty().bind(Bindings.size(viewModel.getSorted()).asString());

        refreshButton.setOnAction(e -> viewModel.refreshServerList());

        statusLabel
                .textProperty()
                .bind(
                        Bindings.createStringBinding(
                                this::calculateStatusText,
                                viewModel.getHasError(),
                                viewModel.getErrorMessage(),
                                viewModel.getIsRefreshing()));
    }

    /**
     * Determine the status text to show in the UI.
     *
     * @return status text
     */
    private String calculateStatusText() {
        if (viewModel.getHasError().get()) {
            return viewModel.getErrorMessage().get();
        } else if (viewModel.getIsRefreshing().get()) {
            return SERVER_ERROR_LOADING_STATUS;
        } else {
            return NO_SERVERS_FOUND_STATUS;
        }
    }

    /**
     * Display error modals when errors are reported.
     */
    private void setupErrorHandling() {
        viewModel
                .getHasError()
                .addListener(
                        (obs, oldVal, newVal) -> {
                            if (Boolean.TRUE.equals(newVal)) {
                                JavaFXUtils.showAlertErrorModal(
                                        SERVER_ERROR_TITLE,
                                        "",
                                        viewModel.getErrorMessage().get(),
                                        () -> viewModel.clearError());

                            }
                        });
    }

    /**
     * Prompt for player name and join the selected server.
     *
     * @param serverItem selected server item
     */
    private void showPLayerNameInputModalAndJoinServer(GameServerItem serverItem) {
        Runnable dialogTask =
                () -> {
                    TextInputDialog dialog = new TextInputDialog();
                    dialog.setTitle("Spielserver beitreten");
                    dialog.setHeaderText("Beitreten: " + serverItem.getName().getValue());
                    dialog.setContentText("Geben sie einen Namen ein.");
                    dialog.setGraphic(null);

                    dialog.getEditor().requestFocus();

                    Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
                    okButton.disableProperty()
                            .bind(
                                    dialog.getEditor()
                                            .textProperty()
                                            .isEqualTo("")
                                            .or(
                                                    Bindings.createBooleanBinding(
                                                            () ->
                                                                    dialog.getEditor()
                                                                            .getText()
                                                                            .trim()
                                                                            .isEmpty(),
                                                            dialog.getEditor().textProperty())));

                    Optional<String> result = dialog.showAndWait();
                    result.ifPresent(
                            playerName -> {
                                playerName = playerName.trim();
                                GameInfoRecord gameInfo =
                                        new GameInfoRecord(
                                                serverItem.getUri().getValue(),
                                                playerName,
                                                serverItem.getName().getValue(),
                                                String.valueOf(
                                                        serverItem.getMaxPlayers().getValue()));
                                viewModel.joinSelectedServer(gameInfo);
                            });
                };
        runOnFxThread(dialogTask);
    }



}
