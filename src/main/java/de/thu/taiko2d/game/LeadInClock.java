package de.thu.taiko2d.game;

import java.util.function.LongSupplier;

/**
 * Song clock with an artificial silent intro, so the first notes scroll in from the
 * right edge instead of appearing at the drum. AI-generated 2026-11-09; see docs/AI_USAGE.md.
 *
 * Before the audio starts the clock counts up from {@code -leadInMs} using the system
 * timer; at 0 it starts the audio and from then on follows the audio's own position.
 * Pure Java: the audio is behind {@link Audio}, so tests can use a fake.
 */
public final class LeadInClock {
    /** The few audio operations the clock needs (JavaFX MediaPlayer in the game). */
    public interface Audio {
        double timeMs();
        void play();
        void pause();
        void seek(double ms);
    }

    private final Audio audio;
    private final LongSupplier nanoTime;
    private final double leadInMs;
    private long startNanos;
    private boolean audioStarted;
    private boolean paused;
    private double frozenMs;

    public LeadInClock(Audio audio, LongSupplier nanoTime, double leadInMs) {
        this.audio = audio;
        this.nanoTime = nanoTime;
        this.leadInMs = Math.max(0, leadInMs);
        this.startNanos = nanoTime.getAsLong();
    }

    /** Current song time in ms; negative during the intro. */
    public double timeMs() {
        if (audioStarted) return audio.timeMs();
        if (paused) return frozenMs;
        double t = (nanoTime.getAsLong() - startNanos) / 1e6 - leadInMs;
        if (t >= 0) {
            audioStarted = true;
            audio.play();
            return 0;
        }
        return t;
    }

    public void pause() {
        if (paused) return;
        if (audioStarted) audio.pause(); else frozenMs = timeMs();
        paused = true;
    }

    public void resume() {
        if (!paused) return;
        paused = false;
        if (audioStarted) audio.play(); else restartIntroAt(frozenMs);
    }

    /** Jumps forward to {@code targetMs} (never backwards, not while paused). */
    public void skipTo(double targetMs) {
        if (paused || targetMs <= timeMs()) return;
        if (targetMs < 0) {
            restartIntroAt(targetMs);
        } else {
            audio.seek(targetMs);
            if (!audioStarted) {
                audioStarted = true;
                audio.play();
            }
        }
    }

    private void restartIntroAt(double songMs) {
        startNanos = nanoTime.getAsLong() - Math.round((songMs + leadInMs) * 1e6);
    }

    /**
     * Intro length so the first note has time to scroll in: the note needs
     * {@code travelMs} to cross the screen, plus one second to get ready.
     */
    public static double leadInFor(double firstNoteMs, double travelMs) {
        return Math.max(0, travelMs + 1000 - firstNoteMs);
    }

    /** Where Space skips to: one second before the first note becomes visible. */
    public static double skipTargetFor(double firstNoteMs, double travelMs) {
        return firstNoteMs - travelMs - 1000;
    }

    /** A skip is only offered if it saves at least this much waiting. */
    public static final double MIN_SKIP_MS = 1000;

    /**
     * Where Space skips to at song time {@code nowMs}, or NaN if there is nothing to skip.
     * Works for the intro and for long breaks between notes (added 2026-11-09): the target is
     * one second before the next note scrolls in, and the previous note must be past its
     * hit window so skipping cannot steal it.
     *
     * @param noteTimes note times in ms, sorted ascending
     */
    public static double skipTargetAt(long[] noteTimes, double nowMs, double travelMs) {
        int next = 0;
        while (next < noteTimes.length && noteTimes[next] <= nowMs) next++;
        if (next == noteTimes.length) return Double.NaN;                     // song is over
        if (next > 0 && nowMs <= noteTimes[next - 1] + JudgementEngine.GOOD_MS) return Double.NaN;
        double target = skipTargetFor(noteTimes[next], travelMs);
        return target - nowMs >= MIN_SKIP_MS ? target : Double.NaN;
    }
}
