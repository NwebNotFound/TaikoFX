package de.thu.taiko2d.io;

// AI-generated (Claude Code, 2026-11-09). See docs/AI_USAGE.md.

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.thu.taiko2d.model.Leaderboard;
import de.thu.taiko2d.model.ScoreEntry;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ScoreStoreTest {
    @TempDir Path dir;

    @Test
    void missingFileGivesAnEmptyBoard() throws Exception {
        assertTrue(new ScoreStore(dir.resolve("none.json")).load().top("a", 5).isEmpty());
    }

    @Test
    void savedScoresLoadBack() throws Exception {
        ScoreStore store = new ScoreStore(dir.resolve("sub").resolve("scores.json"));
        Leaderboard board = new Leaderboard();
        ScoreEntry entry = new ScoreEntry(123456, 97.5, 321, 300, 20, 1, "A", 1_700_000_000_000L);
        board.add("song/charts/hard.json", entry);
        store.save(board);
        assertEquals(entry, store.load().top("song/charts/hard.json", 1).getFirst());
    }

    @Test
    void brokenFileIsMovedAsideNotLost() throws Exception {
        Path file = dir.resolve("scores.json");
        Files.writeString(file, "{ this is not json");
        assertTrue(new ScoreStore(file).load().top("a", 1).isEmpty());
        assertTrue(Files.exists(dir.resolve("scores.json.broken")));
    }

    @Test
    void keyUsesFolderNameAndForwardSlashes() {
        assertEquals("osu-song/charts/hard.json", ScoreStore.key(Path.of("examples", "osu-song"), "charts\\hard.json"));
    }
}
