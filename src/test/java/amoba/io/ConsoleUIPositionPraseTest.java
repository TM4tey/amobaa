package amoba.io;

import static org.junit.jupiter.api.Assertions.*;

import amoba.model.Position;
import org.junit.jupiter.api.Test;

class ConsoleUIPositionPraseTest {

    @Test
    void parse_valid() {
        ConsoleUI ui = new ConsoleUI();
        Position p = ui.parsePosition("b3", 10, 10);
        assertEquals(2, p.row());
        assertEquals(1, p.col());
    }

    @Test
    void parse_invalidColumn_throws() {
        ConsoleUI ui = new ConsoleUI();
        assertThrows(IllegalArgumentException.class, () -> ui.parsePosition("z9", 10, 10));
    }

    @Test
    void parse_invalidRow_throws() {
        ConsoleUI ui = new ConsoleUI();
        assertThrows(IllegalArgumentException.class, () -> ui.parsePosition("b0", 10, 10));
    }
}