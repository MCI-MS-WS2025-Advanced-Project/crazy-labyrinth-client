package mci.dibse.crazylabyrinth.client.domain.model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import mci.dibse.crazylabyrinth.client.domain.model.enums.GameStatus;
import mci.dibse.crazylabyrinth.client.infrastructure.api.dto.GameServerDTO;

/**
 * Represents a game server in the Crazy Labyrinth client application.
 * <p>
 * This class encapsulates the information about a game server. The class is
 * modeled after the GameManagementServer_openapi.yml
 * It implements the Observable pattern through {@link PropertyChangeSupport}
 * to notify listeners when server properties change.
 * <p>
 * The class supports multiple construction patterns:
 * <ul>
 *   <li>Constructor from {@link GameServerDTO} for mapping from API responses</li>
 *   <li>Full constructor for creating instances with all properties</li>
 * </ul>
 *
 *
 *
 * @author Julian Janetschek
 *
 */
@Getter
public class GameServer {


    private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);


    private UUID id;


    private String name;


    private URI uri;


    private int maxPlayers;


    private int currentPlayerCount;


    private GameStatus status;


    private Instant lastSeen;



    /**
     * Creates a GameServer instance from a GameServerDTO.
     * <p>
     * This constructor is typically used when mapping API responses
     * to domain model objects.
     *
     * @param dto the GameServerDTO containing server information
     */
    public GameServer(GameServerDTO dto) {
        this.id = dto.getId();
        this.name = dto.getName();
        this.uri = dto.getUri();
        this.maxPlayers = dto.getMaxPlayers();
        this.currentPlayerCount = dto.getCurrentPlayerCount();
        this.status = dto.getStatus();
        this.lastSeen = dto.getLastSeen();
    }

    /**
     * Creates a GameServer instance with all properties specified.
     *
     * @param id                 unique identifier of the server
     * @param name              human-readable name of the server
     * @param uri               URI endpoint for server connection
     * @param maxPlayers        maximum number of players allowed
     * @param currentPlayerCount current number of connected players
     * @param status            current game status on this server
     * @param lastSeen          timestamp when server was last contacted
     */
    public GameServer(
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


    public void addListener(PropertyChangeListener l) {
        propertyChangeSupport.addPropertyChangeListener(l);
    }


    public void removeListener(PropertyChangeListener l) {
        propertyChangeSupport.removePropertyChangeListener(l);
    }


    public void setName(String name) {
        String oldValue = this.name;
        this.name = name;
        propertyChangeSupport.firePropertyChange("name", oldValue, name);
    }


    public void setUri(URI uri) {
        URI oldValue = this.uri;
        this.uri = uri;
        propertyChangeSupport.firePropertyChange("uri", oldValue, uri);
    }


    public void setMaxPlayers(int maxPlayers) {
        int oldValue = this.maxPlayers;
        this.maxPlayers = maxPlayers;
        propertyChangeSupport.firePropertyChange("maxPlayers", oldValue, maxPlayers);
    }


    public void setCurrentPlayerCount(int currentPlayerCount) {
        int oldValue = this.currentPlayerCount;
        this.currentPlayerCount = currentPlayerCount;
        propertyChangeSupport.firePropertyChange(
                "currentPlayerCount", oldValue, currentPlayerCount);
    }


    public void setStatus(GameStatus status) {
        GameStatus oldValue = this.status;
        this.status = status;
        propertyChangeSupport.firePropertyChange("status", oldValue, status);
    }


    public void setId(UUID id) {
        UUID oldValue = this.id;
        this.id = id;
        propertyChangeSupport.firePropertyChange("id", oldValue, id);
    }


    public void setLastSeen(Instant lastSeen) {
        Instant oldValue = this.lastSeen;
        this.lastSeen = lastSeen;
        propertyChangeSupport.firePropertyChange("lastSeen", oldValue, lastSeen);
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("GameServer{");
        sb.append("id=").append(id)
                .append(", name='").append(name).append('\'')
                .append(", uri=").append(uri)
                .append(", maxPlayers=").append(maxPlayers)
                .append(", currentPlayerCount=").append(currentPlayerCount)
                .append(", status=").append(status)
                .append(", lastSeen=").append(lastSeen)
                .append('}');
        return sb.toString();

    }
}