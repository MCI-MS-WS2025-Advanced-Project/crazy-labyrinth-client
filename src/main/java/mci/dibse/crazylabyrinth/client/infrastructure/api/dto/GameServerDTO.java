package mci.dibse.crazylabyrinth.client.infrastructure.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import mci.dibse.crazylabyrinth.client.domain.model.enums.GameStatus;

/**
 * <p>
 * Data Transfer Object representing a game server in the Crazy Labyrinth system.
 * modeled after GameServer in GameManagementServer_openapi.yml
 * </p>
 * <p>
 * The class includes validation constraints to ensure data integrity:
 * <ul>
 *   <li>Server must have a unique identifier and non-blank name</li>
 *   <li>Maximum players must be at least 2 and max 4</li>
 *   <li>Current player count cannot be negative</li>
 *   <li>Status and last seen timestamp are required</li>
 * </ul>
 * </p>
 *
 *
 */
@Setter
@Getter
public class GameServerDTO {

    @NotNull private UUID id;

    @NotBlank private String name;

    @NotNull private URI uri;

    @Min(2)
    @Max(4)
    private int maxPlayers;

    @Min(0)
    private int currentPlayerCount;

    @NotNull private GameStatus status;


    @NotNull private Instant lastSeen;

    public GameServerDTO() {}

    public GameServerDTO(
            UUID id,
            String name,
            URI uri,
            int maxPlayers,
            int currentPlayerCount,
            GameStatus status,
            Instant lastSeen) {
        this.id = id;
        this.name = name;
        this.uri = uri;
        this.maxPlayers = maxPlayers;
        this.currentPlayerCount = currentPlayerCount;
        this.status = status;
        this.lastSeen = lastSeen;
    }

    /**
     * Return a string representation of the game server DTO.
     *
     * @return string representation
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("GameServer{");
        sb.append("id=")
                .append(id)
                .append(", name='")
                .append(name)
                .append('\'')
                .append(", uri=")
                .append(uri)
                .append(", maxPlayers=")
                .append(maxPlayers)
                .append(", currentPlayerCount=")
                .append(currentPlayerCount)
                .append(", status=")
                .append(status)
                .append(", lastSeen=")
                .append(lastSeen)
                .append('}');
        return sb.toString();
    }
}
