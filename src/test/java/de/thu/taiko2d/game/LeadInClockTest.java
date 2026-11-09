package de.thu.taiko2d.game;

// AI-generated (Claude Code, 2026-11-09). See docs/AI_USAGE.md.

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LeadInClockTest {
    /** Fake audio: position only moves when the test says so. */
    static final class FakeAudio implements LeadInClock.Audio {
        double position;
        boolean playing;
        int plays;
        public double timeMs() { return position; }
        public void play() { playing = true; plays++; }
        public void pause() { playing = false; }
        public void seek(double ms) { position = ms; }
    }

    private final long[] now = {0};
    private final FakeAudio audio = new FakeAudio();

    private void advance(double ms) { now[0] += Math.round(ms * 1e6); }

    @Test
    void countsUpFromMinusLeadInThenStartsAudio() {
        LeadInClock clock = new LeadInClock(audio, () -> now[0], 2000);
        assertEquals(-2000, clock.timeMs(), 1e-6);
        advance(1500);
        assertEquals(-500, clock.timeMs(), 1e-6);
        assertFalse(audio.playing);
        advance(600);
        assertEquals(0, clock.timeMs(), 1e-6);
        assertTrue(audio.playing);
        audio.position = 1234;
        assertEquals(1234, clock.timeMs(), 1e-6);   // now follows the audio
        assertEquals(1, audio.plays);
    }

    @Test
    void pauseFreezesTheIntro() {
        LeadInClock clock = new LeadInClock(audio, () -> now[0], 2000);
        advance(500);
        clock.pause();
        advance(10_000);
        assertEquals(-1500, clock.timeMs(), 1e-6);
        clock.resume();
        advance(100);
        assertEquals(-1400, clock.timeMs(), 1e-6);
        assertFalse(audio.playing);
    }

    @Test
    void pauseAfterIntroPausesTheAudio() {
        LeadInClock clock = new LeadInClock(audio, () -> now[0], 0);
        clock.timeMs();
        assertTrue(audio.playing);
        clock.pause();
        assertFalse(audio.playing);
        clock.resume();
        assertTrue(audio.playing);
    }

    @Test
    void skipDuringIntroMovesTheIntroClock() {
        LeadInClock clock = new LeadInClock(audio, () -> now[0], 3000);
        clock.skipTo(-1000);
        assertEquals(-1000, clock.timeMs(), 1e-6);
        assertFalse(audio.playing);
    }

    @Test
    void skipIntoTheSongSeeksAndStartsTheAudio() {
        LeadInClock clock = new LeadInClock(audio, () -> now[0], 0);
        clock.skipTo(20_000);
        assertEquals(20_000, audio.position, 1e-6);
        assertTrue(audio.playing);
        assertEquals(20_000, clock.timeMs(), 1e-6);
    }

    @Test
    void skipNeverGoesBackwardsOrWhilePaused() {
        LeadInClock clock = new LeadInClock(audio, () -> now[0], 0);
        audio.position = 5000;
        clock.timeMs();
        clock.skipTo(4000);
        assertEquals(5000, audio.position, 1e-6);
        clock.pause();
        clock.skipTo(9000);
        assertEquals(5000, audio.position, 1e-6);
    }

    @Test
    void leadInAndSkipTargetFormulas() {
        // First note at 300 ms, notes need 2000 ms to cross the screen.
        assertEquals(2700, LeadInClock.leadInFor(300, 2000), 1e-9);
        assertEquals(0, LeadInClock.leadInFor(10_000, 2000), 1e-9);      // long intro: no extra wait
        assertEquals(7000, LeadInClock.skipTargetFor(10_000, 2000), 1e-9);
    }

    @Test
    void skipIsOfferedInTheIntroAndInLongBreaks() {
        long[] notes = {10_000, 10_500, 30_000};
        double travel = 2000;
        assertEquals(7000, LeadInClock.skipTargetAt(notes, -2000, travel), 1e-9);   // intro
        assertTrue(Double.isNaN(LeadInClock.skipTargetAt(notes, 6500, travel)));    // saves < 1 s
        assertTrue(Double.isNaN(LeadInClock.skipTargetAt(notes, 10_200, travel)));  // short gap
        assertTrue(Double.isNaN(LeadInClock.skipTargetAt(notes, 10_550, travel)));  // last note still hittable
        assertEquals(27_000, LeadInClock.skipTargetAt(notes, 11_000, travel), 1e-9); // long break
        assertTrue(Double.isNaN(LeadInClock.skipTargetAt(notes, 31_000, travel)));  // after the last note
    }
}
