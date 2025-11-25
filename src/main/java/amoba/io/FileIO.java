package amoba.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import amoba.board.Board;

public final class FileIO {

    private static final int HEADER_PARTS = 2;

    private FileIO() { }

    public static Board loadFromTxt(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path);
        if (lines.isEmpty()) {
            throw new IOException("Üres a bemeneti fájl");
        }
        String[] dims = lines.get(0).trim().split("\\s+");
        if (dims.length != HEADER_PARTS) {
            throw new IOException("Első sor: N M formátum szükséges");
        }
        int n = Integer.parseInt(dims[0]);
        int m = Integer.parseInt(dims[1]);
        Board b = new Board(n, m);
        if (lines.size() - 1 != n) {
            throw new IOException("Nem megfelelő sor szám a táblában");
        }
        b.loadFromCharLines(lines.subList(1, lines.size()));
        return b;
    }

    public static void saveToTxt(Board board, Path path) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(board.rows()).append(' ').append(board.cols()).append('\n');
        for (String line : board.toCharLines()) {
            sb.append(line).append('\n');
        }
        Files.writeString(path, sb.toString());
    }
}