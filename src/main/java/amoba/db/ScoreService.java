package amoba.db;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Score szolgáltatás – TXT alapú tárolás.
 */
public class ScoreService {

    private final ScoreRepository repo = new ScoreRepository();

    public void recordWin(String name) {
        try {
            repo.incrementWin(name);
        } catch (IOException e) {
            // I/O hibát lenyelünk és logolhatnánk, ha lenne logger itt
            // A játék folytatható mentés nélkül is.
        }
    }

    /**
     * Top N score (név -> pont), beszúrási sorrendhez igazodva, pont szerint csökkenő rendezéssel.
     */
    public Map<String, Integer> highScoresTop(int limit) {
        try {
            Map<String, Integer> all = repo.loadAll();
            return all.entrySet().stream()
                    .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                    .limit(limit)
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (x, y) -> x,
                            LinkedHashMap::new
                    ));
        } catch (IOException e) {
            return Map.of();
        }
    }
}