package de.thu.taiko2d.model;

import java.util.List;

/**
 * Per-chart timing and scroll data. AI-generated 2026-11-09; see docs/AI_USAGE.md.
 * {@code scrollMultiplier} and {@code kiai} are optional in the JSON (default 1 / none).
 * Kiai sections are the "highlight" parts of a song, taken from osu! maps.
 */
public record ChartInfo(double bpm, long gridStartMs, double scrollMultiplier, List<Section> kiai) {
    /** Scroll speed of the original fixed-speed game, in pixels per ms. */
    public static final double BASE_PIXELS_PER_MS = 0.4;
    /** BPM at which a chart scrolls at the base speed. */
    public static final double REFERENCE_BPM = 150;

    /** A time range in song milliseconds, start inclusive, end exclusive. */
    public record Section(long startMs, long endMs) {}

    public ChartInfo {
        kiai = List.copyOf(kiai);
    }

    public ChartInfo(double bpm, long gridStartMs, double scrollMultiplier) {
        this(bpm, gridStartMs, scrollMultiplier, List.of());
    }

    /**
     * Faster songs scroll faster, like in taiko, so notes are spaced by beats rather
     * than milliseconds. The BPM factor is clamped so extreme BPMs stay playable.
     * {@code playerMultiplier} is the speed setting from song select.
     */
    public double pixelsPerMs(double playerMultiplier) {
        double bpmFactor = Math.max(0.6, Math.min(1.8, bpm / REFERENCE_BPM));
        return BASE_PIXELS_PER_MS * bpmFactor * scrollMultiplier * playerMultiplier;
    }

    /**
     * Start of the latest kiai section that began at or before {@code songMs} and lasts at
     * least {@code minLengthMs} (osu! maps also use split-second kiai "flashes"), or NaN.
     */
    public double lastKiaiStart(double songMs, double minLengthMs) {
        double start = Double.NaN;
        for (Section s : kiai) {
            if (s.startMs() <= songMs && s.endMs() - s.startMs() >= minLengthMs
                    && !(start >= s.startMs())) start = s.startMs();
        }
        return start;
    }

    public boolean isKiai(double songMs) {
        for (Section s : kiai) {
            if (songMs >= s.startMs() && songMs < s.endMs()) return true;
        }
        return false;
    }
}
