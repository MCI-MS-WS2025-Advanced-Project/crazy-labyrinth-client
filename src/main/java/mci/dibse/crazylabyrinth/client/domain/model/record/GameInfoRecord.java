package mci.dibse.crazylabyrinth.client.domain.model.record;

import java.net.URI;

public record GameInfoRecord(
        URI uri,
        String playerName,
        String serverName
) {
}

