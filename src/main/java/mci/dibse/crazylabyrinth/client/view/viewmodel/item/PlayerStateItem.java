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
    private final ObjectProperty<CoordinatesItem> previousPosition = new SimpleObjectProperty<>();

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
        super();
        List<TreasureItem> treasureItems =
                Arrays.stream(source.getTreasuresFound()).map(TreasureItem::new).toList();

        List<AchievementType> achievements = processAchievementList(source);

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

    /**
     * Update the nested player info item.
     *
     * @param playerInfo player info payload
     */
    public void updatePlayerInfoFrom(PlayerInfo playerInfo) {
        log.debug("updatePlayerInfoFrom {}", playerInfo);
        this.playerInfo.get().updateFrom(playerInfo);
    }

    /**
     * Return the player id.
     *
     * @return player id
     */
    public String getPlayerIdAsString() {
        return playerInfo.get().getId();
    }

    /**
     * Return the player name.
     *
     * @return player name
     */
    public String getPlayerNameAsString() {
        return playerInfo.get().getName();
    }

    /**
     * Return the player color.
     *
     * @return player color
     */
    public PlayerColor getPlayerColor() {
        return playerInfo.get().getColor();
    }

    /**
     * Return the current row position of the player.
     *
     * @return current row
     */
    public int getPlayerCurrentRowAsInt() {
        return currentPosition.get().getRowAsInt();
    }

    /**
     * Return the current column position of the player.
     *
     * @return current column
     */
    public int getPlayerCurrentColumnAsInt() {
        return currentPosition.get().getColumnAsInt();
    }

    /**
     * Return the remaining treasure count.
     *
     * @return remaining treasure count
     */
    public int getRemainingTreasureCountAsInt() {
        return remainingTreasureCount.intValue();
    }

    /**
     * Count available bonuses of the given type.
     *
     * @param type bonus type to count
     * @return available count
     */
    public int getAvailableBonusCountForType(BonusType type) {
        return Math.toIntExact(availableBonusTypes.stream().filter(type::equals).count());
    }

    /**
     * Return the number of treasures found.
     *
     * @return number of treasures found
     */
    public int getTreasuresFoundAsInt() {
        return treasuresFound.size();
    }

    /**
     * Check whether the player is AI controlled.
     *
     * @return true if AI controlled
     */
    public boolean isPlayerAiControlled() {
        return playerInfo.get().getIsAiControlled();
    }

    /**
     * Update player state from the latest payload.
     *
     * @param source player state payload
     */
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
        if (this.currentPosition.get() != null) {
            this.previousPosition.set(this.currentPosition.get());
        }
        this.currentPosition.set(new CoordinatesItem(source.getCurrentPosition()));
        this.treasuresFound.addAll(notExistingTreasures);
        this.playerInfo.get().updateFrom(source.getPlayerInfo());

        this.remainingTreasureCount.set(source.getRemainingTreasureCount());

        List<AchievementType> achievements = processAchievementList(source);
        this.achievementTypes.set(FXCollections.observableArrayList(achievements));

        List<BonusType> bonuses = Arrays.stream(source.getAvailableBonuses()).toList();
        // bonuses = bonuses.stream().filter(a -> !this.availableBonusTypes.contains(a)).toList();
        // this.availableBonusTypes.addAll(bonuses);
        this.availableBonusTypes.set(FXCollections.observableArrayList(bonuses));
    }

    /**
     * Dispose nested items and clear collections.
     */
    @Override
    protected void disposeItem() {
        playerInfo.get().dispose();
        currentPosition.get().dispose();
        homePosition.get().dispose();
        treasuresFound.get().forEach(TreasureItem::dispose);
        treasuresFound.clear();
        achievementTypes.clear();
        availableBonusTypes.clear();

        playerInfo.unbind();
        currentPosition.unbind();
        homePosition.unbind();
    }

    // "Wir hatten ausgemacht, dass die Achievements erst am Schluss mittels achievementUnlockEvent
    // (Broadcast) "verteilt" werden und nicht während dem Spiel"
    private List<AchievementType> processAchievementList(PlayerState source) {
        List<AchievementType> achievements;
        if (source.getAchievements() != null)
            achievements = Arrays.stream(source.getAchievements()).toList();
        else achievements = new java.util.ArrayList<>();
        return achievements;
    }
}
