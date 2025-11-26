package amoba.io;

import amoba.board.Board;
import amoba.model.Cell;
import amoba.model.Position;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileIOTest {

    @TempDir
    Path tempDir;

    @Test
    void saveAndLoad_roundTrip_preservesBoardState() throws IOException {
        Board board = new Board(4, 4);
        board.place(Cell.X, new Position(0,0));
        board.place(Cell.O, new Position(1,1));
        board.place(Cell.X, new Position(2,2));

        Path file = tempDir.resolve("state.txt");
        FileIO.saveToTxt(board, file);

        assertTrue(Files.exists(file), "A mentett fájlnak léteznie kell.");

        Board loaded = FileIO.loadFromTxt(file);
        assertEquals(4, loaded.rows());
        assertEquals(4, loaded.cols());
        assertEquals(Cell.X, loaded.getCell(new Position(0,0)));
        assertEquals(Cell.O, loaded.getCell(new Position(1,1)));
        assertEquals(Cell.X, loaded.getCell(new Position(2,2)));
        assertEquals(Cell.EMPTY, loaded.getCell(new Position(3,3)));
    }

    @Test
    void load_invalidHeader_nonNumeric_throws() throws IOException {
        Path file = tempDir.resolve("bad_header.txt");
        Files.writeString(file, "a 4\n....\n....\n....\n....\n");
        assertThrows(IllegalArgumentException.class, () -> FileIO.loadFromTxt(file));
    }

    @Test
    void load_missingFile_throwsIOException() {
        Path file = tempDir.resolve("does_not_exist.txt");
        assertThrows(IOException.class, () -> FileIO.loadFromTxt(file));
    }
}