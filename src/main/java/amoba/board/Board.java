package amoba.board;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import amoba.model.Cell;
import amoba.model.Position;

@SuppressWarnings("PMD.TooManyMethods")
public class Board {

    private static final int WIN_LENGTH = 5;

    private final int rowCount;
    private final int colCount;
    private final Cell[][] grid;

    // Rövidített tömb inicializálás
    private static final int[][] DIRS = {
            {-1, -1}, {-1, 0}, {-1, 1},
            {0, -1},           {0, 1},
            {1, -1},  {1, 0},  {1, 1}
    };

    public Board(int rows, int cols) {
        if (cols < 4 || rows < cols || rows > 25) {
            throw new IllegalArgumentException("4 <= M <= N <= 25 feltételt sérti! (N=sorok, M=oszlopok)");
        }
        this.rowCount = rows;
        this.colCount = cols;
        this.grid = new Cell[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                grid[r][c] = Cell.EMPTY;
            }
        }
    }

    public int rows() {
        return rowCount;
    }

    public int cols() {
        return colCount;
    }

    public Cell getCell(Position p) {
        return grid[p.row()][p.col()];
    }

    public boolean isInside(int r, int c) {
        return r >= 0 && r < rowCount && c >= 0 && c < colCount;
    }

    public boolean isEmpty(Position p) {
        return getCell(p) == Cell.EMPTY;
    }

    public void place(Cell cell, Position p) {
        if (!isInside(p.row(), p.col())) {
            throw new IllegalArgumentException("Pozíción kívülre nem lehet rakni!");
        }
        if (!isEmpty(p)) {
            throw new IllegalArgumentException("Foglalt mezőre nem lehet rakni!");
        }
        grid[p.row()][p.col()] = cell;
    }

    public Set<Position> legalPositionsByAdjacency() {
        Set<Position> occupied = collectOccupied();
        if (occupied.isEmpty()) {
            return Set.of();
        }
        return collectAdjacentEmpty(occupied);
    }

    private Set<Position> collectOccupied() {
        Set<Position> occ = new HashSet<>();
        for (int r = 0; r < rowCount; r++) {
            for (int c = 0; c < colCount; c++) {
                if (grid[r][c] != Cell.EMPTY) {
                    occ.add(new Position(r, c));
                }
            }
        }
        return occ;
    }

    private Set<Position> collectAdjacentEmpty(Set<Position> occupied) {
        Set<Position> legal = new HashSet<>();
        for (Position pos : occupied) {
            for (int[] d : DIRS) {
                int nr = pos.row() + d[0];
                int nc = pos.col() + d[1];
                if (isInside(nr, nc) && grid[nr][nc] == Cell.EMPTY) {
                    legal.add(new Position(nr, nc));
                }
            }
        }
        return legal;
    }

    public boolean hasFiveInARow(Cell player, Position p) {
        int[][] lines = {
                {-1, 0, 1, 0},
                {0, -1, 0, 1},
                {-1, -1, 1, 1},
                {-1, 1, 1, -1}
        };
        for (int[] line : lines) {
            int count = 1;
            count += countDirection(player, p, line[0], line[1]);
            count += countDirection(player, p, line[2], line[3]);
            if (count >= WIN_LENGTH) {
                return true;
            }
        }
        return false;
    }

    private int countDirection(Cell player, Position start, int dr, int dc) {
        int r = start.row() + dr;
        int c = start.col() + dc;
        int cnt = 0;
        while (isInside(r, c) && grid[r][c] == player) {
            cnt++;
            r += dr;
            c += dc;
        }
        return cnt;
    }

    public String render() {
        StringBuilder sb = new StringBuilder();
        sb.append("   ");
        for (int c = 0; c < colCount; c++) {
            sb.append((char) ('a' + c)).append(' ');
        }
        sb.append('\n');
        for (int r = 0; r < rowCount; r++) {
            sb.append(String.format("%2d ", r + 1));
            for (int c = 0; c < colCount; c++) {
                sb.append(grid[r][c].getSymbol()).append(' ');
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    public void loadFromCharLines(List<String> lines) {
        if (lines.size() != rowCount) {
            throw new IllegalArgumentException("Hibás sor szám a betöltött táblában!");
        }
        for (int r = 0; r < rowCount; r++) {
            String line = lines.get(r);
            if (line.length() != colCount) {
                throw new IllegalArgumentException("Hibás oszlopszám a betöltött táblában (sor: " + (r + 1) + ")!");
            }
            for (int c = 0; c < colCount; c++) {
                grid[r][c] = Cell.fromChar(line.charAt(c));
            }
        }
    }

    public List<String> toCharLines() {
        List<String> lines = new ArrayList<>();
        StringBuilder sb = new StringBuilder(colCount);
        for (int r = 0; r < rowCount; r++) {
            sb.setLength(0);
            for (int c = 0; c < colCount; c++) {
                sb.append(grid[r][c].getSymbol());
            }
            lines.add(sb.toString());
        }
        return lines;
    }

    public Position center() {
        int r = rowCount / 2;
        int c = colCount / 2;
        return new Position(r, c);
    }
}