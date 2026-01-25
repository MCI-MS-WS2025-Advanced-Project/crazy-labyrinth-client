package mci.dibse.crazylabyrinth.client.infrastructure.util;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import labyrinth.contracts.models.PlayerInfo;
import labyrinth.contracts.models.PlayerState;
import org.springframework.stereotype.Component;

@Component
public class ResultsPlayerInfoLookupHelper {

    // queue player Info For Lookup for results presentation and palyers are no longer in lobby
    Map<String, PlayerInfo> playerInfoLookupMap =
            new LinkedHashMap<String, PlayerInfo>(10, 0.75f, false) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, PlayerInfo> eldest) {
                    return size() > 10;
                }
            };

    public void cachePlayerInfoFromPlayerStates(PlayerState[] playerInfo) {
        Arrays.asList(playerInfo)
                .forEach(
                        pl -> {
                            if (playerInfoLookupMap.containsKey(pl.getPlayerInfo().getId())) {
                                playerInfoLookupMap.replace(
                                        pl.getPlayerInfo().getId(), pl.getPlayerInfo());
                            } else {
                                playerInfoLookupMap.put(
                                        pl.getPlayerInfo().getId(), pl.getPlayerInfo());
                            }
                        });
    }

    public Optional<String> lookUpCachedPlayerInfo(String id) {
        if (playerInfoLookupMap.containsKey(id)) {

            PlayerInfo pl = playerInfoLookupMap.get(id);
            String name = pl.getName();
            if (pl.getIsAiControlled()) name += " (AI)";
            return Optional.of(name);
        }
        return Optional.empty();
    }
}
