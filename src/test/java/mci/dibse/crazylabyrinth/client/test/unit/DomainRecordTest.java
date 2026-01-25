package mci.dibse.crazylabyrinth.client.test.unit;

import mci.dibse.crazylabyrinth.client.domain.model.record.*;
import mci.dibse.crazylabyrinth.client.domain.context.PushTileCommandRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.net.URI;
import static org.junit.jupiter.api.Assertions.*;

class DomainRecordTest {

    @Test
    @DisplayName("UT-DOMAIN-01 - Verify domain records integrity")
    void testDomainRecords_UT_DOMAIN_01() {
        // GIVEN: Valid test data
        URI uri = URI.create("ws://localhost:8080");

        // WHEN: Records are created
        GameInfoRecord gameInfo = new GameInfoRecord(uri, "P1", "Lobby", "4");
        StartGameRecord startGame = new StartGameRecord(12, 5, 600, 7);
        PushTileCommandRecord push = new PushTileCommandRecord(1, labyrinth.contracts.models.Direction.UP, new labyrinth.contracts.models.Direction[]{}, labyrinth.contracts.models.CommandType.PUSH_TILE);

        // THEN: Integrity check
        assertAll("Record Validation",
                () -> assertEquals(uri, gameInfo.uri()),
                () -> assertEquals(7, startGame.boardSize()),
                () -> assertEquals(labyrinth.contracts.models.Direction.UP, push.pushDirection())
        );
        System.out.println("UT-DOMAIN-01 Result: All records verified.");
    }
}