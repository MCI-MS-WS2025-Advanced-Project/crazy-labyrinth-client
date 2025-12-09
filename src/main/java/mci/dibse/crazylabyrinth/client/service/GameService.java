package mci.dibse.crazylabyrinth.client.service;

import labyrinth.contracts.models.GameStartedEventPayload;
import labyrinth.contracts.models.GameStateUpdateEventPayload;
import labyrinth.contracts.models.NextTreasureCardEventPayload;
import labyrinth.contracts.models.PlayerTurnEventPayload;

public interface GameService {

    void handleGameEvent(GameStateUpdateEventPayload gameState);
    void handleGameEvent(GameStartedEventPayload gameStarted);
    void handleGameEvent(NextTreasureCardEventPayload nextTreasureCard);
    void handleGameEvent(PlayerTurnEventPayload playerTurn);

}
