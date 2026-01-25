package mci.dibse.crazylabyrinth.client.service;

import labyrinth.contracts.models.GameStartedEventPayload;
import labyrinth.contracts.models.GameStateUpdateEventPayload;
import labyrinth.contracts.models.NextTreasureCardEventPayload;
import labyrinth.contracts.models.PlayerTurnEventPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class GameServiceImpl implements GameService {


    private ApplicationEventPublisher eventListener;

    @Autowired
    public GameServiceImpl(ApplicationEventPublisher eventListener) {
        this.eventListener = eventListener;
    }


    public void handleGameEvent(GameStartedEventPayload gameStarted) {
        eventListener.publishEvent(gameStarted);
    }

    public void handleGameEvent(NextTreasureCardEventPayload nextTreasureCard) {
        eventListener.publishEvent(nextTreasureCard);
    }

    public void handleGameEvent(GameStateUpdateEventPayload gameState) {
        eventListener.publishEvent(gameState);
    }

    public void handleGameEvent(PlayerTurnEventPayload playerTurn) {
        eventListener.publishEvent(playerTurn);
    }

}
