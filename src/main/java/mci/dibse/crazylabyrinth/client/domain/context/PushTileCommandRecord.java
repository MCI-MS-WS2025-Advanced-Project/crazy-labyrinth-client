package mci.dibse.crazylabyrinth.client.domain.context;

import labyrinth.contracts.models.CommandType;
import labyrinth.contracts.models.Direction;

public record PushTileCommandRecord(
        int index,
        Direction pushDirection,
        Direction[] extraTileEntrances,
        CommandType commandType) {}
