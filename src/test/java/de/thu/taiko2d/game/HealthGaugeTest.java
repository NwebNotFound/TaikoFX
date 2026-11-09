package de.thu.taiko2d.game;

// AI-generated (Claude Code, 2026-11-09). See docs/AI_USAGE.md.

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HealthGaugeTest {
    /** n perfect hits, 100 ms apart, starting at {@code fromMs}; returns the time of the last one. */
    private static double hits(HealthGauge gauge, int n, double fromMs) {
        double t = fromMs;
        for (int i = 0; i < n; i++) {
            t = fromMs + i * 100;
            gauge.update(t);
            gauge.record(Judgement.PERFECT, 300, t);
        }
        return t;
    }

    @Test
    void startsFullAndMissesDrain() {
        HealthGauge gauge = new HealthGauge();
        assertEquals(1.0, gauge.health());
        for (int i = 0; i < 4; i++) gauge.record(Judgement.MISS, 0, i);
        assertEquals(1.0 - 4 * HealthGauge.MISS_LOSS, gauge.health(), 1e-9);
        hits(gauge, 3, 100);
        assertEquals(1.0 - 4 * HealthGauge.MISS_LOSS + 3 * HealthGauge.PERFECT_GAIN, gauge.health(), 1e-9);
    }

    @Test
    void missesCostHalfInTheDangerZone() {
        HealthGauge gauge = new HealthGauge();
        int i = 0;
        while (gauge.health() > HealthGauge.DANGER_HEALTH + 1e-9) gauge.record(Judgement.MISS, 0, i++);
        double before = gauge.health();
        gauge.record(Judgement.MISS, 0, i);
        assertEquals(before - HealthGauge.MISS_LOSS / 2, gauge.health(), 1e-9);
    }

    @Test
    void healthNeverExceedsFull() {
        HealthGauge gauge = new HealthGauge();
        hits(gauge, 5, 0);
        assertEquals(1.0, gauge.health());
    }

    @Test
    void failsWhenHealthReachesZeroAndThenIgnoresInput() {
        HealthGauge gauge = new HealthGauge();
        // 0.05 per miss down to 0.3 (14 misses), then 0.025 per miss (12 more): 26 in a row fail.
        for (int i = 0; i < 25; i++) gauge.record(Judgement.MISS, 0, i);
        assertFalse(gauge.failed());
        gauge.record(Judgement.MISS, 0, 100);
        assertTrue(gauge.failed());
        assertEquals(0, gauge.health());
        hits(gauge, 10, 200);
        assertEquals(0, gauge.health());
    }

    @Test
    void feverStartsAfterStreakAndGivesBonus() {
        HealthGauge gauge = new HealthGauge();
        double t = hits(gauge, HealthGauge.FEVER_HITS - 1, 0);
        assertFalse(gauge.fever());
        assertEquals((HealthGauge.FEVER_HITS - 1) / (double) HealthGauge.FEVER_HITS, gauge.feverCharge(), 1e-9);
        gauge.record(Judgement.PERFECT, 300, t);
        assertTrue(gauge.fever());
        assertEquals(0, gauge.bonusScore());                 // the hit that starts fever earns no bonus
        gauge.record(Judgement.PERFECT, 300, t);
        gauge.record(Judgement.GOOD, 100, t);
        assertEquals(150 + 50, gauge.bonusScore());
    }

    @Test
    void comboBreakCancelsFeverAndMeter() {
        HealthGauge gauge = new HealthGauge();
        double t = hits(gauge, HealthGauge.FEVER_HITS, 0);
        assertTrue(gauge.fever());
        gauge.record(Judgement.MISS, 0, t + 10);
        assertFalse(gauge.fever());
        assertEquals(0, gauge.feverCharge());
    }

    @Test
    void meterDrainsOnlyAfterIdleTime() {
        HealthGauge gauge = new HealthGauge();
        double t = hits(gauge, 15, 0);                       // meter at 0.5
        gauge.update(t + HealthGauge.IDLE_MS);               // still within the grace time
        assertEquals(0.5, gauge.feverCharge(), 1e-9);
        gauge.update(t + HealthGauge.IDLE_MS + 1000);        // one idle second
        assertEquals(0.5 - HealthGauge.IDLE_DRAIN_PER_S, gauge.feverCharge(), 1e-9);
        gauge.update(t + 60_000);
        assertEquals(0, gauge.feverCharge());
    }

    @Test
    void feverRunsOutWithoutHits() {
        HealthGauge gauge = new HealthGauge();
        double t = hits(gauge, HealthGauge.FEVER_HITS, 0);
        gauge.update(t + 3000);
        assertTrue(gauge.fever());
        assertEquals(1 - 3 * HealthGauge.FEVER_DRAIN_PER_S, gauge.feverCharge(), 1e-9);
        gauge.update(t + 7000);
        assertFalse(gauge.fever());
        assertEquals(0, gauge.feverCharge());
    }

    @Test
    void hypeRisesWithHitsAndFallsOnMisses() {
        HealthGauge gauge = new HealthGauge();
        hits(gauge, 10, 0);
        assertEquals(10 * HealthGauge.HYPE_PERFECT, gauge.hype(), 1e-9);
        gauge.record(Judgement.MISS, 0, 2000);
        assertEquals(Math.max(0, 10 * HealthGauge.HYPE_PERFECT - HealthGauge.HYPE_MISS), gauge.hype(), 1e-9);
        for (int i = 0; i < 5; i++) gauge.record(Judgement.MISS, 0, 3000 + i);
        assertEquals(0, gauge.hype());
    }

    @Test
    void sparseChartsNeedFewerHitsAndAllowLongerGaps() {
        HealthGauge easy = new HealthGauge(100, 99 * 1500.0);      // 100 notes, 1.5 s apart
        assertEquals(10, easy.feverHits());
        assertEquals(4500, easy.idleMs(), 1e-9);
        HealthGauge hard = new HealthGauge(900, 899 * 150.0);      // dense chart keeps the default
        assertEquals(HealthGauge.FEVER_HITS, hard.feverHits());
        assertEquals(HealthGauge.IDLE_MS, hard.idleMs(), 1e-9);
        for (int i = 0; i < 10; i++) easy.record(Judgement.PERFECT, 300, i * 1500);
        assertTrue(easy.fever());
    }

    @Test
    void kiaiChargesFeverTwiceAsFast() {
        HealthGauge gauge = new HealthGauge();
        gauge.setKiai(true);
        for (int i = 0; i < HealthGauge.FEVER_HITS / 2; i++) gauge.record(Judgement.PERFECT, 300, i * 100);
        assertTrue(gauge.fever());
    }
}
