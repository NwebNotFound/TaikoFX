package de.thu.taiko2d.model;

// AI-generated (Claude Code, 2026-11-09). See docs/AI_USAGE.md.

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ChartInfoTest {
    @Test
    void referenceBpmScrollsAtBaseSpeed() {
        assertEquals(ChartInfo.BASE_PIXELS_PER_MS, new ChartInfo(150, 0, 1).pixelsPerMs(1), 1e-12);
    }

    @Test
    void fasterSongsScrollFasterAndMultipliersStack() {
        assertEquals(0.4 * (200 / 150.0) * 1.5 * 0.8, new ChartInfo(200, 0, 1.5).pixelsPerMs(0.8), 1e-12);
    }

    @Test
    void extremeBpmIsClamped() {
        assertEquals(0.4 * 1.8, new ChartInfo(900, 0, 1).pixelsPerMs(1), 1e-12);
        assertEquals(0.4 * 0.6, new ChartInfo(30, 0, 1).pixelsPerMs(1), 1e-12);
    }

    @Test
    void kiaiSectionsAreStartInclusiveEndExclusive() {
        ChartInfo info = new ChartInfo(150, 0, 1, java.util.List.of(new ChartInfo.Section(1000, 2000)));
        org.junit.jupiter.api.Assertions.assertFalse(info.isKiai(999));
        org.junit.jupiter.api.Assertions.assertTrue(info.isKiai(1000));
        org.junit.jupiter.api.Assertions.assertTrue(info.isKiai(1999));
        org.junit.jupiter.api.Assertions.assertFalse(info.isKiai(2000));
        org.junit.jupiter.api.Assertions.assertFalse(new ChartInfo(150, 0, 1).isKiai(1000));
    }

    @Test
    void lastKiaiStartIgnoresShortFlashes() {
        ChartInfo info = new ChartInfo(150, 0, 1, java.util.List.of(
                new ChartInfo.Section(1000, 5000), new ChartInfo.Section(6000, 6072),
                new ChartInfo.Section(8000, 12000)));
        org.junit.jupiter.api.Assertions.assertTrue(Double.isNaN(info.lastKiaiStart(999, 1000)));
        assertEquals(1000, info.lastKiaiStart(3000, 1000), 1e-9);
        assertEquals(1000, info.lastKiaiStart(7000, 1000), 1e-9);   // the 72 ms flash does not count
        assertEquals(8000, info.lastKiaiStart(20_000, 1000), 1e-9);
    }
}
