package amoba.db;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ScoreService – top N és recordWin viselkedés.
 * A ScoreService repo-ját átirányítjuk ideiglenes fájlra reflect segítséggel,
 * hogy ne a default "scores.txt"-t használja.
 */
class ScoreServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void recordWin_and_highScoresTop_sortedAndLimited() throws Exception {
        Path file = tempDir.resolve("scores.txt");
        ScoreRepository repo = new ScoreRepository(file);
        ScoreService svc = new ScoreService();

        // Inject custom repo (reflection, hogy a temp fájlt használja)
        Field f = ScoreService.class.getDeclaredField("repo");
        f.setAccessible(true);
        f.set(svc, repo);

        // Rögzítünk győzelmeket
        svc.recordWin("Jani");
        svc.recordWin("Jani");
        svc.recordWin("Géza");
        svc.recordWin("Béla");
        svc.recordWin("Béla");
        svc.recordWin("Béla");

        Map<String, Integer> top2 = svc.highScoresTop(2);
        assertEquals(2, top2.size());
        // Charlie 3, Alice 2, Bob 1 → top2: Charlie, Alice
        assertTrue(top2.containsKey("Béla"));
        assertTrue(top2.containsKey("Jani"));
        assertEquals(3, top2.get("Béla"));
        assertEquals(2, top2.get("Jani"));
    }

    @Test
    void highScoresTop_emptyOnIOException() throws Exception {
        // Átirányítjuk repo-t egy nem létező könyvtárba, hogy IO hibát kapjunk (nem feltétlen garantált minden OS-en)
        // Alternatíva: mock, de itt maradunk egyszerű IO-nál.
        Path nonExistingDir = tempDir.resolve("no_dir").resolve("scores.txt");
        ScoreRepository badRepo = new ScoreRepository(nonExistingDir);
        ScoreService svc = new ScoreService();
        Field f = ScoreService.class.getDeclaredField("repo");
        f.setAccessible(true);
        f.set(svc, badRepo);

        Map<String, Integer> top = svc.highScoresTop(10);
        assertTrue(top.isEmpty());
    }
}