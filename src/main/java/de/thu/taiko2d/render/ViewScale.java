package de.thu.taiko2d.render;

/**
 * Uniform scale factor that makes the game and menus grow with the window.
 * Everything is laid out for a design size of at least 800 x 560 and then scaled up or down.
 * AI-generated (Claude Code, 2026-11-10); see docs/AI_USAGE.md.
 */
public final class ViewScale {
    /** Narrowest layout that still fits the song select (detail card + list). */
    public static final double BASE_WIDTH = 800;
    /** The default window height; at this height the scale is 1. */
    public static final double BASE_HEIGHT = 560;

    private ViewScale() {}

    /** Largest scale at which the design size still fits into {@code width x height}. */
    public static double fit(double width, double height) {
        if (width <= 0 || height <= 0) return 1;
        return Math.min(width / BASE_WIDTH, height / BASE_HEIGHT);
    }

    /**
     * {@link #fit} rounded to a multiple of {@code 1 / spriteScale}, so that sprites drawn at
     * {@code spriteScale} end up a whole number of screen pixels per sprite pixel (crisp pixel art).
     */
    public static double pixelSnapped(double width, double height, int spriteScale) {
        int steps = Math.max(1, spriteScale);
        return Math.max(1, Math.round(fit(width, height) * steps)) / (double) steps;
    }
}
