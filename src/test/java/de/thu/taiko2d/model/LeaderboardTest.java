package de.thu.taiko2d.model;

// AI-generated (Claude Code, 2026-11-09). See docs/AI_USAGE.md.

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LeaderboardTest {
    static ScoreEntry play(long score, double accuracy, long at) {
        return new ScoreEntry(score, accuracy, 10, 10, 0, 0, "S", at);
    }

    @Test
    void bestScoreFirstAndRankIsReturned() {
        Leaderboard board = new Leaderboard();
        assertEquals(1, board.add("a", play(500, 90, 1)));
        assertEquals(1, board.add("a", play(900, 90, 2)));
        assertEquals(2, board.add("a", play(700, 90, 3)));
        assertEquals(List.of(900L, 700L, 500L), board.top("a", 5).stream().map(ScoreEntry::score).toList());
        assertTrue(board.top("other", 5).isEmpty());
    }

    @Test
    void tiesGoToHigherAccuracyThenToTheOlderPlay() {
        Leaderboard board = new Leaderboard();
        board.add("a", play(500, 90, 1));
        assertEquals(1, board.add("a", play(500, 95, 2)));   // same score, better accuracy
        assertEquals(3, board.add("a", play(500, 90, 3)));   // exact tie: the older play stays ahead
        assertEquals(List.of(2L, 1L, 3L), board.top("a", 3).stream().map(ScoreEntry::playedAtMs).toList());
    }

    @Test
    void onlyTheBestAreKept() {
        Leaderboard board = new Leaderboard();
        for (int i = 0; i < Leaderboard.KEEP; i++) board.add("a", play(1000 + i, 90, i));
        assertEquals(0, board.add("a", play(1, 90, 99)));                  // too low to enter
        assertEquals(1, board.add("a", play(5000, 90, 100)));
        List<ScoreEntry> all = board.top("a", 100);
        assertEquals(Leaderboard.KEEP, all.size());
        assertEquals(1001, all.getLast().score());                         // 1000 dropped out
    }

    @Test
    void savedListsAreSortedOnLoad() {
        Leaderboard board = new Leaderboard(Map.of("a", List.of(play(1, 90, 1), play(3, 90, 2), play(2, 90, 3))));
        assertEquals(List.of(3L, 2L, 1L), board.top("a", 3).stream().map(ScoreEntry::score).toList());
    }
}
