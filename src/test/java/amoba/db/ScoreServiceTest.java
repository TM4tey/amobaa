package amoba.db;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ScoreServiceTest {

    @Test
    void recordWin_delegatesToRepo() {
        ScoreRepository repo = mock(ScoreRepository.class);
        ScoreService service = new ScoreService(repo);

        service.recordWin("Alice");
        verify(repo, times(1)).recordWin("Alice");
    }

    @Test
    void highScoresTop_returnsDataFromRepo() {
        ScoreRepository repo = mock(ScoreRepository.class);
        Map<String, Integer> mockData = new LinkedHashMap<>();
        mockData.put("Alice", 3);
        mockData.put("Bob", 2);
        when(repo.loadHighScoresTop(10)).thenReturn(mockData);

        ScoreService service = new ScoreService(repo);
        Map<String, Integer> res = service.highScoresTop(10);

        assertEquals(2, res.size());
        assertEquals(3, res.get("Alice"));
        assertEquals(2, res.get("Bob"));
    }
}