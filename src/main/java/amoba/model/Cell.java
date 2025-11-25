package amoba.model;


public enum Cell {
    EMPTY('.'),
    X('x'),
    O('o');

    private static final char X_CHAR = 'x';
    private static final char O_CHAR = 'o';

    private final char symbol;

    Cell(char symbol) {
        this.symbol = symbol;
    }

    public char getSymbol() {
        return symbol;
    }

    public static Cell fromChar(char c) {
        char n = Character.toLowerCase(c);
        if (n == X_CHAR) {
            return X;
        }
        if (n == O_CHAR) {
            return O;
        }
        return EMPTY;
    }
}