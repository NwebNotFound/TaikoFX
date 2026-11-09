package de.thu.taiko2d.game;

/**
 * Health, fever and crowd hype, in the style of Muse Dash. AI-generated 2026-11-09,
 * rules chosen by the student; see docs/AI_USAGE.md. Pure model, no JavaFX.
 *
 * <ul>
 *   <li>Health starts full. Hits refill a little, a miss drains more (half as much in the
 *       danger zone below {@link #DANGER_HEALTH}). At 0 the run fails.</li>
 *   <li>Every hit charges the fever meter (twice as much in kiai). When it is full, fever
 *       starts: every hit is worth {@link #FEVER_BONUS} more points. How many hits fill
 *       it depends on the chart, see {@link #HealthGauge(int, double)}.</li>
 *   <li>The meter drains on its own: slowly when no notes are hit for a while, and
 *       steadily during fever, which ends when the meter is empty. Hits in fever top it up.</li>
 *   <li>A miss breaks the combo, so it cancels fever and empties the meter.</li>
 *   <li>Hype (0..1) measures how well you are doing right now; the crowd grows with it.</li>
 * </ul>
 */
public final class HealthGauge {
    // Softened 2026-11-09 (was 0.01 / 0.005 / 0.08: about 13 misses failed the run and one
    // miss took 8 perfects to make up). Now two or three hits make up a miss.
    public static final double PERFECT_GAIN = 0.02;
    public static final double GOOD_GAIN = 0.015;
    public static final double MISS_LOSS = 0.05;
    /** Below this health a miss costs only half, so a bad section is survivable. */
    public static final double DANGER_HEALTH = 0.3;
    public static final int FEVER_HITS = 30;
    public static final double FEVER_BONUS = 0.5;
    /** No hit for this long and the fever meter starts to drain. */
    public static final double IDLE_MS = 1000;
    /** Meter lost per second while idle (outside fever). */
    public static final double IDLE_DRAIN_PER_S = 0.25;
    /** Meter lost per second during fever: an empty song section ends fever in ~6 s. */
    public static final double FEVER_DRAIN_PER_S = 1.0 / 6;
    public static final double HYPE_PERFECT = 0.02;
    public static final double HYPE_GOOD = 0.01;
    public static final double HYPE_MISS = 0.12;

    /** Hits needed to fill the meter; fewer on sparse (easy) maps. */
    private final int feverHits;
    /** Idle grace time; longer on maps whose notes are far apart. */
    private final double idleMs;
    private boolean kiai;

    /** Default tuning (30 hits, 1 s grace). */
    public HealthGauge() {
        this.feverHits = FEVER_HITS;
        this.idleMs = IDLE_MS;
    }

    /**
     * Tuned to the chart so easy maps can reach fever too: about a tenth of the notes
     * fill the meter (between 10 and {@link #FEVER_HITS}), and the idle grace is three
     * average note gaps (at least {@link #IDLE_MS}).
     */
    public HealthGauge(int noteCount, double chartLengthMs) {
        this.feverHits = Math.max(10, Math.min(FEVER_HITS, Math.round(noteCount / 10f)));
        double averageGap = noteCount > 1 ? chartLengthMs / (noteCount - 1) : IDLE_MS;
        this.idleMs = Math.max(IDLE_MS, 3 * averageGap);
    }

    /** During kiai sections (from osu! maps) hits charge the fever meter twice as fast. */
    public void setKiai(boolean kiai) { this.kiai = kiai; }

    public int feverHits() { return feverHits; }
    public double idleMs() { return idleMs; }

    private double health = 1.0;
    private double feverMeter;
    private boolean fever;
    private boolean failed;
    private int bonusScore;
    private double hype;
    private double lastHitMs = Double.NEGATIVE_INFINITY;
    private double lastUpdateMs = Double.NaN;

    /** Applies one judgement at song time {@code atMs}. {@code basePoints} is what the engine awarded. */
    public void record(Judgement judgement, int basePoints, double atMs) {
        if (failed) return;
        switch (judgement) {
            case PERFECT, GOOD -> {
                boolean perfect = judgement == Judgement.PERFECT;
                health = Math.min(1.0, health + (perfect ? PERFECT_GAIN : GOOD_GAIN));
                hype = Math.min(1.0, hype + (perfect ? HYPE_PERFECT : HYPE_GOOD));
                lastHitMs = atMs;
                double charge = (kiai ? 2.0 : 1.0) / feverHits;
                if (fever) {
                    bonusScore += (int) Math.round(basePoints * FEVER_BONUS);
                    feverMeter = Math.min(1.0, feverMeter + charge / 2);
                } else {
                    feverMeter = Math.min(1.0, feverMeter + charge);
                    if (feverMeter >= 1.0 - 1e-9) {
                        feverMeter = 1.0;
                        fever = true;
                    }
                }
            }
            case MISS -> {
                double loss = health <= DANGER_HEALTH + 1e-9 ? MISS_LOSS / 2 : MISS_LOSS;
                health = Math.max(0.0, health - loss);
                hype = Math.max(0.0, hype - HYPE_MISS);
                fever = false;
                feverMeter = 0;
                if (health <= 1e-9) {
                    health = 0;
                    failed = true;
                }
            }
        }
    }

    /** Call once per frame with the song time; drains the fever meter over time. */
    public void update(double nowMs) {
        double elapsedS = Double.isNaN(lastUpdateMs) ? 0 : Math.max(0, nowMs - lastUpdateMs) / 1000.0;
        lastUpdateMs = nowMs;
        if (failed || elapsedS == 0) return;
        if (fever) {
            feverMeter -= FEVER_DRAIN_PER_S * elapsedS;
        } else if (nowMs - lastHitMs > idleMs) {
            feverMeter -= IDLE_DRAIN_PER_S * elapsedS;
        }
        if (feverMeter <= 0) {
            feverMeter = 0;
            fever = false;
        }
    }

    public static int basePoints(Judgement judgement) {
        return switch (judgement) {
            case PERFECT -> 300;
            case GOOD -> 100;
            case MISS -> 0;
        };
    }

    public double health() { return health; }
    /** Fever meter from 0 to 1. */
    public double feverCharge() { return feverMeter; }
    public boolean fever() { return fever; }
    public boolean failed() { return failed; }
    public int bonusScore() { return bonusScore; }
    /** How well the player is doing right now, 0..1. */
    public double hype() { return hype; }
}
