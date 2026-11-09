package de.thu.taiko2d.io;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import de.thu.taiko2d.model.Leaderboard;
import de.thu.taiko2d.model.ScoreEntry;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

/**
 * Saves the local leaderboard as JSON in the user's home folder. AI-generated 2026-11-09;
 * see docs/AI_USAGE.md. A broken file is moved aside (scores.json.broken) instead of being
 * overwritten, so no scores are silently lost.
 */
public final class ScoreStore {
    private static final int FORMAT_VERSION = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** File layout: {"formatVersion": 1, "scores": {"map-folder/charts/x.json": [ ... ]}}. */
    private record FileFormat(int formatVersion, Map<String, List<ScoreEntry>> scores) {}

    private final Path file;

    public ScoreStore(Path file) { this.file = file; }

    /** ~/.taikofx/scores.json */
    public static Path defaultFile() {
        return Path.of(System.getProperty("user.home"), ".taikofx", "scores.json");
    }

    /** Key of a chart: map folder name + chart path, e.g. "osu-song/charts/hard.json". */
    public static String key(Path mapFolder, String chart) {
        return mapFolder.getFileName() + "/" + chart.replace('\\', '/');
    }

    public Leaderboard load() throws IOException {
        if (!Files.exists(file)) return new Leaderboard();
        try (Reader reader = Files.newBufferedReader(file)) {
            FileFormat data = GSON.fromJson(reader, FileFormat.class);
            if (data == null || data.scores() == null) return new Leaderboard();
            return new Leaderboard(data.scores());
        } catch (JsonParseException | IllegalStateException e) {
            Files.move(file, file.resolveSibling(file.getFileName() + ".broken"), StandardCopyOption.REPLACE_EXISTING);
            System.err.println("Scores file was broken, moved aside: " + e.getMessage());
            return new Leaderboard();
        }
    }

    /** Writes to a temporary file first, then replaces the old file. */
    public void save(Leaderboard board) throws IOException {
        Path dir = file.toAbsolutePath().getParent();
        Files.createDirectories(dir);
        Path temp = Files.createTempFile(dir, "scores", ".tmp");
        try (Writer writer = Files.newBufferedWriter(temp)) {
            GSON.toJson(new FileFormat(FORMAT_VERSION, board.asMap()), writer);
        }
        try {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
