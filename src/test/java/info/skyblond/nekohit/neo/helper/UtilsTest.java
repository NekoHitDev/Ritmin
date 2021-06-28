package info.skyblond.nekohit.neo.helper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UtilsTest {
    @Test
    void testRequireTrue() {
        assertDoesNotThrow(() -> Utils.require(true, ""));
    }

    @Test
    void testRequireFalse() {
        var message = "message";
        var t = assertThrows(
            Exception.class,
            () -> Utils.require(false, message)
        );
        assertTrue(t.getMessage().contains(message), "Unknown error: " + t.getMessage());
    }
}