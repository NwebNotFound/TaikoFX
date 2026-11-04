package de.thu.taiko2d.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Tests for the note position formula. AI-generated; see docs/AI_USAGE.md.
 */
class NoteTest {
    private static final double HIT_X = 200;
    private static final double PIXELS_PER_MS = 0.5;
    private static final double DELTA = 0.0001;

    @Test
    void noteAtSongTimeSitsOnHitMarker() {
        assertEquals(200, Note.noteX(HIT_X, 1000, 1000, PIXELS_PER_MS), DELTA);
    }

    @Test
    void futureNoteIsRightOfHitMarker() {
        // 500 ms ahead * 0.5 px/ms = 250 px to the right
        assertEquals(450, Note.noteX(HIT_X, 1500, 1000, PIXELS_PER_MS), DELTA);
    }

    @Test
    void pastNoteIsLeftOfHitMarker() {
        // 200 ms behind * 0.5 px/ms = 100 px to the left
        assertEquals(100, Note.noteX(HIT_X, 800, 1000, PIXELS_PER_MS), DELTA);
    }

    @Test
    void positionIsIndependentOfFrameRate() {
        // Same song time must give the same x regardless of how it was reached.
        double after60Frames = 0;
        for (int i = 0; i < 60; i++) {
            after60Frames += 1000.0 / 60;
        }
        double after30Frames = 0;
        for (int i = 0; i < 30; i++) {
            after30Frames += 1000.0 / 30;
        }
        assertEquals(
                Note.noteX(HIT_X, 1500, after60Frames, PIXELS_PER_MS),
                Note.noteX(HIT_X, 1500, after30Frames, PIXELS_PER_MS),
                DELTA);
    }

    @Test
    void chartOffsetMovesNoteLater() {
        // Caller passes timeMs + offsetMs; +20 ms offset shifts the note 10 px right.
        assertEquals(210, Note.noteX(HIT_X, 1000 + 20, 1000, PIXELS_PER_MS), DELTA);
    }
}
