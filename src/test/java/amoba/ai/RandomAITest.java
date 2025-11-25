package amoba.ai;

import static org.junit.jupiter.api.Assertions.*;

import amoba.board.Board;
import amoba.model.Cell;
import amoba.model.Position;
import org.junit.jupiter.api.Test;

class RandomAITest {

    @Test
    void chooseMove_returnsLegalPosition() {
        Board b = new Board(10, 10);
        Position center = b.center();
        b.place(Cell.X, center);

        RandomAI ai = new RandomAI();
        Position p = ai.chooseMove(b);
        assertNotNull(p);
        assertTrue(b.legalPositionsByAdjacency().contains(p));
    }
}