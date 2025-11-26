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

/**
 * Tesztek az XmlPersistence osztályhoz.
 * Figyelem: 4 <= cols (M) <= rows (N) <= 25 a Board konstruktornál.
 */
class XmlPersistenceTest {

    @TempDir
    Path tempDir;

    @Test
    void load_invalidHeader_missingRowsAttr_throws() throws IOException {
        Path file = tempDir.resolve("bad_header.xml");
        Files.writeString(file,
                "<game cols=\"4\">\n<row>....</row>\n<row>....</row>\n<row>....</row>\n<row>....</row>\n</game>");
        assertThrows(IOException.class, () -> XmlPersistence.loadFromXml(file));
    }

    @Test
    void load_rowCountMismatch_throws() throws IOException {
        Path file = tempDir.resolve("row_count_mismatch.xml");
        Files.writeString(file,
                "<game rows=\"4\" cols=\"4\">\n<row>....</row>\n<row>....</row>\n<row>....</row>\n</game>");
        assertThrows(IOException.class, () -> XmlPersistence.loadFromXml(file));
    }

    @Test
    void load_rowWithWrongLength_throws() throws IOException {
        Path file = tempDir.resolve("wrong_length.xml");
        Files.writeString(file,
                "<game rows=\"4\" cols=\"4\">\n<row>....</row>\n<row>...</row>\n<row>....</row>\n<row>....</row>\n</game>");
        assertThrows(IOException.class, () -> XmlPersistence.loadFromXml(file));
    }
    @Test
    void load_unknownCharacter_isTreatedAsEmpty() throws IOException {
        Path file = tempDir.resolve("unknown_char.xml");
        Files.writeString(file,
                "<game rows=\"4\" cols=\"4\">\n" +
                        "<row>x?..</row>\n" +      // x ? . .
                        "<row>.o..</row>\n" +
                        "<row>....</row>\n" +
                        "<row>....</row>\n" +
                        "</game>");
        Board b = XmlPersistence.loadFromXml(file);
        assertEquals(Cell.X, b.getCell(new Position(0,0)));
        assertEquals(Cell.EMPTY, b.getCell(new Position(0,1))); // '?' -> EMPTY
        assertEquals(Cell.EMPTY, b.getCell(new Position(0,2)));
        assertEquals(Cell.EMPTY, b.getCell(new Position(0,3)));
        assertEquals(Cell.O, b.getCell(new Position(1,1)));
    }

    @Test
    void load_missingFile_throwsIOException() {
        Path file = tempDir.resolve("no.xml");
        assertThrows(IOException.class, () -> XmlPersistence.loadFromXml(file));
    }
}