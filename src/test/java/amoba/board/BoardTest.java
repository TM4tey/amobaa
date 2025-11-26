package amoba.board;

import amoba.model.Cell;
import amoba.model.Position;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BoardTest {

    @Test
    @DisplayName("Üres tábla - nincs legális lépés (nincs lerakott jel)")
    void adjacency_emptyBoard_noLegalMoves() {
        Board b = new Board(10, 10);
        assertTrue(b.legalPositionsByAdjacency().isEmpty());
    }

    @Test
    @DisplayName("Első X lerakása után pontosan 8 szomszédos mező lesz legális")
    void adjacency_afterFirstMove_neighborsAreLegal() {
        Board b = new Board(10, 10);
        Position center = b.center();
        b.place(Cell.X, center);
        Set<Position> legal = b.legalPositionsByAdjacency();
        assertEquals(8, legal.size());
        // Ellenőrizzük, hogy nincs duplikáció (Set méret alapján OK)
        // Példa egy konkrét szomszédra:
        assertTrue(legal.contains(new Position(center.row() - 1, center.col() - 1)));
    }

    @Test
    @DisplayName("Több külön helyen lévő jel - adjacency nem duplikál")
    void adjacency_multipleSeeds_noDuplicates() {
        Board b = new Board(10, 10);
        b.place(Cell.X, new Position(1, 1));
        b.place(Cell.X, new Position(5, 5));
        Set<Position> legal = b.legalPositionsByAdjacency();
        // Nem ellenőrzünk pontos számot, csak hogy nincs benne a már foglalt és nincs duplikáció.
        assertFalse(legal.contains(new Position(1, 1)));
        assertFalse(legal.contains(new Position(5, 5)));
        assertTrue(legal.contains(new Position(0, 0)));
        assertTrue(legal.contains(new Position(6, 6)));
    }

    @Test
    @DisplayName("Vízszintes ötös sorozat felismerése")
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
    @DisplayName("Függőleges ötös sorozat felismerése")
    void fiveInARow_vertical() {
        Board b = new Board(10, 10);
        int c = 3;
        for (int r = 1; r <= 4; r++) {
            b.place(Cell.X, new Position(r, c));
        }
        Position last = new Position(5, c);
        b.place(Cell.X, last);
        assertTrue(b.hasFiveInARow(Cell.X, last));
    }

    @Test
    @DisplayName("Átló NW-SE ötös sorozat felismerése")
    void fiveInARow_diagonalNWSE() {
        Board b = new Board(10, 10);
        for (int i = 2; i <= 5; i++) {
            b.place(Cell.X, new Position(i, i));
        }
        Position last = new Position(6, 6);
        b.place(Cell.X, last);
        assertTrue(b.hasFiveInARow(Cell.X, last));
    }

    @Test
    @DisplayName("Átló NE-SW ötös sorozat felismerése")
    void fiveInARow_diagonalNESW() {
        Board b = new Board(10, 10);
        b.place(Cell.X, new Position(2,7));
        b.place(Cell.X, new Position(3,6));
        b.place(Cell.X, new Position(4,5));
        b.place(Cell.X, new Position(5,4));
        Position last = new Position(6,3);
        b.place(Cell.X, last);
        assertTrue(b.hasFiveInARow(Cell.X, last));
    }

    @Test
    @DisplayName("Négy egymás mellett nem győzelem")
    void fourInRow_notWin() {
        Board b = new Board(10, 10);
        // 4 vízszintes X
        for (int c = 1; c <= 4; c++) {
            b.place(Cell.X, new Position(3, c));
        }
        assertFalse(b.hasFiveInARow(Cell.X, new Position(3,4)));
    }

    @Test
    @DisplayName("Másik játékos (O) sorozat felismerése, X keveredés ne zavarjon")
    void fiveInARow_forO() {
        Board b = new Board(10, 10);
        // O-kat rakunk vegyes X-ekkel körülötte
        b.place(Cell.X, new Position(4,1));
        b.place(Cell.O, new Position(2,5));
        b.place(Cell.O, new Position(3,5));
        b.place(Cell.O, new Position(4,5));
        b.place(Cell.O, new Position(5,5));
        Position last = new Position(6,5);
        b.place(Cell.O, last);
        assertTrue(b.hasFiveInARow(Cell.O, last));
        assertFalse(b.hasFiveInARow(Cell.X, last)); // X-re természetesen nincs
    }

    @Test
    @DisplayName("Foglalt mezőre lépés hibát dob")
    void place_onOccupied_throws() {
        Board b = new Board(10, 10);
        Position p = new Position(2,2);
        b.place(Cell.X, p);
        assertThrows(IllegalArgumentException.class, () -> b.place(Cell.O, p));
    }

    @Test
    @DisplayName("Táblán kívüli lépés hibát dob")
    void place_outsideBoard_throws() {
        Board b = new Board(10, 10);
        assertThrows(IllegalArgumentException.class,
                () -> b.place(Cell.X, new Position(-1, 0)));
        assertThrows(IllegalArgumentException.class,
                () -> b.place(Cell.X, new Position(0, 10)));
    }

    @Test
    @DisplayName("toCharLines és loadFromCharLines round-trip")
    void toCharLines_loadFromCharLines_roundTrip() {
        Board b = new Board(6, 4); // 6 sor, 4 oszlop
        b.place(Cell.X, new Position(0,0));
        b.place(Cell.O, new Position(5,3));

        List<String> lines = b.toCharLines(); // pl. ['x...', '....', ..., '...o']
        assertEquals(6, lines.size());
        for (String line : lines) {
            assertEquals(4, line.length());
        }

        Board b2 = new Board(6, 4);
        b2.loadFromCharLines(lines);

        assertEquals(Cell.X, b2.getCell(new Position(0,0)));
        assertEquals(Cell.O, b2.getCell(new Position(5,3)));
        assertEquals(Cell.EMPTY, b2.getCell(new Position(1,1)));
    }

    @Test
    @DisplayName("render() tartalmazza az oszlop betűket és a sor sorszámot")
    void render_format() {
        Board b = new Board(4, 4);
        b.place(Cell.X, new Position(0,0));
        String rendered = b.render();
        // Ellenőrizzük, hogy szerepel 'a' és '4' és az 'x' karakter
        assertTrue(rendered.contains("a"));
        assertTrue(rendered.contains("4")); // utolsó sor száma
        assertTrue(rendered.contains("x"));
        // Sorok száma + fejléc ellenőrzés
        long lineCount = rendered.lines().count();
        assertTrue(lineCount >= 5); // fejléc + 4 sor
    }

    @Test
    @DisplayName("Nem győztes izolált lépésnél hasFiveInARow false")
    void isolatedMark_notWin() {
        Board b = new Board(8, 5);
        Position p = new Position(7,4);
        b.place(Cell.X, p);
        assertFalse(b.hasFiveInARow(Cell.X, p));
    }

    @Test
    @DisplayName("Hibás dimenziók – felső és alsó határok")
    void invalidDimensions_throws() {
        assertThrows(IllegalArgumentException.class, () -> new Board(3, 4));   // N túl kicsi
        assertThrows(IllegalArgumentException.class, () -> new Board(26, 5));  // N túl nagy
        assertThrows(IllegalArgumentException.class, () -> new Board(5, 6));   // M > N
        assertThrows(IllegalArgumentException.class, () -> new Board(4, 3));   // M túl kicsi
    }
}