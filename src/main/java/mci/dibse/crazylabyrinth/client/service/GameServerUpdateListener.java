package mci.dibse.crazylabyrinth.client.service;

import java.util.Collection;
import mci.dibse.crazylabyrinth.client.domain.model.GameServer;

/**
 *  Interface for ModelView to get notified about GameServer updates
 *  @author Julian Janetschek
 */
public interface GameServerUpdateListener {
    void onServersUpdated(Collection<GameServer> servers);
}
