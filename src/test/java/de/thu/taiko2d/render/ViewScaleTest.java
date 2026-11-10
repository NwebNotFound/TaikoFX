package de.thu.taiko2d.render;

// AI-generated (Claude Code, 2026-11-10). See docs/AI_USAGE.md.

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ViewScaleTest {
    @Test
    void defaultWindowIsUnscaled() {
        assertEquals(1.0, ViewScale.fit(960, 560), 1e-9);
    }

    @Test
    void fullHdScalesWithTheHeight() {
        assertEquals(1080 / 560.0, ViewScale.fit(1920, 1080), 1e-9);
    }

    @Test
    void narrowWindowScalesDownToKeepTheLayoutWidth() {
        assertEquals(0.8, ViewScale.fit(640, 900), 1e-9);
    }

    @Test
    void emptySizeFallsBackToOne() {
        assertEquals(1.0, ViewScale.fit(0, 0), 1e-9);
    }

    @Test
    void snappedScaleGivesWholeSpritePixels() {
        double s = ViewScale.pixelSnapped(1920, 1080, 3);   // fit 1.93 -> 2
        assertEquals(2.0, s, 1e-9);
        assertEquals(5.0 / 3, ViewScale.pixelSnapped(1920, 1009, 3), 1e-9);   // fit 1.80 -> 5/3
        assertEquals(1.0 / 3, ViewScale.pixelSnapped(10, 10, 3), 1e-9);       // never zero
    }
}
