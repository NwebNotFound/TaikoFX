package de.thu.taiko2d.render;

import de.thu.taiko2d.game.HealthGauge;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Pixelated "liquid" health bar with a moving wave on its surface.
 *
 * SCAFFOLD: the frame, colours, fever strip and column drawing loop were
 * AI-generated 2026-11-09 (see docs/AI_USAGE.md). The two TODO methods,
 * {@link #waveOffset} and {@link #snap}, are the student's part.
 */
final class WaveHealthBar {
    /** Size of one art pixel on screen. The bar is drawn as PIXEL x PIXEL blocks. */
    static final int PIXEL = 4;

    private static final Color FRAME = PixelArt.INK;
    private static final Color SHADOW = Color.rgb(0, 0, 0, 0.35);
    private static final Color EMPTY = Color.web("#2b2142");
    private static final Color LIQUID = Color.web("#e8433a");
    private static final Color LIQUID_LIGHT = Color.web("#ff8a6b");
    private static final Color LOW = Color.web("#8e5bd6");
    private static final Color FEVER_A = PixelArt.GOLD;
    private static final Color FEVER_B = Color.web("#fff1a8");

    /**
     * TODO (student): vertical offset of the liquid surface, in screen pixels, at
     * horizontal position {@code x} (pixels from the bar's left edge) and song time
     * {@code timeMs}. Positive values go down.
     *
     * Idea: a travelling sine wave, amplitude * sin(k * x - omega * t), maybe with a
     * second smaller sine on top so it does not look too regular. {@code amplitude}
     * already grows during fever and on the beat.
     *
     * Placeholder: no wave at all (flat surface).
     */
    static double waveOffset(double x, double timeMs, double amplitude) {
        double t = timeMs / 1000.0;
        double primary = Math.sin(0.05 * x - 7.5 * t);
        double secondary = 0.35 * Math.sin(0.15 * x - 10.0 * t);
        return amplitude * (primary + secondary);
    }
    static double snap(double value) {
        return Math.floor(value / PIXEL) * PIXEL;
    }

    /**
     * Draws the bar with its top-left corner at (x, y). {@code beatPulse} is 1 right on
     * a beat and falls to 0 before the next one.
     */
    void draw(GraphicsContext g, double x, double y, double width, double height,
              HealthGauge gauge, double timeMs, double beatPulse) {
        x = Math.round(x / PIXEL) * PIXEL;
        y = Math.round(y / PIXEL) * PIXEL;
        width = Math.round(width / PIXEL) * PIXEL;
        height = Math.round(height / PIXEL) * PIXEL;

        // Chunky frame with a hard drop shadow.
        g.setFill(SHADOW);
        g.fillRect(x, y, width + 2 * PIXEL, height + 2 * PIXEL);
        g.setFill(FRAME);
        g.fillRect(x - PIXEL, y - PIXEL, width + 2 * PIXEL, height + 2 * PIXEL);
        g.setFill(EMPTY);
        g.fillRect(x, y, width, height);

        boolean fever = gauge.fever();
        double health = gauge.health();
        double filled = Math.round(width * health / PIXEL) * PIXEL;
        double amplitude = (fever ? 3 : 1.5) * PIXEL + beatPulse * PIXEL;
        double surface = y + height * 0.3;   // resting level of the liquid
        Color body = fever ? FEVER_A : health < 0.3 ? LOW : LIQUID;
        Color crest = fever ? FEVER_B : LIQUID_LIGHT;

        // One column per art pixel: the column is filled from the wave surface down.
        for (double cx = 0; cx < filled; cx += PIXEL) {
            double top = snap(surface + waveOffset(cx, timeMs, amplitude));
            top = Math.max(y, Math.min(y + height - PIXEL, top));
            g.setFill(body);
            g.fillRect(x + cx, top, PIXEL, y + height - top);
            g.setFill(crest);
            g.fillRect(x + cx, top, PIXEL, PIXEL);
        }

        // Fever charge: a strip of segments under the bar.
        int segments = 10;
        double segmentWidth = (width - (segments - 1) * PIXEL) / segments;
        double lit = gauge.feverCharge() * segments;
        double sy = y + height + 2 * PIXEL;
        for (int i = 0; i < segments; i++) {
            double sx = x + i * (segmentWidth + PIXEL);
            g.setFill(FRAME);
            g.fillRect(sx - PIXEL / 2.0, sy - PIXEL / 2.0, segmentWidth + PIXEL, 2 * PIXEL);
            boolean on = i < lit;
            boolean blink = fever && ((long) (timeMs / 120) + i) % 2 == 0;
            g.setFill(on ? (blink ? FEVER_B : FEVER_A) : EMPTY);
            g.fillRect(sx, sy, segmentWidth, PIXEL);
        }
    }
}
