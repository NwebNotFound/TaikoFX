package de.thu.taiko2d.render;

import java.util.Map;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.transform.Affine;

/**
 * Drum mascot that bobs to the beat, swings a stick on every drum hit and does a
 * flip on combo milestones (50x, 100x, ...).
 *
 * SCAFFOLD: the placeholder pixel art, state handling and drawing with a Matrix3x3
 * as the canvas transform were AI-generated 2026-11-09 (see docs/AI_USAGE.md).
 * The three TODO transform methods are the student's part: they decide how the
 * mascot moves, using the student's own {@link Matrix3x3}.
 *
 * Sprite-local coordinates: the origin is the centre between the mascot's feet,
 * x goes right, y goes DOWN (screen convention), so the body is at negative y.
 * Each method returns the matrix that maps sprite-local points to screen points.
 */
final class Mascot {

    static final double MISS_MS = 400;
    static final int SCALE = 4;   // screen pixels per art pixel
    static final double HIT_MS = 160;
    static final double FLIP_MS = 700;

    private static final Image IDLE;
    private static final Image HIT_LEFT;
    private static final Image HIT_RIGHT;
    private static final Image SAD;

    private double hitMs = Double.NEGATIVE_INFINITY;
    private boolean hitLeft;
    private double flipMs = Double.NEGATIVE_INFINITY;
    private int flipSpins;
    private int lastCombo;
    private double missMs = Double.NEGATIVE_INFINITY;

    // ---------------------------------------------------------------- student TODOs

    /**
     * TODO (student): idle bob. {@code beatPulse} is 1 exactly on a beat and falls to 0.
     * Idea: scale about the feet, slightly wider and shorter on the beat
     * (squash), e.g. translate(footX, footY) * scale(1 + a*pulse, 1 - a*pulse).
     * Because the origin is already at the feet, the feet stay on the ground.
     *
     * Placeholder: just places the mascot, no motion.
     */
    static Matrix3x3 idleTransform(double beatPulse, double footX, double footY) {
        double intensity = 0.12;
        double scaleX = 1.0 + intensity * beatPulse;
        double scaleY = 1.0 - intensity * beatPulse;
        return Matrix3x3.translate(footX, footY).multiply(Matrix3x3.scale(scaleX, scaleY));
    }

    /**
     * TODO (student): reaction to a drum hit, {@code t} goes 0 -> 1 over HIT_MS.
     * {@code left} says which hand hit. Idea: a quick lean towards that side, a
     * rotation about the feet (pivot at the origin), plus a small squash that
     * springs back: the strength should go to 0 at t = 1.
     *
     * Placeholder: no motion (the stick still changes, that is the sprite frame).
     */
    static Matrix3x3 hitTransform(double t, boolean left, double footX, double footY) {
        double fade = Math.sin((1.0 - t) * Math.PI * 0.5);
        double leanAngle = Math.toRadians(left ? -12.0 : 12.0) * fade;
        double squashY = 1.0 - 0.08 * Math.sin(t * Math.PI) * fade;
        double squashX = 1.0 + 0.08 * Math.sin(t * Math.PI) * fade;

        return Matrix3x3.translate(footX, footY).multiply(Matrix3x3.rotate(leanAngle)).multiply(Matrix3x3.scale(squashX, squashY));
    }

    /**
     * TODO (student): the combo flip, {@code t} goes 0 -> 1 over FLIP_MS.
     * {@code spins} is 1 at 50x and 2 at every 100x.
     * Idea: jump on a parabola (like the hit-note flight) and rotate by
     * spins * 2 * PI * t about the BODY CENTRE, which is the point (0, -bodyHeight / 2)
     * in sprite-local coordinates, not the feet. That is a rotation about a pivot:
     * translate(pivot) * rotate(angle) * translate(-pivot).
     *
     * Placeholder: no motion.
     */
    static Matrix3x3 flipTransform(double t, int spins, double footX, double footY, double bodyHeight) {
        double maxJumpHeight = 80.0;
        double jumpY = -4.0 * maxJumpHeight * t * (1.0 - t);
        double angleRad = spins * 2.0 * Math.PI * t;
        double pivotY = -bodyHeight / 2.0;
        return Matrix3x3.translate(footX, footY + jumpY).multiply(Matrix3x3.translate(0, pivotY)).multiply(Matrix3x3.rotate(angleRad)).multiply(Matrix3x3.translate(0, -pivotY));
    }

    /**
     * TODO (student): sad reaction to a miss, {@code t} goes 0 -> 1.
     * Placeholder: just places the mascot, no motion.
     */
    static Matrix3x3 sadTransform(double t, double footX, double footY) {
        double fade = 1.0 - t;

        double offsetX = Math.sin(t * Math.PI * 8.0) * 8.0 * fade;
        double offsetY = Math.sin(t * Math.PI) * 4.0 * fade;
        return Matrix3x3.translate(footX + offsetX, footY + offsetY);
    }

    // ---------------------------------------------------------------- game hooks

    /** TODO (student): remember when the miss happened. Call it from GameCanvas.showJudgement on MISS. */
    void onMiss(double atMs) {
        this.missMs = atMs;
    }

    void onDrumHit(boolean left, double atMs) {
        hitLeft = left;
        hitMs = atMs;
    }

    /** Call every frame with the current combo; starts a flip when a milestone is crossed. */
    void onCombo(int combo, double atMs) {
        int spins = flipSpins(lastCombo, combo);
        if (spins > 0) {
            flipSpins = spins;
            flipMs = atMs;
        }
        lastCombo = combo;
    }

    /** 0 = no flip, 1 = a flip at 50x/150x/..., 2 = a double flip at every 100x. */
    static int flipSpins(int previousCombo, int combo) {
        if (combo <= previousCombo || combo < 50) return 0;
        if (combo / 50 == previousCombo / 50) return 0;
        return (combo / 50) % 2 == 0 ? 2 : 1;
    }

    void draw(GraphicsContext g, double footX, double footY, double nowMs, double beatPulse) {
        Image frame = IDLE;
        double width = IDLE.getWidth() * SCALE;
        double height = IDLE.getHeight() * SCALE;
        Matrix3x3 m;
        double flipT = (nowMs - flipMs) / FLIP_MS;
        double missT = (nowMs - missMs) / MISS_MS;
        double hitT = (nowMs - hitMs) / HIT_MS;

        if (flipT >= 0 && flipT < 1) {
            m = flipTransform(flipT, flipSpins, footX, footY, height);
        } else if (missT >= 0 && missT < 1) {
            m = sadTransform(missT, footX, footY);
            frame = SAD;
        } else if (hitT >= 0 && hitT < 1) {
            m = hitTransform(hitT, hitLeft, footX, footY);
            frame = hitLeft ? HIT_LEFT : HIT_RIGHT;
        } else {
            m = idleTransform(beatPulse, footX, footY);
        }

        // Shadow on the ground stays put while the mascot jumps.
        g.setFill(Color.rgb(0, 0, 0, 0.3));
        g.fillRect(Math.round(footX - width * 0.3), Math.round(footY - SCALE), Math.round(width * 0.6), 2 * SCALE);

        g.save();
        // Matrix3x3 (row-major, p' = M p) to JavaFX Affine (mxx, mxy, tx, myx, myy, ty).
        g.setTransform(new Affine(m.get(0, 0), m.get(0, 1), m.get(0, 2),
                                  m.get(1, 0), m.get(1, 1), m.get(1, 2)));
        g.setImageSmoothing(false);
        g.drawImage(frame, -width / 2, -height, width, height);
        if (frame == SAD) {
            double fall = Math.floor(missT * 6) * SCALE;
            double arcY = -4.0 * (12.0 * SCALE) * missT * (1.0 - missT);

            g.setGlobalAlpha(1.0 - missT);
            g.setFill(PixelArt.BLUE);

            // Linke Träne (nach links gespritzt)
            double leftTearX = -width / 2 + 6 * SCALE - (missT * 8 * SCALE);
            double leftTearY = -height + 5 * SCALE + fall + arcY;
            g.fillRect(leftTearX, leftTearY, SCALE, 2 * SCALE);

            // Rechte Träne (nach rechts gespritzt)
            double rightTearX = -width / 2 + 13 * SCALE + (missT * 8 * SCALE);
            double rightTearY = -height + 5 * SCALE + fall + arcY;
            g.fillRect(rightTearX, rightTearY, SCALE, 2 * SCALE);

            g.setGlobalAlpha(1.0); // Alpha wieder zurücksetzen
        }
        g.restore();
    }

    // ---------------------------------------------------------------- placeholder art

    private static final String[] BODY = {
        ".....KKKKKK.....",
        "...KKWWWWWWKK...",
        "..KWWWWWWWWWWK..",
        "..KWWKWWWWKWWK..",
        ".KWWWKWWWWKWWWK.",
        ".KWPWWWWWWWWPWK.",
        ".KWWWWWKKWWWWWK.",
        ".KKWWWWWWWWWWKK.",
        ".KRKKKKKKKKKKRK.",
        ".KRRGRRRRRRGRRK.",
        ".KRRRRRRRRRRRRK.",
        ".KRRGRRRRRRGRRK.",
        "..KRRRRRRRRRRK..",
        "...KKKKKKKKKK...",
        "....KK....KK....",
    };
    private static final Map<Character, Color> PALETTE = Map.of(
            'K', PixelArt.INK, 'W', PixelArt.CREAM, 'R', PixelArt.RED,
            'G', PixelArt.GOLD, 'P', PixelArt.PINK, 'B', PixelArt.WOOD,
            'T', PixelArt.BLUE);

    static {
        IDLE = PixelArt.override("mascot_idle", build(false, false));
        HIT_LEFT = PixelArt.override("mascot_hit_left", build(true, false));
        HIT_RIGHT = PixelArt.override("mascot_hit_right", build(false, true));
        SAD = PixelArt.override("mascot_sad", buildSad());
    }

    /**
     * TODO (student): the sad frame (sad face, both sticks down), built like {@link #build}.
     * Placeholder: no frame yet.
     */
    private static Image buildSad() {
        char[][] grid = new char[BODY.length][20];
        for (int y = 0; y < BODY.length; y++) {
            String row = ".." + BODY[y] + "..";
            grid[y] = row.toCharArray();
        }
        replaceRow(grid, 3, ".KWWKWKWWKWKWWK.");
        replaceRow(grid, 4, "..KWKKWWWWKKWK..");
        replaceRow(grid, 6, ".KWWWKKRRKKWWWK.");
        grid[5][6] = 'T';  grid[6][6] = 'T';
        grid[5][13] = 'T'; grid[6][13] = 'T';
        stick(grid, false, false);
        stick(grid, false, true);
        String[] rows = new String[grid.length];
        for (int y = 0; y < grid.length; y++) {
            rows[y] = new String(grid[y]);
        }
        return PixelArt.fromRows(PALETTE, rows);
    }

    private static Image build(boolean leftUp, boolean rightUp) {
        char[][] grid = new char[BODY.length][20];
        for (int y = 0; y < BODY.length; y++) {
            String row = ".." + BODY[y] + "..";
            grid[y] = row.toCharArray();
        }
        if (leftUp || rightUp) {   // happy closed eyes and open mouth while drumming
            replaceRow(grid, 4, ".KWWKWKWWKWKWWK.");
            replaceRow(grid, 6, ".KWWWWKRRKWWWWK.");
        }
        stick(grid, leftUp, false);
        stick(grid, rightUp, true);
        String[] rows = new String[grid.length];
        for (int y = 0; y < grid.length; y++) rows[y] = new String(grid[y]);
        return PixelArt.fromRows(PALETTE, rows);
    }

    private static void replaceRow(char[][] grid, int y, String body) {
        grid[y] = (".." + body + "..").toCharArray();
    }

    /** Paints one drum stick, mirrored for the right hand. */
    private static void stick(char[][] grid, boolean raised, boolean right) {
        int[][] down = {{2, 9, 'K'}, {1, 10, 'B'}, {1, 11, 'B'}, {0, 12, 'B'}};
        int[][] up = {{2, 6, 'K'}, {1, 5, 'B'}, {1, 4, 'B'}, {0, 3, 'B'}, {0, 2, 'G'}};
        for (int[] p : raised ? up : down) {
            int x = right ? 19 - p[0] : p[0];
            grid[p[1]][x] = (char) p[2];
        }
    }
}
