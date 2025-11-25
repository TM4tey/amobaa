package amoba.io;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Scanner;

import amoba.model.Position;

public class ConsoleUI {

    private static final int MIN_LEN_FOR_POSITION = 2;
    private final Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);

    public void println(String s) {
        System.out.println(s);
    }

    public void print(String s) {
        System.out.print(s);
    }

    public String ask(String prompt) {
        print(prompt);
        return scanner.nextLine();
    }

    public Position parsePosition(String input, int maxRows, int maxCols) {
        if (input == null) {
            throw new IllegalArgumentException("Üres bemenet!");
        }
        String s = input.trim().toLowerCase(Locale.ROOT);
        if (s.length() < MIN_LEN_FOR_POSITION) {
            throw new IllegalArgumentException("Add meg a lépést pl.: b3");
        }
        char colChar = s.charAt(0);
        validateColumn(colChar, maxCols);
        int rowIndex = parseRow(s.substring(1), maxRows);
        int colIndex = colChar - 'a';
        return new Position(rowIndex, colIndex);
    }

    private void validateColumn(char colChar, int maxCols) {
        if (colChar < 'a' || colChar >= 'a' + maxCols) {
            throw new IllegalArgumentException("Ismeretlen oszlop betű!");
        }
    }

    private int parseRow(String rowPart, int maxRows) {
        final int rowNum;
        try {
            rowNum = Integer.parseInt(rowPart);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("A sor legyen szám! Példa: b3", e);
        }
        int idx = rowNum - 1;
        if (idx < 0 || idx >= maxRows) {
            throw new IllegalArgumentException("A megadott sor kívül esik a táblán!");
        }
        return idx;
    }
}