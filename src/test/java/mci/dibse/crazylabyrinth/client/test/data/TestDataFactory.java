package mci.dibse.crazylabyrinth.client.test.data;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import mci.dibse.crazylabyrinth.client.domain.model.enums.GameStatus;
import mci.dibse.crazylabyrinth.client.domain.model.management.GameServer;
import mci.dibse.crazylabyrinth.client.infrastructure.api.dto.GameServerDTO;

/**
 *  @author Julian Janetschek
 */
public class TestDataFactory {

    public static String valid_getServers_ResponseBody() {
        return """
        [
                 {
                   "id": "7b5f0b3b-3d2a-4f69-a2d0-8bfa2ec1c9c1",
                   "name": "Arena Alpha",
                   "uri": "http://example.com/server/alpha",
                   "maxPlayers": 4,
                   "currentPlayerCount": 2,
                   "status": "LOBBY",
                   "lastSeen": "2025-12-05T12:07:21Z"
                 },
                 {
                   "id": "0f2e6d71-3a90-4d7a-9f3a-5d6d3d5b2b41",
                   "name": "Desert Clash",
                   "uri": "http://example.com/server/desert",
                   "maxPlayers": 4,
                   "currentPlayerCount": 1,
                   "status": "IN_PROGRESS",
                   "lastSeen": "2025-12-05T11:58:03Z"
                 },
                 {
                   "id": "b2c3d4e5-6f70-4182-9b34-1a2b3c4d5e6f",
                   "name": "Nightfall",
                   "uri": "http://example.com/server/nightfall",
                   "maxPlayers": 4,
                   "currentPlayerCount": 1,
                   "status": "FINISHED",
                   "lastSeen": "2025-12-04T23:59:59Z"
                 },
                 {
                   "id": "a1d9c8b7-2e3f-4a5b-8c9d-0e1f2a3b4c5d",
                   "name": "Ice Cavern",
                   "uri": "http://example.com/server/ice-cavern",
                   "maxPlayers": 4,
                   "currentPlayerCount": 4,
                   "status": "LOBBY",
                   "lastSeen": "2025-12-05T12:10:45Z"
                 }
               ]
        """;
    }

    public static List<GameServer> createRandomGameServers(int count) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        List<GameServer> servers = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            int hostNumber = rnd.nextInt(1000, 10000);
            URI uri = URI.create(String.format("ws://gs.examaple%d.com:9000/game", hostNumber));

            int maxPlayers = 4;
            int currentPlayerCount = rnd.nextInt(0, maxPlayers + 1);
            GameStatus status = randomStatus(rnd);

            // Random lastSeen within the last 24 hours
            Instant lastSeen = Instant.now().minusSeconds(rnd.nextLong(0, 10 * 60));

            String name = "Game Server-" + hostNumber;

            servers.add(
                    new GameServer(
                            UUID.randomUUID(),
                            name,
                            uri,
                            maxPlayers,
                            currentPlayerCount,
                            status,
                            lastSeen));
        }

        return servers;
    }

    public static List<GameServerDTO> createRandomGameServersDTOs(int count) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        List<GameServerDTO> servers = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            int hostNumber = rnd.nextInt(1000, 10000);
            URI uri = URI.create(String.format("ws://gs.examaple%d.com:9000/game", hostNumber));

            int maxPlayers = 4;
            int currentPlayerCount = rnd.nextInt(0, maxPlayers + 1);
            GameStatus status = randomStatus(rnd);

            Instant lastSeen = Instant.now().minusSeconds(rnd.nextLong(0, 10 * 60));

            String name = "Game Server-" + hostNumber;

            servers.add(
                    new GameServerDTO(
                            UUID.randomUUID(),
                            name,
                            uri,
                            maxPlayers,
                            currentPlayerCount,
                            status,
                            lastSeen));
        }

        return servers;
    }

    public static GameServerDTO createInvalidGameServersDTO() {
        return new GameServerDTO(
                UUID.randomUUID(),
                "Invalid Game Server",
                URI.create("ws://invalid.example.com:9000/game"),
                4,
                -1,
                GameStatus.FINISHED,
                Instant.now().minusSeconds(10 * 60));
    }

    private static GameStatus randomStatus(ThreadLocalRandom rnd) {
        GameStatus[] statuses = GameStatus.values();
        return statuses[rnd.nextInt(statuses.length)];
    }
}
