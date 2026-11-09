package de.thu.taiko2d.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Local high scores, one list per chart (key: map folder + chart file). Pure model, saved by
 * {@code io.ScoreStore}. AI-generated 2026-11-09; see docs/AI_USAGE.md.
 *
 * Order: higher score first, then higher accuracy, then the older play (a tie does not
 * push an existing score down). Only the best {@link #KEEP} per chart are kept.
 */
public final class Leaderboard {
    public static final int KEEP = 20;

    static final Comparator<ScoreEntry> ORDER = Comparator.comparingLong(ScoreEntry::score).reversed()
            .thenComparing(Comparator.comparingDouble(ScoreEntry::accuracy).reversed())
            .thenComparingLong(ScoreEntry::playedAtMs);

    private final Map<String, List<ScoreEntry>> scores = new TreeMap<>();

    public Leaderboard() {}

    /** Builds a leaderboard from saved lists (any order, any length). */
    public Leaderboard(Map<String, List<ScoreEntry>> saved) {
        saved.forEach((key, list) -> list.forEach(entry -> add(key, entry)));
    }

    /** Adds a play; returns its rank (1 = best) or 0 if it did not make the top {@link #KEEP}. */
    public int add(String key, ScoreEntry entry) {
        List<ScoreEntry> list = scores.computeIfAbsent(key, k -> new ArrayList<>());
        int index = 0;
        while (index < list.size() && ORDER.compare(list.get(index), entry) <= 0) index++;
        if (index >= KEEP) return 0;
        list.add(index, entry);
        if (list.size() > KEEP) list.removeLast();
        return index + 1;
    }

    /** The best {@code n} plays of a chart, best first. */
    public List<ScoreEntry> top(String key, int n) {
        List<ScoreEntry> list = scores.getOrDefault(key, List.of());
        return List.copyOf(list.subList(0, Math.min(n, list.size())));
    }

    /** Copy of all lists, for saving. */
    public Map<String, List<ScoreEntry>> asMap() {
        Map<String, List<ScoreEntry>> copy = new TreeMap<>();
        scores.forEach((key, list) -> copy.put(key, List.copyOf(list)));
        return copy;
    }
}
