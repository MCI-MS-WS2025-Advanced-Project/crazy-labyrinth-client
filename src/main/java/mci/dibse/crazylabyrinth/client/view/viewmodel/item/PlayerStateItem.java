package mci.dibse.crazylabyrinth.client.view.viewmodel.item;

import java.util.Arrays;
import java.util.List;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import labyrinth.contracts.models.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import mci.dibse.crazylabyrinth.client.infrastructure.util.JavaFXUtils;

@Slf4j
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

    public void updatePlayerInfoFrom(PlayerInfo playerInfo) {
        log.debug("updatePlayerInfoFrom {}", playerInfo);
        this.playerInfo.get().updateFrom(playerInfo);
    }

    public String getPlayerIdAsString() {
        return playerInfo.get().getId();
    }

    public String getPlayerNameAsString() {
        return playerInfo.get().getName();
    }

    public PlayerColor getPlayerColor() {
        return playerInfo.get().getColor();
    }

    public int getPlayerCurrentRowAsInt() {
        return currentPosition.get().getRowAsInt();
    }

    public int getPlayerCurrentColumnAsInt() {
        return currentPosition.get().getColumnAsInt();
    }

    public int getRemainingTreasureCountAsInt() {
        return remainingTreasureCount.intValue();
    }

    public int getAvailableBonusCountForType(BonusType type) {
        return Math.toIntExact(availableBonusTypes.stream().filter(type::equals).count());
    }

    public boolean isPlayerAiControlled() {
        return playerInfo.get().getIsAiControlled();
    }

    @Override
    protected void updateItemFromSource(PlayerState source) {
        List<Treasure> treasures = Arrays.stream(source.getTreasuresFound()).toList();
        List<TreasureItem> notExistingTreasures =
                treasures.stream()
                        .filter(
                                t ->
                                        !this.treasuresFound.get().stream()
                                                .map(TreasureItem::getId)
                                                .toList()
                                                .contains(t.getId()))
                        .map(TreasureItem::new)
                        .toList();
        this.currentPosition.get().updateFrom(source.getCurrentPosition());
        this.treasuresFound.addAll(notExistingTreasures);
        this.playerInfo.get().updateFrom(source.getPlayerInfo());
        this.remainingTreasureCount.set(source.getRemainingTreasureCount());

        List<AchievementType> achievements = Arrays.stream(source.getAchievements()).toList();
        // achievements = achievements.stream().filter(a ->
        // !this.achievementTypes.contains(a)).toList();
        // this.achievementTypes.addAll(achievements);
        this.achievementTypes.set(FXCollections.observableArrayList(achievements));

        List<BonusType> bonuses = Arrays.stream(source.getAvailableBonuses()).toList();
        // bonuses = bonuses.stream().filter(a -> !this.availableBonusTypes.contains(a)).toList();
        // this.availableBonusTypes.addAll(bonuses);
        this.availableBonusTypes.set(FXCollections.observableArrayList(bonuses));
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
