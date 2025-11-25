package amoba.board;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

import amoba.model.Cell;
import amoba.model.Position;
import org.junit.jupiter.api.Test;

class BoardTest {

    @Test
    void adjacency_emptyBoard_noLegalMoves() {
        Board b = new Board(10, 10);
        assertTrue(b.legalPositionsByAdjacency().isEmpty());
    }

    @Test
    void adjacency_afterFirstMove_neighborsAreLegal() {
        Board b = new Board(10, 10);
        Position center = b.center();
        b.place(Cell.X, center);
        Set<Position> legal = b.legalPositionsByAdjacency();
        assertEquals(8, legal.size());
    }

    @Test
    void fiveInARow_horizontal() {
        Board b = new Board(10, 10);
        int r = 5;
        for (int c = 2; c <= 5; c++) {
            b.place(Cell.X, new Position(r, c));
        }
        Position last = new Position(r, 6);
        b.place(Cell.X, last);
        assertTrue(b.hasFiveInARow(Cell.X, last));
    }

    @Test
    void invalidDimensions_throws() {
        assertThrows(IllegalArgumentException.class, () -> new Board(3, 4));
        assertThrows(IllegalArgumentException.class, () -> new Board(26, 5));
    }
}