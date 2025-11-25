package amoba.ai;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import amoba.board.Board;
import amoba.model.Position;

public class RandomAI {

    private final SecureRandom random = new SecureRandom();

    public Position chooseMove(Board board) {
        Set<Position> legal = board.legalPositionsByAdjacency();
        if (legal.isEmpty()) {
            return null;
        }
        List<Position> list = new ArrayList<>(legal);
        return list.get(random.nextInt(list.size()));
    }
}