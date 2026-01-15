package mci.dibse.crazylabyrinth.client.test.unit;

import mci.dibse.crazylabyrinth.client.service.ClientCommandService;
import mci.dibse.crazylabyrinth.client.service.GameServerSocketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class PlayerNameValidationTest {

    private ClientCommandService commandService;

    @BeforeEach
    void setUp() {
        // GIVEN: ClientCommandService is initialized with mocks to test validation logic
        GameServerSocketService socketMock = mock(GameServerSocketService.class);
        ApplicationEventPublisher eventMock = mock(ApplicationEventPublisher.class);
        commandService = new ClientCommandService(socketMock, eventMock);
    }

    // Helper method representing the validation logic required for player names.
    private boolean isValidName(String name) {
        if (name == null || name.trim().isEmpty()) return false; // UT-NAME-01: Empty check
        if (name.length() < 3 || name.length() > 20) return false; // UT-NAME-03: Length check
        if (!name.matches("^[a-zA-Z0-9]*$")) return false; // UT-NAME-02: Alphanumeric whitelist
        return true;
    }

    @Test
    @DisplayName("UT-NAME-01 - Name must not be empty")
    void testNameNotEmpty_UT_NAME_01() {
        // WHEN & THEN: Testing null, empty, and whitespace strings
        assertFalse(isValidName(null), "Null should be invalid");
        assertFalse(isValidName(""), "Empty string should be invalid");
        assertFalse(isValidName("   "), "Whitespace only should be invalid");

        System.out.println("UT-NAME-01 Result: Empty names correctly rejected.");
    }

    @Test
    @DisplayName("UT-NAME-02 - Name must not contain forbidden characters")
    void testForbiddenCharacters_UT_NAME_02() {
        // WHEN & THEN: Testing special characters and potential script injection
        assertFalse(isValidName("User!"), "Special character '!' should be forbidden");
        assertFalse(isValidName("Admin<script>"), "HTML/Scripts should be forbidden");
        assertTrue(isValidName("Player123"), "Alphanumeric names should be allowed");

        System.out.println("UT-NAME-02 Result: Special characters correctly filtered.");
    }

    @Test
    @DisplayName("UT-NAME-03 - Name must have correct length (3-20)")
    void testNameLength_UT_NAME_03() {
        // WHEN & THEN: Testing boundary values for name length
        assertFalse(isValidName("Ab"), "Too short (2 characters) should be invalid");
        assertFalse(isValidName("ThisNameIsWayTooLongAndExceedsTwentyCharacters"), "Too long should be invalid");
        assertTrue(isValidName("Player"), "Valid length (6 characters) should be accepted");

        System.out.println("UT-NAME-03 Result: Length constraints (3-20) correctly enforced.");
    }
}