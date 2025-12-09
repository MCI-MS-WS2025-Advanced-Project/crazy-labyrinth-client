package mci.dibse.crazylabyrinth.client.domain.model.record;

public record StartGameRecord(int treasureCardCount, int totalBonusCount, int gameDurationInSeconds, int boardSize) {
}
