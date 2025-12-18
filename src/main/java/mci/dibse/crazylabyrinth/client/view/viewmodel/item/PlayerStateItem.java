package mci.dibse.crazylabyrinth.client.view.viewmodel.item;

import java.util.Arrays;
import java.util.List;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import labyrinth.contracts.models.AchievementType;
import labyrinth.contracts.models.BonusType;
import labyrinth.contracts.models.PlayerState;
import labyrinth.contracts.models.Treasure;
import lombok.Getter;
import mci.dibse.crazylabyrinth.client.infrastructure.util.JavaFXUtils;

public class PlayerStateItem extends BaseItem<PlayerState> {

    @Getter private final ObjectProperty<PlayerInfoItem> playerInfo = new SimpleObjectProperty<>();

    @Getter
    private final ObjectProperty<CoordinatesItem> currentPosition = new SimpleObjectProperty<>();

    @Getter
    private final ObjectProperty<CoordinatesItem> homePosition = new SimpleObjectProperty<>();

    @Getter
    private final ListProperty<TreasureItem> treasuresFound =
            new SimpleListProperty<>(FXCollections.observableArrayList());

    @Getter private final IntegerProperty remainingTreasureCount = new SimpleIntegerProperty();

    @Getter
    private final ListProperty<AchievementType> achievementTypes =
            new SimpleListProperty<>(FXCollections.observableArrayList());

    @Getter
    private final ListProperty<BonusType> availableBonusTypes =
            new SimpleListProperty<>(FXCollections.observableArrayList());

    public PlayerStateItem(PlayerState source) {
        List<TreasureItem> treasureItems =
                Arrays.stream(source.getTreasuresFound()).map(TreasureItem::new).toList();
        List<AchievementType> achievements = Arrays.stream(source.getAchievements()).toList();
        List<BonusType> bonuses = Arrays.stream(source.getAvailableBonuses()).toList();
        Runnable apply =
                () -> {
                    this.playerInfo.set(new PlayerInfoItem(source.getPlayerInfo()));
                    this.currentPosition.set(new CoordinatesItem(source.getCurrentPosition()));
                    this.homePosition.set(new CoordinatesItem(source.getHomePosition()));
                    this.treasuresFound.set(FXCollections.observableArrayList(treasureItems));
                    this.remainingTreasureCount.set(source.getRemainingTreasureCount());
                    this.achievementTypes.set(FXCollections.observableArrayList(achievements));
                    this.availableBonusTypes.set(FXCollections.observableArrayList(bonuses));
                };
        JavaFXUtils.runOnFxThread(apply);
    }

    public String getPlayerId() {
        return playerInfo.get().getId();
    }

    @Override
    protected void updateItemFromSource(PlayerState source) {
        List<Treasure> treasures = Arrays.stream(source.getTreasuresFound()).toList();
        List<TreasureItem> notExistingTreasures =
                treasures.stream()
                        .filter(
                                t ->
                                        !this.treasuresFound.get().stream()
                                                .map(i -> i.getId().get())
                                                .toList()
                                                .contains(t.getId()))
                        .map(TreasureItem::new)
                        .toList();
        this.currentPosition.get().updateFrom(source.getCurrentPosition());
        this.treasuresFound.addAll(notExistingTreasures);
        this.playerInfo.get().updateFrom(source.getPlayerInfo());
    }

    @Override
    protected void disposeItem() {
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
