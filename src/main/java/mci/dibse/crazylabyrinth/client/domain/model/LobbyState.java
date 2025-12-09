package mci.dibse.crazylabyrinth.client.domain.model;

import labyrinth.contracts.models.EventType;
import labyrinth.contracts.models.LobbyStateEventPayload;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class LobbyState {


    @Getter
    private List<PlayerInfo> players;
    @Getter
    private Map<String, Object> additionalProperties;



    public LobbyState(LobbyStateEventPayload payload) {
        this.players = mapToPlayerInfo(payload.getPlayers());
        this.additionalProperties = payload.getAdditionalProperties();
    }


    private List<PlayerInfo> mapToPlayerInfo(labyrinth.contracts.models.PlayerInfo[] players) {
        return Stream.of(players).map(p -> {
            PlayerInfo playerInfo = new PlayerInfo();
            playerInfo.setId(p.getId());
            playerInfo.setName(p.getName());
            playerInfo.setColor(p.getColor());
            playerInfo.setIsAdmin(p.getIsAdmin());
            playerInfo.setIsAiControlled(p.getIsAiControlled());
            playerInfo.setAdditionalProperties(p.getAdditionalProperties());
            return playerInfo;
        }).collect(java.util.stream.Collectors.toList());

    }
}
