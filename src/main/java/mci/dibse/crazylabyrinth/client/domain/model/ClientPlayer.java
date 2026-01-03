package mci.dibse.crazylabyrinth.client.domain.model;

import labyrinth.contracts.models.ConnectAckEventPayload;
import labyrinth.contracts.models.PlayerInfo;
import lombok.Getter;
import lombok.Setter;

public class ClientPlayer {

    @Setter @Getter private String identifierToken;
    @Setter @Getter private String id;

}
