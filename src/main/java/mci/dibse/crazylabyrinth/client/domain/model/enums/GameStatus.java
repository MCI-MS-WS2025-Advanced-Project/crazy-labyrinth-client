package mci.dibse.crazylabyrinth.client.domain.model.enums;

/**
 * GameStatus modeled after GameManagementServer_openapi.yml
 *  Current game state:
 *     - `LOBBY`: Waiting for players to join
 *     - `IN_GAME`: Game is currently running
 *     - `FINISHED`: Game has ended
 *
 * @author Julian Janetschek
 */
public enum GameStatus {
    LOBBY,
    IN_PROGRESS,
    FINISHED
}
