package de.thu.taiko2d.game;

import de.thu.taiko2d.model.Note;
import de.thu.taiko2d.model.NoteType;
import java.util.Comparator;
import java.util.List;

public final class JudgementEngine {
    public static final long PERFECT_MS = 45;
    public static final long GOOD_MS = 100;

    private final List<Note> notes;          // sorted by (timeMs, id)
    private final boolean[] resolved;
    private int firstUnresolved = 0;
    private Note lastHitNote;
    private int score, combo, maxCombo, perfect, good, miss;

    public JudgementEngine(List<Note> chart) {
        if (chart.isEmpty()) {
            throw new IllegalArgumentException("Chart has no notes");   // avoids divide-by-zero in accuracy
        }
        this.notes = chart.stream()
                .sorted(Comparator.comparingLong(Note::timeMs).thenComparingInt(Note::id))
                .toList();
        this.resolved = new boolean[notes.size()];
    }

    public Judgement hit(NoteType type, double inputMs) {
        int best = -1;
        double bestError = Double.MAX_VALUE;
        for (int i = firstUnresolved; i < notes.size(); i++) {
            Note note = notes.get(i);
            if (note.timeMs() - inputMs > GOOD_MS) break;            // sorted: nothing later can match
            if (resolved[i] || note.noteType() != type) continue;
            double error = Math.abs(inputMs - note.timeMs());
            if (error <= GOOD_MS && error < bestError) {             // strict <: earlier note wins ties
                best = i;
                bestError = error;
            }
        }
        if (best < 0) return null;
        resolved[best] = true;
        lastHitNote = notes.get(best);
        advance();
        Judgement result = bestError <= PERFECT_MS ? Judgement.PERFECT : Judgement.GOOD;
        record(result);
        return result;
    }

    public int update(double songMs) {
        int missed = 0;
        for (int i = firstUnresolved; i < notes.size(); i++) {
            if (songMs - notes.get(i).timeMs() <= GOOD_MS) break;    // sorted: the rest are still in the future
            if (!resolved[i]) {
                resolved[i] = true;
                record(Judgement.MISS);
                missed++;
            }
        }
        advance();
        return missed;
    }

    private void advance() {
        while (firstUnresolved < notes.size() && resolved[firstUnresolved]) firstUnresolved++;
    }

    private void record(Judgement j) {
        switch (j) {
            case PERFECT -> { score += 300; combo++; perfect++; }
            case GOOD    -> { score += 100; combo++; good++; }
            case MISS    -> { combo = 0; miss++; }
        }
        maxCombo = Math.max(maxCombo, combo);
    }

    public double accuracy() {
        return 100.0 * (300 * perfect + 100 * good) / (300.0 * notes.size());
    }

    /** The note matched by the most recent successful {@link #hit}, or null. */
    public Note lastHitNote() { return lastHitNote; }
    public int score() { return score; }
    public int combo() { return combo; }
    public int maxCombo() { return maxCombo; }
    public int perfect() { return perfect; }
    public int good() { return good; }
    public int miss() { return miss; }
    public int total() { return notes.size(); }
}