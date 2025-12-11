package mci.dibse.crazylabyrinth.client.view.viewmodel.item;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import labyrinth.contracts.models.AchievementType;
import labyrinth.contracts.models.BonusType;
import labyrinth.contracts.models.PlayerState;
import labyrinth.contracts.models.Treasure;


import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PlayerStateItem implements Disposable {

    private final java.util.concurrent.atomic.AtomicBoolean pendingUpdate =
            new java.util.concurrent.atomic.AtomicBoolean(false);

    private final ObjectProperty<PlayerInfoItem> playerInfo = new SimpleObjectProperty<>();
    private final ObjectProperty<CoordinatesItem> currentPosition = new SimpleObjectProperty<>();
    private final ObjectProperty<CoordinatesItem> homePosition = new SimpleObjectProperty<>();
    private final ListProperty<TreasureItem> treasuresFound = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final IntegerProperty remainingTreasureCount = new SimpleIntegerProperty();
    private final ListProperty<AchievementType> achievementTypes = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ListProperty<BonusType> availableBonusTypes = new SimpleListProperty<>(FXCollections.observableArrayList());


    public PlayerStateItem(PlayerState source) {
        List<TreasureItem> treasureItems = Arrays.stream(source.getTreasuresFound()).map(TreasureItem::new).toList();
        List<AchievementType> achievements = Arrays.stream(source.getAchievements()).toList();
        List<BonusType> bonuses = Arrays.stream(source.getAvailableBonuses()).toList();
        Runnable apply = () -> {

            this.playerInfo.set(new PlayerInfoItem(source.getPlayerInfo()));
            System.out.println("playerInfoItem created" + source.getPlayerInfo().toString());

            this.currentPosition.set(new CoordinatesItem(source.getCurrentPosition()));
            this.homePosition.set(new CoordinatesItem(source.getHomePosition()));

            this.treasuresFound.set(FXCollections.observableArrayList(treasureItems));

            this.remainingTreasureCount.set(source.getRemainingTreasureCount());

            this.achievementTypes.set(FXCollections.observableArrayList(achievements));

            this.availableBonusTypes.set(FXCollections.observableArrayList(bonuses));
        };
        if (javafx.application.Platform.isFxApplicationThread()) apply.run();
        else javafx.application.Platform.runLater(apply);
    }

    public void updateFrom(PlayerState source) {
        List<Treasure> treasures = Arrays.stream(source.getTreasuresFound()).toList();
        List<TreasureItem> notExistingTreasures = treasures.stream()
                .filter(t -> !this.treasuresFound.get().stream().map(i -> i.getId().get()).toList().contains(t.getId())).map(TreasureItem::new)
                .toList();
        Runnable apply = () -> {
            System.out.println("PlayerStateItem: currentPosition: " + source.getPlayerInfo().getName() +"  " + source.getCurrentPosition().toString() + "");
            this.currentPosition.get().updateFrom(source.getCurrentPosition());
            this.treasuresFound.addAll(notExistingTreasures);
            this.playerInfo.get().updateFrom(source.getPlayerInfo());
        };

        if (javafx.application.Platform.isFxApplicationThread()) apply.run();
        else javafx.application.Platform.runLater(apply);

    }

    public PlayerInfoItem getPlayerInfo() {
        return playerInfo.get();
    }

    public ObjectProperty<PlayerInfoItem> playerInfoProperty() {
        return playerInfo;
    }

    public void setPlayerInfo(PlayerInfoItem playerInfo) {
        this.playerInfo.set(playerInfo);
    }

    public CoordinatesItem getCurrentPosition() {
        return currentPosition.get();
    }

    public ObjectProperty<CoordinatesItem> currentPositionProperty() {
        return currentPosition;
    }

    public void setCurrentPosition(CoordinatesItem currentPosition) {
        this.currentPosition.set(currentPosition);
    }

    public CoordinatesItem getHomePosition() {
        return homePosition.get();
    }

    public ObjectProperty<CoordinatesItem> homePositionProperty() {
        return homePosition;
    }

    public void setHomePosition(CoordinatesItem homePosition) {
        this.homePosition.set(homePosition);
    }

    public ObservableList<TreasureItem> getTreasuresFound() {
        return treasuresFound.get();
    }

    public ListProperty<TreasureItem> treasuresFoundProperty() {
        return treasuresFound;
    }

    public void setTreasuresFound(ObservableList<TreasureItem> treasuresFound) {
        this.treasuresFound.set(treasuresFound);
    }

    public int getRemainingTreasureCount() {
        return remainingTreasureCount.get();
    }

    public IntegerProperty remainingTreasureCountProperty() {
        return remainingTreasureCount;
    }

    public void setRemainingTreasureCount(int remainingTreasureCount) {
        this.remainingTreasureCount.set(remainingTreasureCount);
    }

    public ObservableList<AchievementType> getAchievementTypes() {
        return achievementTypes.get();
    }

    public ListProperty<AchievementType> achievementTypesProperty() {
        return achievementTypes;
    }



    public ObservableList<BonusType> getAvailableBonusTypes() {
        return availableBonusTypes.get();
    }

    public ListProperty<BonusType> availableBonusTypesProperty() {
        return availableBonusTypes;
    }





    @Override
    public void dispose() {
        playerInfo.get().dispose();
        currentPosition.get().dispose();
        homePosition.get().dispose();
        treasuresFound.get().forEach(TreasureItem::dispose);
        treasuresFound.clear();
        achievementTypes.clear();
        availableBonusTypes.clear();
        playerInfo.set(null);
        currentPosition.set(null);
        homePosition.set(null);
    }
}

