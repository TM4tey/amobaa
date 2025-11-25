package amoba.db;

import java.util.Map;


public class ScoreService {

    private final ScoreRepository repo;

    public ScoreService() {
        this.repo = new ScoreRepository();
    }

    public ScoreService(ScoreRepository repo) {
        this.repo = repo;
    }

    public void recordWin(String playerName) {
        repo.recordWin(playerName);
    }

    public Map<String, Integer> highScoresTop(int limit) {
        return repo.loadHighScoresTop(limit);
    }
}