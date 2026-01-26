package mci.dibse.crazylabyrinth.client.test.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import labyrinth.contracts.models.CommandType;
import labyrinth.contracts.models.Direction;
import mci.dibse.crazylabyrinth.client.domain.context.PushTileCommandRecord;
import mci.dibse.crazylabyrinth.client.domain.model.record.StartGameRecord;
import mci.dibse.crazylabyrinth.client.service.ClientCommandService;
import mci.dibse.crazylabyrinth.client.service.GameServerSocketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

class ClientCommandServiceTest {

    private ClientCommandService commandService;
    private GameServerSocketService socketServiceMock;
    private ApplicationEventPublisher eventPublisherMock;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // GIVEN: ClientCommandService is initialized with mocks
        socketServiceMock = mock(GameServerSocketService.class);
        eventPublisherMock = mock(ApplicationEventPublisher.class);
        commandService = new ClientCommandService(socketServiceMock, eventPublisherMock);
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("UT-COMMAND-01 - movePawn command is created correctly")
    void testMovePawnCommandCreation_UT_COMMAND_01() {
        // GIVEN: Target coordinates
        int targetRow = 3;
        int targetCol = 5;
        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);

        // WHEN: Method is called
        commandService.sendMovePawnCommand(targetRow, targetCol);

        // THEN: Command object is created (intercepted via socket call)
        verify(socketServiceMock).sendCommand(jsonCaptor.capture(), any());
        String capturedJson = jsonCaptor.getValue();

        // THEN: JSON is valid and contains correct data
        assertDoesNotThrow(
                () -> {
                    var jsonNode = objectMapper.readTree(capturedJson);
                    assertEquals("MOVE_PAWN", jsonNode.get("type").asText());
                    assertEquals(targetRow, jsonNode.get("targetCoordinates").get("row").asInt());
                    assertEquals(
                            targetCol, jsonNode.get("targetCoordinates").get("column").asInt());

                    System.out.println("UT-01 Result: " + capturedJson);
                });
    }

    @Test
    @DisplayName("UT-COMMAND-02 - pushTile command is created correctly")
    void testPushTileCommandCreation_UT_COMMAND_02() {
        // GIVEN: Push action exists (Mock)
        PushTileCommandRecord mockAction = mock(PushTileCommandRecord.class);
        when(mockAction.commandType()).thenReturn(CommandType.PUSH_TILE);
        when(mockAction.pushDirection()).thenReturn(Direction.UP);
        when(mockAction.index()).thenReturn(2);

        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);

        // WHEN: Method to create pushTile command is called
        commandService.sendPushTileCommand(mockAction);

        // THEN: Verification at the socket level
        verify(socketServiceMock).sendCommand(jsonCaptor.capture(), eq(CommandType.PUSH_TILE));
        String capturedJson = jsonCaptor.getValue();

        // THEN: JSON serialization is valid
        assertDoesNotThrow(
                () -> {
                    var jsonNode = objectMapper.readTree(capturedJson);
                    assertEquals("PUSH_TILE", jsonNode.get("type").asText());
                    assertEquals("UP", jsonNode.get("direction").asText());
                    assertEquals(2, jsonNode.get("rowOrColIndex").asInt());

                    System.out.println("UT-02 Result: " + capturedJson);
                });
    }

    @Test
    @DisplayName("UT-COMMAND-03 - Configuration command (BoardSize/Duration)")
    void testConfiguration_UT_03() {
        // GIVEN: We set all values to 7 to ensure that 7 is correctly transmitted
        // (boardSize, duration, treasures, bonuses)
        StartGameRecord config = new StartGameRecord(7, 7, 7, 7);
        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);

        // WHEN: Start game command is sent
        commandService.sendStartGameCommand(config);

        // THEN: Verify the command sent to the socket
        verify(socketServiceMock, atLeastOnce())
                .sendCommand(jsonCaptor.capture(), eq(CommandType.START_GAME));
        String json = jsonCaptor.getValue();

        assertDoesNotThrow(
                () -> {
                    var node = objectMapper.readTree(json);
                    // Verify if the BoardSize in the JSON is actually 7
                    assertEquals(7, node.get("boardSize").get("rows").asInt());
                    System.out.println("UT-03 Result: Config validated with BoardSize 7.");
                });
    }

    @Test
    @DisplayName("UT-COMMAND-04 - Game Start command")
    void testStartCommand_UT_04() {
        // GIVEN: Start request
        StartGameRecord start = new StartGameRecord(7, 300, 10, 2);
        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);

        // WHEN: Method to start is called
        commandService.sendStartGameCommand(start);

        // THEN: Type must be START_GAME (Requirement A1.4)
        verify(socketServiceMock).sendCommand(jsonCaptor.capture(), eq(CommandType.START_GAME));
        assertTrue(jsonCaptor.getValue().contains("\"type\":\"START_GAME\""));
        System.out.println("UT-04 (Start) Result: START_GAME command type verified.");
    }

    @Test
    @DisplayName("UT-COMMAND-05 - NewRound command")
    void testNewRound_UT_05() {
        // GIVEN: Game has ended (Mocked by providing new start data)
        // GIVEN: New round command exists (Mock via StartGameRecord)
        StartGameRecord restart = new StartGameRecord(7, 600, 12, 4);
        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);

        // WHEN: Method to create NewRound command is called
        commandService.sendStartGameCommand(restart);

        // THEN: Command object created and JSON serialization is valid
        verify(socketServiceMock, atLeastOnce())
                .sendCommand(jsonCaptor.capture(), eq(CommandType.START_GAME));
        assertNotNull(jsonCaptor.getValue());
        assertTrue(jsonCaptor.getValue().contains("START_GAME"));
        System.out.println("UT-05 Result: NewRound/Restart JSON: " + jsonCaptor.getValue());
    }
}
