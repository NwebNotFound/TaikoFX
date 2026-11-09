package de.thu.taiko2d.render;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

/**
 * Night-festival background in blocky pixels: sky, stars, a yagura tower, a string
 * of paper lanterns that blink on the beat and a crowd that dances along. More
 * people join as the combo grows; during fever everyone jumps and the lanterns flash.
 *
 * AI-generated 2026-11-09 as placeholder art, see docs/AI_USAGE.md. The dancer
 * sprites can be replaced with the student's PNGs (dancer_a_down.png, dancer_a_up.png, ...).
 */
final class FestivalScene {
    private static final int P = 4;   // art pixel size on screen

    private static final Color SKY_TOP = Color.web("#120d24");
    private static final Color SKY_BOTTOM = Color.web("#3d2a5c");
    private static final Color STAR = Color.web("#f6e7c8");
    private static final Color HILL = Color.web("#24183a");
    private static final Color GROUND = Color.web("#3a2433");
    private static final Color GROUND_LINE = Color.web("#5a3442");
    private static final Color LANTERN_OFF = Color.web("#a4262c");
    private static final Color LANTERN_ON = Color.web("#ff6a4d");
    private static final Color[] FEVER_LANTERNS = {
        Color.web("#f7b733"), Color.web("#ff6a4d"), Color.web("#3fa7e8"), Color.web("#f59bb0")};

    private static final Map<Character, Color> BASE = Map.of('K', PixelArt.INK, 'S', PixelArt.SKIN,
            'H', Color.web("#2a1a14"), 'W', PixelArt.CREAM);
    private static final String[] DOWN = {
        "..KKKK..",
        ".KHHHHK.",
        ".KKSSKK.",
        ".KSSSSK.",
        "..KKKK..",
        ".KCCCCK.",
        "KSCCCCSK",
        ".KCOOCK.",
        ".KCCCCK.",
        ".KCCCCK.",
        "..KKKK..",
        "..K..K..",
    };
    private static final String[] UP = {
        "S.KKKK.S",
        "SKHHHHKS",
        "KKKSSKKK",
        ".KSSSSK.",
        "..KKKK..",
        ".KCCCCK.",
        ".KCCCCK.",
        ".KCOOCK.",
        ".KCCCCK.",
        ".KCCCCK.",
        "..KKKK..",
        "..K..K..",
    };
    /** Yukata/happi colours (C) and sash colours (O) for the different people. */
    private static final String[][] OUTFITS = {
        {"#3fa7e8", "#f6e7c8"}, {"#e8433a", "#f7b733"}, {"#f59bb0", "#7a4b2a"},
        {"#5fbf6a", "#1a1423"}, {"#f7b733", "#e8433a"}, {"#8e5bd6", "#f6e7c8"},
    };
    private static final List<Image[]> DANCERS = new ArrayList<>();

    static {
        for (int i = 0; i < OUTFITS.length; i++) {
            var palette = new java.util.HashMap<>(BASE);
            palette.put('C', Color.web(OUTFITS[i][0]));
            palette.put('O', Color.web(OUTFITS[i][1]));
            char id = (char) ('a' + i);
            DANCERS.add(new Image[] {
                PixelArt.override("dancer_" + id + "_down", PixelArt.fromRows(palette, DOWN)),
                PixelArt.override("dancer_" + id + "_up", PixelArt.fromRows(palette, UP))});
        }
    }

    private double beatMs = 500;
    private double gridStartMs;

    void setTiming(double bpm, double gridStartMs) {
        this.beatMs = 60_000.0 / (bpm > 0 ? bpm : 120);
        this.gridStartMs = gridStartMs;
    }

    /** Position within the current beat, 0 right on the beat, approaching 1 just before the next. */
    double beatPhase(double nowMs) {
        double beats = (nowMs - gridStartMs) / beatMs;
        return beats - Math.floor(beats);
    }

    long beatIndex(double nowMs) {
        return (long) Math.floor((nowMs - gridStartMs) / beatMs);
    }

    /** 1 on the beat, fading quickly to 0. */
    double beatPulse(double nowMs) {
        double phase = beatPhase(nowMs);
        return Math.max(0, 1 - phase * 3);
    }

    /** Draws the sky and lanterns (behind the lane). */
    void drawSky(GraphicsContext g, double width, double height, double nowMs, boolean fever, boolean kiai,
                 double kiaiStartMs) {
        // Banded gradient, one band per 8 art pixels, so it reads as pixel art.
        int bands = Math.max(1, (int) (height / (8 * P)));
        for (int i = 0; i < bands; i++) {
            g.setFill(SKY_TOP.interpolate(SKY_BOTTOM, i / (double) bands));
            g.fillRect(0, i * 8 * P, width, 8 * P + 1);
        }
        // Stars: fixed pseudo-random spots that twinkle.
        for (int i = 0; i < 40; i++) {
            long h = hash(i);
            double sx = snap((h & 0xffff) / 65535.0 * width);
            double sy = snap(((h >> 16) & 0xffff) / 65535.0 * height * 0.35);
            boolean twinkle = ((long) (nowMs / 400) + i) % 7 == 0;
            g.setFill(twinkle ? Color.WHITE : STAR.deriveColor(0, 1, 1, 0.6));
            g.fillRect(sx, sy, P, P);
        }
        // Moon.
        double mx = snap(width * 0.82), my = snap(height * 0.08);
        g.setFill(Color.web("#fff1c4"));
        g.fillRect(mx, my + P, 6 * P, 4 * P);
        g.fillRect(mx + P, my, 4 * P, 6 * P);
        // Kiai (the song's highlight, from osu! maps): fireworks every two beats, all in the
        // sky above the hills, so nothing flashes near the lane.
        if (kiai) drawFireworks(g, width, height, nowMs);
        // Hills and the yagura tower silhouette.
        g.setFill(HILL);
        for (double x = 0; x < width; x += P) {
            double hill = height * 0.36 + Math.round(Math.sin(x / 90) * 3 + Math.sin(x / 37) * 1.5) * P;
            g.fillRect(x, snap(hill), P, height);
        }
        double tx = snap(width * 0.62), ty = snap(height * 0.18);
        g.fillRect(tx - 10 * P, ty, 20 * P, 3 * P);             // roof
        g.fillRect(tx - 8 * P, ty - P, 16 * P, P);
        g.fillRect(tx - 7 * P, ty + 3 * P, 2 * P, height);       // legs
        g.fillRect(tx + 5 * P, ty + 3 * P, 2 * P, height);
        g.fillRect(tx - 7 * P, ty + 10 * P, 14 * P, 2 * P);      // platform
        // A kiai starts: fountains spray from the yagura roof for a few seconds.
        drawFountains(g, width, height, nowMs, kiaiStartMs);

        // Lantern string across the top, sagging between poles.
        int count = Math.max(4, (int) (width / 90));
        double spacing = width / count;
        long beat = beatIndex(nowMs);
        double pulse = beatPulse(nowMs);
        for (int i = 0; i <= count; i++) {
            double lx = snap(i * spacing + spacing / 2);
            double sag = Math.sin(Math.PI * ((lx % spacing) / spacing)) * 3 * P;
            double ly = snap(height * 0.04 + sag);
            g.setFill(PixelArt.INK);
            g.fillRect(lx - spacing / 2, snap(height * 0.04), spacing, P / 2.0);
            boolean on = kiai || (beat + i) % 2 == 0;   // kiai: every lantern lit
            Color c = fever ? FEVER_LANTERNS[(int) Math.floorMod(beat + i, FEVER_LANTERNS.length)]
                            : on ? LANTERN_ON : LANTERN_OFF;
            if (on || fever) {   // glow
                g.setFill(c.deriveColor(0, 1, 1, 0.18 + 0.2 * pulse));
                g.fillRect(lx - 5 * P, ly - P, 10 * P, 11 * P);
            }
            drawLantern(g, lx, ly, c);
        }
    }

    private static void drawLantern(GraphicsContext g, double x, double y, Color body) {
        g.setFill(PixelArt.INK);
        g.fillRect(x - 2 * P, y, 4 * P, 9 * P);
        g.fillRect(x - 3 * P, y + P, 6 * P, 7 * P);
        g.setFill(body);
        g.fillRect(x - 2 * P, y + 2 * P, 4 * P, 5 * P);
        g.fillRect(x - P, y + P, 2 * P, 7 * P);
        g.setFill(PixelArt.GOLD);
        g.fillRect(x - P, y, 2 * P, P);
        g.fillRect(x - P, y + 8 * P, 2 * P, P);
        g.setFill(PixelArt.INK.deriveColor(0, 1, 1, 0.5));
        g.fillRect(x - 2 * P, y + 4 * P, 4 * P, P / 2.0);   // rib line
    }

    /** Rocket flight and burst time of one firework. */
    private static final double ROCKET_MS = 350;
    private static final double BURST_MS = 1100;

    /**
     * Kiai fireworks (reworked 2026-11-09: the old one-per-bar burst was too faint to notice).
     * One shot every two beats: a rocket rises from the hills, then bursts into two rings.
     */
    private void drawFireworks(GraphicsContext g, double width, double height, double nowMs) {
        long shot = Math.floorDiv(beatIndex(nowMs), 2);
        for (long s = shot - 1; s <= shot; s++) {
            double local = nowMs - (gridStartMs + s * 2 * beatMs);
            if (local < 0 || local >= ROCKET_MS + BURST_MS) continue;
            long h = hash((int) s);
            // Right of the mascot, high in the sky above the health bar.
            double cx = snap((0.32 + 0.62 * ((h & 0xffff) / 65535.0)) * width);
            double cy = (0.07 + 0.08 * (((h >> 16) & 0xffff) / 65535.0)) * height;
            Color color = FEVER_LANTERNS[(int) Math.floorMod(h >> 32, FEVER_LANTERNS.length)];
            if (local < ROCKET_MS) {
                double p = local / ROCKET_MS;
                double y = height * 0.34 + (cy - height * 0.34) * (1 - (1 - p) * (1 - p));   // slows near the top
                for (int k = 0; k < 4; k++) {                                                 // rocket + fading trail
                    g.setGlobalAlpha(1 - k * 0.25);
                    g.setFill(k == 0 ? Color.WHITE : color);
                    g.fillRect(cx, snap(y + k * 2 * P), P, P);
                }
                g.setGlobalAlpha(1);
                continue;
            }
            double p = (local - ROCKET_MS) / BURST_MS;
            double radius = (1 - Math.pow(1 - p, 3)) * 12 * P;   // fast burst, slows down
            double fall = p * p * 4 * P;                          // sparks sink
            g.setGlobalAlpha(0.95 * (1 - p));
            if (p < 0.1) {                                        // bright flash at the centre
                g.setFill(Color.WHITE);
                g.fillRect(cx - P, snap(cy) - P, 3 * P, 3 * P);
            }
            for (int k = 0; k < 16; k++) {
                double angle = k * Math.PI / 8;
                double cos = Math.cos(angle), sin = Math.sin(angle);
                g.setFill(color);
                g.fillRect(snap(cx + cos * radius), snap(cy + sin * radius + fall), P, P);
                g.setFill(color.deriveColor(0, 1, 1, 0.45));     // trail behind each spark
                g.fillRect(snap(cx + cos * radius * 0.8), snap(cy + sin * radius * 0.8 + fall), P, P);
                if (k % 2 == 0) {                                 // white inner ring, turned half a step
                    double a2 = angle + Math.PI / 16;
                    g.setFill(Color.WHITE);
                    g.fillRect(snap(cx + Math.cos(a2) * radius * 0.55), snap(cy + Math.sin(a2) * radius * 0.55 + fall), P, P);
                }
            }
            g.setGlobalAlpha(1);
        }
    }

    /** The fountains spray this long after a kiai starts; each spark lives SPARK_LIFE_MS. */
    private static final double FOUNTAIN_MS = 3500;
    private static final double SPARK_LIFE_MS = 900;
    private static final double EMIT_EVERY_MS = 20;
    /** Gravity for the fountain sparks, in pixels per ms². */
    private static final double GRAVITY = 0.0004;

    /**
     * Two fountain fireworks on the yagura roof (added 2026-11-09). Stateless: spark i was
     * launched at {@code start + i * EMIT_EVERY_MS} with a fixed pseudo-random velocity, so its
     * position at any time is the projectile formula x = x0 + vx·t, y = y0 − vy·t + ½·g·t².
     */
    private void drawFountains(GraphicsContext g, double width, double height, double nowMs, double startMs) {
        double since = nowMs - startMs;
        if (!(since >= 0 && since < FOUNTAIN_MS + SPARK_LIFE_MS)) return;   // also false for NaN
        double baseY = snap(height * 0.18) - 2 * P;                         // just above the roof
        double[] nozzles = {snap(width * 0.62) - 8 * P, snap(width * 0.62) + 8 * P};
        long first = Math.max(0, (long) Math.ceil((since - SPARK_LIFE_MS) / EMIT_EVERY_MS));
        long last = (long) Math.min(Math.floor(since / EMIT_EVERY_MS), FOUNTAIN_MS / EMIT_EVERY_MS);
        for (long i = first; i <= last; i++) {
            double age = since - i * EMIT_EVERY_MS;
            if (age < 0 || age >= SPARK_LIFE_MS) continue;
            for (int n = 0; n < nozzles.length; n++) {
                long h = hash((int) (i * 2 + n) + 7919);
                double outwards = n == 0 ? -1 : 1;                             // left nozzle leans left
                double vx = (0.02 + 0.06 * ((h & 0xff) / 255.0)) * outwards;   // px per ms
                double peak = height * (0.08 + 0.05 * (((h >> 8) & 0xff) / 255.0));
                double vy = Math.sqrt(2 * GRAVITY * peak);                     // rises exactly to the peak
                double x = nozzles[n] + vx * age;
                double y = baseY - vy * age + 0.5 * GRAVITY * age * age;
                if (y > baseY) continue;                                       // fell back to the roof
                double life = age / SPARK_LIFE_MS;
                g.setGlobalAlpha(0.9 * (1 - life));
                g.setFill(life < 0.3 ? Color.WHITE : ((h >> 16) & 1) == 0 ? PixelArt.GOLD : LANTERN_ON);
                g.fillRect(snap(x), snap(y), P, P);
            }
        }
        g.setGlobalAlpha(1);
    }

    /** Idol-concert glowstick colours; the whole crowd switches colour every 4 beats. */
    private static final Color[] GLOW = {
        Color.web("#ff4fa3"), Color.web("#4fd8ff"), Color.web("#b6ff4f"), Color.web("#ffe14f"), Color.web("#c77dff")};

    /** One place in the crowd: horizontal position 0..1, depth 0 (far) .. 1 (near). */
    private record Spot(double fx, double depth, int outfit, int beatOffset, long joinOrder) {}

    private static final int CROWD_DEPTH_ROWS = 4;
    private List<Spot> spots = List.of();
    private int spotColumns = -1;

    /**
     * Scattered spots on a jittered grid: CROWD_DEPTH_ROWS depth bands, columns by
     * window width, each spot nudged by a fixed pseudo-random amount so nobody
     * stands in a straight line. Sorted far to near so near people overlap far ones.
     */
    private List<Spot> spots(double width) {
        int columns = Math.max(4, (int) (width / 80));
        if (columns == spotColumns) return spots;
        List<Spot> list = new ArrayList<>();
        for (int row = 0; row < CROWD_DEPTH_ROWS; row++) {
            for (int col = 0; col < columns; col++) {
                int id = row * 1000 + col;
                long h = hash(id);
                double jitterX = ((h & 0xff) / 255.0 - 0.5) * 0.9;
                double jitterY = (((h >> 8) & 0xff) / 255.0 - 0.5) * 0.8;
                double stagger = row % 2 == 0 ? 0 : 0.5;
                double fx = (col + 0.5 + stagger + jitterX) / columns;
                double depth = (row + 0.5 + jitterY) / CROWD_DEPTH_ROWS;
                list.add(new Spot(fx - Math.floor(fx), Math.max(0, Math.min(1, depth)),
                        (int) ((h >> 16) & 0xff) % DANCERS.size(), (int) ((h >> 24) & 1), hash(id + 7)));
            }
        }
        list.sort(Comparator.comparingDouble(Spot::depth));
        spots = List.copyOf(list);
        spotColumns = columns;
        walk = new double[spots.size()];   // window resized: everyone starts off-screen again
        return spots;
    }

    /** Walking speed of the crowd, in screen pixels per ms. */
    private static final double WALK_PX_PER_MS = 0.25;
    /** Per spot: 0 = off-screen at the nearer side edge, 1 = standing at the spot. */
    private double[] walk = new double[0];
    private double lastCrowdMs = Double.NaN;

    /**
     * Draws the festival ground and the crowd in the lower part of the screen. The
     * crowd grows with {@code hype} (how well the player is doing): people walk in from
     * the nearer side of the screen to random spots all over the ground, and walk off
     * again when hype drops. During fever everyone comes and does a glowstick dance.
     */
    void drawCrowd(GraphicsContext g, double width, double top, double height, double nowMs,
                   double hype, boolean fever, boolean kiai) {
        top = snap(top);
        g.setFill(GROUND);
        g.fillRect(0, top, width, height - top);
        g.setFill(GROUND_LINE);
        g.fillRect(0, top, width, P);

        List<Spot> all = spots(width);
        int capacity = all.size();
        int people = fever ? capacity : 2 + (int) Math.round(Math.max(0, Math.min(1, hype)) * (capacity - 2));
        // The first `people` spots in join order are present; join order is random, so the crowd
        // fills in everywhere at once. Always keep the same people when hype changes.
        List<Long> order = all.stream().map(Spot::joinOrder).sorted().toList();
        long cutoff = order.get(Math.min(people, capacity) - 1);

        double farFeet = top + 12 * 3 + 2 * P;           // far people's heads stay below the lane
        double nearFeet = height - P;
        long beat = beatIndex(nowMs);
        double pulse = beatPulse(nowMs);
        double elapsed = Double.isNaN(lastCrowdMs) ? 0 : Math.max(0, Math.min(100, nowMs - lastCrowdMs));
        lastCrowdMs = nowMs;
        g.setImageSmoothing(false);
        for (int i = 0; i < all.size(); i++) {
            Spot spot = all.get(i);
            int scale = 3 + (int) Math.round(spot.depth() * 2);   // 3 far .. 5 near
            double w = 8 * scale, h = 12 * scale;
            double spotX = spot.fx() * width;
            boolean fromLeft = spot.fx() < 0.5;
            double edgeX = fromLeft ? -w : width + w;

            // Walk towards the spot if this person should be here, else back to the edge.
            // Moving `walk` by distance-scaled steps gives everyone the same walking speed.
            boolean wanted = spot.joinOrder() <= cutoff;
            double step = elapsed * WALK_PX_PER_MS / Math.max(1, Math.abs(spotX - edgeX));
            walk[i] = wanted ? Math.min(1, walk[i] + step) : Math.max(0, walk[i] - step);
            if (walk[i] <= 0) continue;
            boolean arrived = walk[i] >= 1;

            double x = snap(edgeX + (spotX - edgeX) * walk[i]);
            double groundY = snap(farFeet + spot.depth() * (nearFeet - farFeet));
            Image[] dancer = DANCERS.get(spot.outfit());
            boolean up;
            double jump;
            if (arrived) {
                up = fever || kiai || (beat + spot.beatOffset()) % 2 == 0;
                jump = fever || kiai ? snap(pulse * 3 * P) : (up ? P : 0);   // kiai: everyone jumps on the beat
            } else {
                // Walking: a quick two-step bob, faster than the beat dance.
                boolean stepPhase = ((long) (nowMs / 140) + spot.beatOffset()) % 2 == 0;
                up = false;
                jump = stepPhase ? P : 0;
            }
            double y = groundY - h - jump;
            boolean glowsticks = fever && arrived;
            g.setGlobalAlpha(0.7 + 0.3 * spot.depth());   // far people slightly faded
            g.setFill(Color.rgb(0, 0, 0, 0.3));
            g.fillRect(x - w / 2 + P, groundY - P, w - 2 * P, P);
            // Fever: idol-style glowstick dance. Glow goes behind the dancer, sticks in front.
            // Hands of the arms-up frame are at art pixels (0, 0) and (7, 0).
            Color glow = GLOW[(int) Math.floorMod(beat / 4, GLOW.length)];
            double swing = (beat % 2 == 0 ? -1 : 1) * Math.toRadians(35);   // everyone sways together
            double leftHand = x - w / 2 + scale / 2.0, rightHand = x - w / 2 + 7.5 * scale, hands = y + scale / 2.0;
            if (glowsticks) {
                drawGlowstick(g, leftHand, hands, swing - 0.25, scale, glow, true);
                drawGlowstick(g, rightHand, hands, swing + 0.25, scale, glow, true);
            }
            g.drawImage(up ? dancer[1] : dancer[0], x - w / 2, y, w, h);
            if (glowsticks) {
                drawGlowstick(g, leftHand, hands, swing - 0.25, scale, glow, false);
                drawGlowstick(g, rightHand, hands, swing + 0.25, scale, glow, false);
            }
        }
        g.setGlobalAlpha(1);
    }

    /** A glowstick of 5 art pixels from the hand, pointing up and tilted by {@code angle}. */
    private static void drawGlowstick(GraphicsContext g, double handX, double handY, double angle,
                                      int scale, Color color, boolean haloOnly) {
        double dx = Math.sin(angle), dy = -Math.cos(angle);
        for (int k = 1; k <= 5; k++) {
            double px = Math.floor((handX + dx * k * scale) / scale) * scale;
            double py = Math.floor((handY + dy * k * scale) / scale) * scale;
            if (haloOnly) {
                g.setFill(color.deriveColor(0, 1, 1, 0.22));
                g.fillRect(px - scale, py - scale, 3 * scale, 3 * scale);
            } else {
                g.setFill(k == 5 ? Color.WHITE : color);
                g.fillRect(px, py, scale, scale);
            }
        }
    }

    private static double snap(double v) { return Math.floor(v / P) * P; }

    private static long hash(int i) {
        long h = i * 0x9E3779B97F4A7C15L;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        return h ^ (h >>> 32);
    }
}
