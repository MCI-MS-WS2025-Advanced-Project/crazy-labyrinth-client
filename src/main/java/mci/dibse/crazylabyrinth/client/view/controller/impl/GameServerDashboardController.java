package mci.dibse.crazylabyrinth.client.view.controller.impl;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import mci.dibse.crazylabyrinth.client.domain.model.record.GameInfoRecord;
import mci.dibse.crazylabyrinth.client.view.viewmodel.GameServerDashboardViewModel;
import mci.dibse.crazylabyrinth.client.view.viewmodel.item.GameServerItem;
import mci.dibse.crazylabyrinth.client.view.ViewCleanup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *  @author Julian Janetschek
 */
@Component
public class GameServerDashboardController implements ViewCleanup {

    private GameServerDashboardViewModel viewModel;
    private final DateTimeFormatter lastSeenFormatter =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(java.time.ZoneId.systemDefault());

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

    @Autowired
    public GameServerDashboardController(GameServerDashboardViewModel vm) {
        this.viewModel = vm;
    }

    @FXML
    public void initialize() {

        System.out.println("GameServerDashboardController initialized");
        serversTable.setItems(viewModel.getSorted());

        viewModel.getSorted().comparatorProperty().bind(serversTable.comparatorProperty());
        showOnlyJoinable.selectedProperty().bindBidirectional(viewModel.onlyJoinableProperty());

        serverCountLabel.textProperty().bind(Bindings.size(viewModel.getSorted()).asString());

        nameCol.setCellValueFactory(cd -> cd.getValue().nameProperty());

        statusCol.setCellValueFactory(
                cd -> {
                    var item = cd.getValue();
                    var wrapped = new ReadOnlyStringWrapper();

                    wrapped.bind(item.statusProperty().asString());
                    return wrapped;
                });

        currentPlayersCol.setCellValueFactory(
                cd -> {
                    var item = cd.getValue();
                    var combined = new ReadOnlyStringWrapper();

                    combined.bind(
                            item.currentPlayerCountProperty()
                                    .asString()
                                    .concat(" / ")
                                    .concat(item.maxPlayersProperty().asString()));
                    return combined;
                });

        lastSeenCol.setCellFactory(
                col ->
                        new TableCell<>() {
                            @Override
                            protected void updateItem(String iso, boolean empty) {
                                super.updateItem(iso, empty);
                                if (empty || iso == null || iso.isBlank()) {
                                    setText(null);
                                } else {
                                    setText(iso);
                                }
                                var rowItem = getTableRow().getItem();
                                var instant =
                                        rowItem == null ? null : rowItem.getLastSeen().getValue();
                                if (instant == null) {
                                    setText("");
                                } else {
                                    setText(lastSeenFormatter.format(instant));
                                }
                            }
                        });

        lastSeenCol.setCellValueFactory(cd -> cd.getValue().lastSeenProperty().asString());

        actionCol.setCellFactory(
                col ->
                        new TableCell<>() {
                            private final Button joinBtn = new Button("Join");

                            {
                                joinBtn.setOnAction(
                                        e -> {
                                            GameServerItem item =
                                                    getTableRow() == null
                                                            ? null
                                                            : getTableRow().getItem();
                                            if (item != null) {
                                                System.out.println(item.getName());
                                                // TODO: call your actual join method here
                                               // viewModel.joinSelectedServer(item.getUri().get());
                                                showPlayerNameInputModal(item);

                                            }
                                        });
                                joinBtn.setMinWidth(70);
                            }

                            @Override
                            protected void updateItem(String value, boolean empty) {
                                super.updateItem(value, empty);
                                if (empty
                                        || getTableRow() == null
                                        || getTableRow().getItem() == null) {
                                    setGraphic(null);
                                    return;
                                }
                                GameServerItem item = getTableRow().getItem();
                                // disable if not joinable (same rule as your context menu)
                                boolean joinable =
                                        item.getStatus().getValue()
                                                        == mci.dibse.crazylabyrinth.client.domain
                                                                .model.enums.GameStatus.LOBBY
                                                && item.getCurrentPlayerCount().getValue()
                                                        < item.getMaxPlayers().getValue();
                                joinBtn.setDisable(!joinable);
                                setGraphic(joinBtn);
                            }
                        });

        serversTable.setRowFactory(
                tv -> {
                    TableRow<GameServerItem> row = new TableRow<>();
                    MenuItem joinMenuItem = new MenuItem("Join");
                    joinMenuItem.setOnAction(
                            e -> {
                                GameServerItem item = row.getItem();
                                if (item != null) {
                                    // TODO impl join feature
                                    showPlayerNameInputModal(item);

                                }
                            });

                    ContextMenu rowMenu = new ContextMenu(joinMenuItem);
                    row.contextMenuProperty()
                            .bind(
                                    Bindings.when(row.emptyProperty())
                                            .then((ContextMenu) null)
                                            .otherwise(rowMenu));
                    return row;
                });

        refreshButton.setOnAction(
                e -> {
                    viewModel.refreshServerList();
                });

        statusLabel
                .textProperty()
                .bind(
                        Bindings.createStringBinding(
                                () -> {
                                    if (viewModel.getHasError().get()) {
                                        return viewModel.getErrorMessage().get();
                                    } else if (viewModel.getIsRefreshing().get()) {
                                        return "Loading servers...";
                                    } else {
                                        return "No servers found. Click Refresh to query the"
                                                + " registry.";
                                    }
                                },
                                viewModel.getHasError(),
                                viewModel.getErrorMessage(),
                                viewModel.getIsRefreshing()));

        viewModel
                .getHasError()
                .addListener(
                        (obs, oldVal, newVal) -> {
                            if (newVal) {
                                showAlertModal(
                                        "Server loading error",
                                        "",
                                        "Something went wrong. Please try again later.");
                            }
                        });
        viewModel.refreshServerList();
    }

    //TODO impl cleanup


    private void showPlayerNameInputModal(GameServerItem serverItem) {
        Runnable showDialog = () -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Join Game Server");
            dialog.setHeaderText("Join " + serverItem.getName().getValue());
            dialog.setContentText("Please enter your player name:");
            dialog.setGraphic(null);

            // Set default focus on the text field
            dialog.getEditor().requestFocus();

            // Validate input - don't allow empty names
            Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
            okButton.disableProperty().bind(
                    dialog.getEditor().textProperty().isEmpty().or(
                            dialog.getEditor().textProperty().isEqualTo("").or(
                                    Bindings.createBooleanBinding(
                                            () -> dialog.getEditor().getText().trim().isEmpty(),
                                            dialog.getEditor().textProperty()
                                    )
                            )
                    )
            );

            Optional<String> result = dialog.showAndWait();
            result.ifPresent(playerName -> {
                playerName = playerName.trim();
                if (!playerName.isEmpty()) {

                    // Call your actual join method here with the player name

                    GameInfoRecord gameInfo = new GameInfoRecord(serverItem.getUri().getValue(), playerName, serverItem.getName().getValue(), String.valueOf(serverItem.getMaxPlayers().getValue()));
                    viewModel.joinSelectedServer(gameInfo);
                    // You might want to pass the playerName to your join method as well
                }
            });
        };

        if (Platform.isFxApplicationThread()) {
            showDialog.run();
        } else {
            Platform.runLater(showDialog);
        }
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

    @Override
    public void cleanup() {

    }
}

