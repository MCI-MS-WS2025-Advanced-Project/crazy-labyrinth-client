package mci.dibse.crazylabyrinth.client.service;

import labyrinth.contracts.models.EventType;
import labyrinth.contracts.models.GameStateEventPayload;
import labyrinth.contracts.models.NextTreasureCardEventPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class GameServiceImpl implements GameService {

    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public GameServiceImpl(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * Publish relevant game state events to the application bus.
     *
     * @param gameState game state payload
     */
    @Override
    public void handleGameEvent(GameStateEventPayload gameState) {
        switch (gameState.getType()) {
            case EventType.GAME_STARTED, EventType.GAME_STATE_UPDATE ->
                    eventPublisher.publishEvent(gameState);
        }
    }

    /**
     * Publish next treasure card events to the application bus.
     *
     * @param nextTreasureCard next treasure payload
     */
    @Override
    public void handleGameEvent(NextTreasureCardEventPayload nextTreasureCard) {
        eventPublisher.publishEvent(nextTreasureCard);
    }
}
