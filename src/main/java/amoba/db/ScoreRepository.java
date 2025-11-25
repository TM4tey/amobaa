package amoba.db;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"PMD.UseConcurrentHashMap"})
public class ScoreRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScoreRepository.class);

    private static final String JDBC_URL = "jdbc:h2:./amoba-db;AUTO_SERVER=TRUE";
    private static final String JDBC_USER = "sa";
    private static final String JDBC_PASS = "";
    private static final Path FILE_STORE = Path.of("highscore.txt");
    private static final int PARTS_EXPECTED = 2;

    private boolean useFileStore;

    public ScoreRepository() {
        detectBackend();
    }

    private void detectBackend() {
        try (Connection c = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS);
             Statement st = c.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS PLAYERS(NAME VARCHAR(255) PRIMARY KEY, WINS INT NOT NULL)");
            useFileStore = false;
            LOGGER.info("High score: H2 adatbázis használatban.");
        } catch (SQLException e) {
            useFileStore = true;
            LOGGER.warn("H2 nem elérhető. Fallback: fájl.");
            ensureFileInitialized();
        }
    }

    public void recordWin(String playerName) {
        if (playerName == null || playerName.isBlank()) {
            return;
        }
        if (useFileStore) {
            Map<String, Integer> map = readFileScores();
            map.put(playerName, map.getOrDefault(playerName, 0) + 1);
            writeFileScores(map);
            return;
        }
        try (Connection c = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS);
             PreparedStatement up = c.prepareStatement(
                     "MERGE INTO PLAYERS KEY(NAME) VALUES(?, COALESCE((SELECT WINS FROM PLAYERS WHERE NAME=?)+1, 1))")) {
            up.setString(1, playerName);
            up.setString(2, playerName);
            up.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("recordWin hiba (DB). Fallback fájlra.", e);
            useFileStore = true;
            recordWin(playerName);
        }
    }

    public Map<String, Integer> loadHighScoresTop(int limit) {
        final int effectiveLimit = (limit <= 0) ? 10 : limit;
        if (useFileStore) {
            Map<String, Integer> map = readFileScores();
            return map.entrySet().stream()
                    .sorted((a, b) -> {
                        int cmp = Integer.compare(b.getValue(), a.getValue());
                        if (cmp != 0) {
                            return cmp;
                        }
                        return a.getKey().compareToIgnoreCase(b.getKey());
                    })
                    .limit(effectiveLimit)
                    .collect(LinkedHashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), LinkedHashMap::putAll);
        }
        Map<String, Integer> result = new LinkedHashMap<>();
        try (Connection c = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS);
             PreparedStatement ps = c.prepareStatement(
                     "SELECT NAME, WINS FROM PLAYERS ORDER BY WINS DESC, NAME ASC LIMIT ?")) {
            ps.setInt(1, effectiveLimit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getString(1), rs.getInt(2));
                }
            }
        } catch (SQLException e) {
            LOGGER.error("High score lekérdezési hiba (DB). Fallback fájlra.", e);
            useFileStore = true;
            return loadHighScoresTop(limit);
        }
        return result;
    }

    private void ensureFileInitialized() {
        if (!Files.exists(FILE_STORE)) {
            try {
                Files.writeString(FILE_STORE, "");
            } catch (IOException e) {
                LOGGER.warn("highscore.txt létrehozási hiba.", e);
            }
        }
    }

    private Map<String, Integer> readFileScores() {
        ensureFileInitialized();
        Map<String, Integer> map = new LinkedHashMap<>();
        try {
            for (String line : Files.readAllLines(FILE_STORE)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                String[] parts = trimmed.split(";", PARTS_EXPECTED);
                if (parts.length == PARTS_EXPECTED) {
                    map.put(parts[0], Integer.parseInt(parts[1]));
                }
            }
        } catch (IOException | NumberFormatException e) {
            LOGGER.warn("High score fájl olvasási hiba.", e);
        }
        return map;
    }

    private void writeFileScores(Map<String, Integer> map) {
        StringBuilder sb = new StringBuilder(map.size() * 10);
        for (Map.Entry<String, Integer> e : map.entrySet()) {
            sb.append(e.getKey()).append(';').append(e.getValue()).append('\n');
        }
        try {
            Files.writeString(FILE_STORE, sb.toString());
        } catch (IOException e) {
            LOGGER.warn("High score fájl írási hiba.", e);
        }
    }
}