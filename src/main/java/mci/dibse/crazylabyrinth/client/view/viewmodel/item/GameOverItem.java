package mci.dibse.crazylabyrinth.client.view.viewmodel.item;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import labyrinth.contracts.models.*;
import lombok.Getter;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class GameOverItem extends BaseItem<GameOverEventPayload>{


    @Getter private SimpleStringProperty winnerIdProperty = new SimpleStringProperty();
    @Getter private final ListProperty<RankingEntry> rankingEntries = new SimpleListProperty<RankingEntry>();
    private final FilteredList<RankingEntry> filteredRankings;
    @Getter private final SortedList<RankingEntry> sortedRankings;

    public GameOverItem(GameOverEventPayload source) {
        super();
        winnerIdProperty.set(source.getWinnerId());
        List<RankingEntry> rankingEntryList = Arrays.stream(source.getRanking()).toList();
        rankingEntries.sort(Comparator.comparingInt(RankingEntry::getRank));
        rankingEntries.set(FXCollections.observableArrayList(rankingEntryList));
        filteredRankings = new FilteredList<>(rankingEntries);
        sortedRankings = new SortedList<>(filteredRankings);
    }

    @Override
    protected void updateItemFromSource(GameOverEventPayload source) {
        winnerIdProperty.set(source.getWinnerId());
        List<RankingEntry> rankingEntryList = Arrays.stream(source.getRanking()).toList();
        rankingEntries.set(FXCollections.observableArrayList(rankingEntryList));
    }

    @Override
    protected void disposeItem() {
        winnerIdProperty.unbind();
        //winnerIdProperty.set(null);
        rankingEntries.unbind();
        //rankingEntries.set(null);

        sortedRankings.comparatorProperty().unbind();
    }

    public void bindSortedRankingsComparator(ReadOnlyObjectProperty<Comparator<RankingEntry>> comparatorReadOnlyObjectProperty) {
        sortedRankings.comparatorProperty().bind(comparatorReadOnlyObjectProperty);
    }
}

