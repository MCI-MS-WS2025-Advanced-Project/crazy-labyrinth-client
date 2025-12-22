package mci.dibse.crazylabyrinth.client.service;

import labyrinth.contracts.models.GameStateEventPayload;
import labyrinth.contracts.models.NextTreasureCardEventPayload;

public interface GameService {

    void handleGameEvent(GameStateEventPayload gameState);

    void handleGameEvent(NextTreasureCardEventPayload nextTreasureCard);
}
