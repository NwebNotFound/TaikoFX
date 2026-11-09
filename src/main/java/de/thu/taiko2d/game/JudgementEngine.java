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

    /**
     * In-order judging (AI-assisted 2026-11-09, replaces "nearest matching note", which let a
     * late press in a fast stream steal the next note and set off a chain of misses):
     * <ol>
     *   <li>Walk the pending notes inside the GOOD window in time order and hit the first one
     *       of the pressed colour. Notes that have already passed the hit line may be
     *       skipped (update() misses them later).</li>
     *   <li>A note that is still coming up blocks everything after it: a press of the
     *       other colour cannot jump over it.</li>
     *   <li>Anti-spam: if nothing was hit, the first upcoming note within MISS_MS (wrong
     *       drum, or more than GOOD_MS early) is missed. Presses far from notes are ignored.</li>
     * </ol>
     */
    public Judgement hit(NoteType type, double inputMs) {
        for (int i = firstUnresolved; i < notes.size(); i++) {
            Note note = notes.get(i);
            double ahead = note.timeMs() - inputMs;                  // > 0: note still coming
            if (ahead > GOOD_MS) break;                              // sorted: nothing later can match
            if (resolved[i] || -ahead > GOOD_MS) continue;           // done, or too far behind
            if (note.noteType() == type) return resolveHit(i, Math.abs(ahead));
            if (ahead >= 0) break;                                   // upcoming other colour blocks
        }
        return missUpcomingNote(inputMs);
    }

    private Judgement resolveHit(int index, double error) {
        resolved[index] = true;
        lastHitNote = notes.get(index);
        advance();
        Judgement result = error <= PERFECT_MS ? Judgement.PERFECT : Judgement.GOOD;
        record(result);
        return result;
    }

    /** Anti-spam window: a wrong or far-too-early press this close to an upcoming note costs it. */
    public static final long MISS_MS = 150;

    private Judgement missUpcomingNote(double inputMs) {
        for (int i = firstUnresolved; i < notes.size(); i++) {
            double ahead = notes.get(i).timeMs() - inputMs;
            if (ahead > MISS_MS) return null;                        // sorted: the rest is later still
            if (resolved[i] || ahead < 0) continue;                  // done, or already passed
            resolved[i] = true;
            advance();
            record(Judgement.MISS);
            return Judgement.MISS;
        }
        return null;
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
    /** True once every note was hit or missed (AI-assisted 2026-11-09, for the results screen). */
    public boolean finished() { return firstUnresolved >= notes.size(); }
    /** Time of the last note in ms. */
    public long lastNoteMs() { return notes.getLast().timeMs(); }
}