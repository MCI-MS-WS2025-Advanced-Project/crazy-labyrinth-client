package mci.dibse.crazylabyrinth.client.domain.model;

import labyrinth.contracts.models.PlayerInfo;
import lombok.Getter;
import lombok.Setter;

public class ClientPlayer extends PlayerInfo {

    @Setter @Getter private String identifierToken;
}
