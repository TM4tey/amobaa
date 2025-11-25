package amoba.model;


public record Position(int row, int col) {
    public Position {
        if (row < 0 || col < 0) {
            throw new IllegalArgumentException("A sor és oszlop index nem lehet negatív!");
        }
    }
}