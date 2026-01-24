package mci.dibse.crazylabyrinth.client.service;

import java.util.ArrayList;
import java.util.List;
import labyrinth.contracts.models.AchievementUnlockedEventPayload;
import labyrinth.contracts.models.EventType;
import labyrinth.contracts.models.GameStateEventPayload;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AchievementService {

    //
    // {"type":"ACHIEVEMENT_UNLOCKED","playerId":"52f0ddf3-bae9-4321-b517-26aeec40d498","achievement":"PUSHER"}
    @Getter private List<AchievementUnlockedEventPayload> achievementEvents = new ArrayList<>();

    /**
     * Store an achievement event and log it for debugging.
     *
     * @param event achievement event payload
     */
    public void handleAchievementEvent(AchievementUnlockedEventPayload event) {
        log.debug("Achievement Event received: {}", event);
        achievementEvents.add(event);
    }

    /**
     * Clear the achievement list when a new game starts.
     *
     * @param event game state payload
     */
    @EventListener
    public void listenForGameStartEvent(GameStateEventPayload event) {
        if (EventType.GAME_STARTED.equals(event.getType())) {
            log.debug("Clear Achievement list on game start");
            achievementEvents.clear();
        }
    }

    /**
     * Filter achievement events for a specific player.
     *
     * @param id player identifier
     * @return list of achievement events for the player
     */
    public List<AchievementUnlockedEventPayload> getAchievementEventsForPLayerId(String id) {
        return achievementEvents.stream().filter(e -> e.getPlayerId().equals(id)).toList();
    }
}
