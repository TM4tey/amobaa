package amoba.db;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Egyszerű TXT-alapú score tároló.
 * Formátum: soronként "név;pontszám"
 * Példa:
 *   Játékos;3
 *   Gép;5
 */
public class ScoreRepository {

    private final Path file;

    public ScoreRepository() {
        this(Path.of("scores.txt"));
    }

    public ScoreRepository(Path file) {
        this.file = file;
    }

    /**
     * Pontszám növelése adott névhez (1-gyel).
     */
    public synchronized void incrementWin(String name) throws IOException {
        Map<String, Integer> scores = loadAll();
        int current = scores.getOrDefault(name, 0);
        scores.put(name, current + 1);
        saveAll(scores);
    }

    /**
     * Összes score betöltése (név -> pontszám), beolvasási sorrendet megőrzi.
     */
    public synchronized Map<String, Integer> loadAll() throws IOException {
        Map<String, Integer> result = new LinkedHashMap<>();
        if (!Files.exists(file)) {
            return result;
        }
        for (String line : Files.readAllLines(file)) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            int sep = trimmed.indexOf(';');
            if (sep < 0) {
                // rossz sor -> kihagyjuk
                continue;
            }
            String name = trimmed.substring(0, sep).trim();
            String val = trimmed.substring(sep + 1).trim();
            try {
                int wins = Integer.parseInt(val);
                if (!name.isEmpty() && wins >= 0) {
                    result.put(name, wins);
                }
            } catch (NumberFormatException ignored) {
                // hibás szám -> kihagyjuk
            }
        }
        return result;
    }

    /**
     * Összes score mentése txt-be.
     */
    public synchronized void saveAll(Map<String, Integer> scores) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> e : scores.entrySet()) {
            sb.append(e.getKey())
                    .append(';')
                    .append(e.getValue())
                    .append('\n');
        }
        Files.writeString(file, sb.toString());
    }
}