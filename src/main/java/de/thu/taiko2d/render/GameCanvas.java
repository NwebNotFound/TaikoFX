package de.thu.taiko2d.render;

import de.thu.taiko2d.game.HealthGauge;
import de.thu.taiko2d.game.Judgement;
import de.thu.taiko2d.model.ChartInfo;
import de.thu.taiko2d.model.Note;
import de.thu.taiko2d.model.NoteType;
import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

/**
 * Gameplay view: lane, notes, festival scene, mascot and health bar. AI-assisted; see docs/AI_USAGE.md.
 */
public final class GameCanvas extends Canvas {
    private double songTime;
    private AnimationTimer timer;
    /** Scroll speed in pixels per ms; set per map with {@link #setScrollSpeed}. */
    private double speed = 0.4;

    //test notes
    private List<Note> notes = List.of();

    // Festival pixel theme, AI-assisted 2026-11-09 (replaces the 2026-10-20 palette).
    private static final Color LANE = Color.web("#2b2142");
    private static final Color MUTED = Color.web("#4a3b66");
    private static final Color CENTRE_BORDER = Color.web("#C62E2E");
    private static final Color RIM_BORDER = Color.web("#77CDFF");

    public GameCanvas() {
        widthProperty().addListener(observable -> draw());
        heightProperty().addListener(observable -> draw());
    }

    // Festival layout (AI-assisted 2026-11-09): sky and lanterns behind, lane at 45 %
    // height, mascot standing on the lane above the drum, health bar above the lane on
    // the right, crowd below the lane.
    private final FestivalScene scene = new FestivalScene();
    private final Mascot mascot = new Mascot();
    private final WaveHealthBar healthBar = new WaveHealthBar();
    private HealthGauge gauge = new HealthGauge();

    /**
     * Window-size scale (AI-generated 2026-11-10): the scene is drawn in design units
     * ({@link #viewWidth()} x {@link #viewHeight()}) and one context scale blows it up to the
     * real canvas, so lane, notes, crowd and text all grow with the window.
     */
    private double uiScale() { return ViewScale.pixelSnapped(getWidth(), getHeight(), SPRITE_SCALE); }

    /** Canvas width in design units (what all layout code in this class works in). */
    public double viewWidth() { return getWidth() / uiScale(); }

    /** Canvas height in design units. */
    public double viewHeight() { return getHeight() / uiScale(); }

    private void draw() {
        if (getWidth() <= 0 || getHeight() <= 0) return;
        GraphicsContext graphics = getGraphicsContext2D();
        graphics.clearRect(0, 0, getWidth(), getHeight());
        graphics.save();
        double ui = uiScale();
        graphics.scale(ui, ui);
        drawScene(graphics, getWidth() / ui, getHeight() / ui);
        graphics.restore();
    }

    private void drawScene(GraphicsContext graphics, double width, double height) {
        double laneY = Math.round(height * 0.45);
        double targetX = width * 0.18;
        double half = SPRITE_PX * SPRITE_SCALE / 2.0 + 14;   // lane grows with the sprites
        boolean fever = gauge.fever();
        boolean kiai = chartInfo != null && chartInfo.isKiai(songTime);
        double pulse = scene.beatPulse(songTime);

        double kiaiStart = chartInfo == null ? Double.NaN : chartInfo.lastKiaiStart(songTime, 1000);
        scene.drawSky(graphics, width, height, songTime, fever, kiai, kiaiStart);
        scene.drawCrowd(graphics, width, laneY + half + 8, height, songTime, gauge.hype(), fever, kiai);

        // Lane: dark track with a chunky outline.
        graphics.setFill(PixelArt.INK);
        graphics.fillRect(0, laneY - half - 4, width, 2 * half + 8);
        graphics.setFill(LANE);   // one steady colour: the lane must stay calm to read notes
        graphics.fillRect(0, laneY - half, width, 2 * half);
        graphics.setFill(MUTED);
        graphics.fillRect(0, laneY - 1, width, 2);

        // Drum panel left of the hit area shows the combo, like on a taiko drum.
        double panelRight = Math.round(targetX - SPRITE_PX * SPRITE_SCALE / 2.0 - 8);
        graphics.setFill(PixelArt.WOOD);
        graphics.fillRect(0, laneY - half, panelRight, 2 * half);
        graphics.setFill(PixelArt.INK);
        graphics.fillRect(panelRight, laneY - half, 4, 2 * half);
        if (combo >= 10) {
            graphics.setTextAlign(TextAlignment.CENTER);
            graphics.setFill(combo >= 50 ? PixelArt.GOLD : PixelArt.CREAM);
            graphics.setFont(Font.font("Consolas", FontWeight.BOLD, 30));
            graphics.fillText(String.valueOf(combo), panelRight / 2, laneY + 6);
            graphics.setFont(Font.font("Consolas", FontWeight.BOLD, 12));
            graphics.fillText("COMBO", panelRight / 2, laneY + 24);
            graphics.setTextAlign(TextAlignment.LEFT);
        }

        drawSprite(graphics, HIT_AREA, targetX, laneY, 1);
        for (int i = 0; i < PRESS_SPRITES.length; i++) {   // several drums can show at once
            double since = songTime - pressMs[i];
            if (since >= 0 && since < PRESS_MS) drawDrumHalf(graphics, PRESS_SPRITES[i], targetX, laneY, i % 2 == 0);
        }
        for (Note note : notes) {
            if (hitFlights.containsKey(note.id())) continue;   // drawn as a flying hit below
            double x = Note.noteX(targetX, note.timeMs(), songTime, speed);
            if (x < -60 || x > width + 60) continue;
            boolean don = note.noteType() == NoteType.DON;
            drawNote(graphics, x, laneY, don, 1);
        }

        mascot.onCombo(combo, songTime);
        mascot.draw(graphics, targetX, laneY - half - 4, songTime, pulse);

        double barX = targetX + SPRITE_PX * SPRITE_SCALE;
        double barWidth = Math.max(120, width - barX - 140);
        healthBar.draw(graphics, barX, laneY - half - 52, barWidth, 24, gauge, songTime, pulse);
        if (fever) {
            graphics.setFill(((long) (songTime / 150)) % 2 == 0 ? PixelArt.GOLD : PixelArt.CREAM);
            graphics.setFont(Font.font("Consolas", FontWeight.BOLD, 20));
            graphics.fillText("FEVER!", barX + barWidth + 16, laneY - half - 32);
        }

        drawHitFlights(graphics, targetX, laneY, width);
        if (lastJudgement != null && songTime - lastJudgementMs < 500) {   // fades out after 0.5 s
            graphics.setFill(switch (lastJudgement) {
                case PERFECT -> PixelArt.GOLD;
                case GOOD -> RIM_BORDER;
                case MISS -> CENTRE_BORDER;
            });
            graphics.setFont(Font.font("Consolas", FontWeight.BOLD, 26));
            graphics.setTextAlign(TextAlignment.CENTER);
            graphics.fillText(lastJudgement.name(), targetX, laneY + half + 34);
            graphics.setTextAlign(TextAlignment.LEFT);
        }
        drawOutlinedText(graphics, String.format("%08d", score), width - 16, 40, 26, TextAlignment.RIGHT);
        if (skipHint != null) {   // long intro or break: offer the skip
            drawOutlinedText(graphics, skipHint, width - 16, height - 20, 18, TextAlignment.RIGHT);
        }
    }

    private static void drawOutlinedText(GraphicsContext g, String text, double x, double y, double size,
                                         TextAlignment align) {
        g.setFont(Font.font("Consolas", FontWeight.BOLD, size));
        g.setTextAlign(align);
        g.setFill(PixelArt.INK);
        g.fillText(text, x + 3, y + 3);   // hard pixel shadow
        g.setFill(PixelArt.CREAM);
        g.fillText(text, x, y);
        g.setTextAlign(TextAlignment.LEFT);
    }

    // Sprites (32x32 pixel art, student-made in Aseprite). Loading code AI-assisted 2026-11-05.
    // Change this to resize every sprite. Keep it a whole number (2, 3, 4) so pixels stay crisp.
    private static final int SPRITE_SCALE = 3;
    private static final int SPRITE_PX = 32;

    private static final Image RED_NOTE = PixelArt.fromRows(Map.of(
            'K', PixelArt.INK,
            'R', PixelArt.RED,
            'W', PixelArt.CREAM),
        "....KKKKKKKK....",
        "..KKRRRRRRRRKK..",
        ".KRRWWWWWRRRRRK.",
        ".KRRWWWWWRRRRRK.",
        "KRRRRRRRRRRRRRRK",
        "KRRRRRRRRRRRRRRK",
        "KRRRRRRRRRRRRRRK",
        "KRRRRRRRRRRRRRRK",
        "KRRRRRRRRRRRRRRK",
        "KRRRRRRRRRRRRRRK",
        "KRRRRRRRRRRRRRRK",
        "KRRRRRRRRRRRRRRK",
        ".KRRRRRRRRRRRRK.",
        ".KRRRRRRRRRRRRK.",
        "..KKRRRRRRRRKK..",
        "....KKKKKKKK...."
    );

    private static final Image BLUE_NOTE = PixelArt.fromRows(Map.of(
            'K', PixelArt.INK,
            'B', PixelArt.BLUE,
            'W', PixelArt.CREAM),
        "....KKKKKKKK....",
        "..KKBBBBBBBBKK..",
        ".KBBWWWWWWBBBBK.",
        ".KBBWWWWWWBBBBK.",
        "KBBBBBBBBBBBBBBK",
        "KBBBBBBBBBBBBBBK",
        "KBBBBBBBBBBBBBBK",
        "KBBBBBBBBBBBBBBK",
        "KBBBBBBBBBBBBBBK",
        "KBBBBBBBBBBBBBBK",
        "KBBBBBBBBBBBBBBK",
        "KBBBBBBBBBBBBBBK",
        ".KBBBBBBBBBBBBK.",
        ".KBBBBBBBBBBBBK.",
        "..KKBBBBBBBBKK..",
        "....KKKKKKKK...."
    );

    private static final Image HIT_AREA = PixelArt.fromRows(Map.of(
            'K', PixelArt.INK,
            'W', PixelArt.CREAM),
        "....KKKKKKKK....",
        "..KKWWWWWWWWKK..",
        ".KWWKKKKKKKKWWK.",
        ".KWK........KWK.",
        "KWK..........KWK",
        "KWK..........KWK",
        "KWK..........KWK",
        "KWK..........KWK",
        "KWK..........KWK",
        "KWK..........KWK",
        "KWK..........KWK",
        "KWK..........KWK",
        ".KWK........KWK.",
        ".KWWKKKKKKKKWWK.",
        "..KKWWWWWWWWKK..",
        "....KKKKKKKK...."
    );

    private static final Image HIT_BIG_LEFT = PixelArt.fromRows(Map.of(
            'K', PixelArt.INK,
            'O', Color.web("#f7b733")),
        "....KKKK........",
        "..KKOOOOKK......",
        ".KOOOOOOOOK.....",
        ".KOOOOOOOOK.....",
        "KOOOOOOOOOOK....",
        "KOOOOOOOOOOK....",
        "KOOOOOOOOOOK....",
        "KOOOOOOOOOOK....",
        "KOOOOOOOOOOK....",
        "KOOOOOOOOOOK....",
        "KOOOOOOOOOOK....",
        "KOOOOOOOOOOK....",
        ".KOOOOOOOOK.....",
        ".KOOOOOOOOK.....",
        "..KKOOOOKK......",
        "....KKKK........"
    );

    private static final Image HIT_BIG_RIGHT = PixelArt.fromRows(Map.of(
            'K', PixelArt.INK,
            'O', Color.web("#f7b733")),
        "........KKKK....",
        "......KKOOOOKK..",
        ".....KOOOOOOOOK.",
        ".....KOOOOOOOOK.",
        "....KOOOOOOOOOOK",
        "....KOOOOOOOOOOK",
        "....KOOOOOOOOOOK",
        "....KOOOOOOOOOOK",
        "....KOOOOOOOOOOK",
        "....KOOOOOOOOOOK",
        "....KOOOOOOOOOOK",
        "....KOOOOOOOOOOK",
        ".....KOOOOOOOOK.",
        ".....KOOOOOOOOK.",
        "......KKOOOOKK..",
        "........KKKK...."
    );

    private static final Image HIT_SMALL_LEFT = PixelArt.fromRows(Map.of(
            'K', PixelArt.INK,
            'R', PixelArt.RED),
        "................",
        "......KKKK......",
        "....KKRRRRKK....",
        "...KRRRRRRRRK...",
        "..KRRRRRRRRRRK..",
        "..KRRRRRRRRRRK..",
        "..KRRRRRRRRRRK..",
        "..KRRRRRRRRRRK..",
        "..KRRRRRRRRRRK..",
        "..KRRRRRRRRRRK..",
        "..KRRRRRRRRRRK..",
        "..KRRRRRRRRRRK..",
        "...KRRRRRRRRK...",
        "....KKRRRRKK....",
        "......KKKK......",
        "................"
    );

    private static final Image HIT_SMALL_RIGHT = PixelArt.fromRows(Map.of(
            'K', PixelArt.INK,
            'B', PixelArt.BLUE),
        "................",
        "......KKKK......",
        "....KKBBBBKK....",
        "...KBBBBBBBBK...",
        "..KBBBBBBBBBBK..",
        "..KBBBBBBBBBBK..",
        "..KBBBBBBBBBBK..",
        "..KBBBBBBBBBBK..",
        "..KBBBBBBBBBBK..",
        "..KBBBBBBBBBBK..",
        "..KBBBBBBBBBBK..",
        "..KBBBBBBBBBBK..",
        "...KBBBBBBBBK...",
        "....KKBBBBKK....",
        "......KKKK......",
        "................"
    );

    private static Image loadSprite(String name) {
        return new Image(Objects.requireNonNull(
                GameCanvas.class.getResourceAsStream("/de/thu/taiko2d/sprites/" + name), name));
    }

    /** Draws a sprite centred on (x, y). {@code scale} multiplies SPRITE_SCALE (1 = normal). */
    private void drawSprite(GraphicsContext graphics, Image sprite, double x, double y, double scale) {
        // Whole-number multiple of 32 px and whole-pixel position: no uneven or shimmering pixels.
        long size = SPRITE_PX * Math.max(1, Math.round(SPRITE_SCALE * scale));
        graphics.setImageSmoothing(false);   // nearest-neighbour
        graphics.drawImage(sprite, Math.round(x - size / 2.0), Math.round(y - size / 2.0), size, size);
    }

    /**
     * Draws only the left or right half of a drum-hit sprite, at the matching half of the
     * hit area, so a left and a right press never cover each other.
     */
    private void drawDrumHalf(GraphicsContext graphics, Image sprite, double x, double y, boolean left) {
        long size = SPRITE_PX * SPRITE_SCALE;
        double half = sprite.getWidth() / 2;
        double dx = Math.round(x - size / 2.0) + (left ? 0 : size / 2.0);
        graphics.setImageSmoothing(false);
        graphics.drawImage(sprite, left ? 0 : half, 0, half, sprite.getHeight(),
                dx, Math.round(y - size / 2.0), size / 2.0, size);
    }

    // Key-press feedback: the drum sprite shows briefly at the hit area on every D/F/J/K press.
    // One slot per drum half, so pressing two keys together shows both.
    private static final double PRESS_MS = 120;
    private static final Image[] PRESS_SPRITES = {HIT_BIG_LEFT, HIT_BIG_RIGHT, HIT_SMALL_LEFT, HIT_SMALL_RIGHT};
    private final double[] pressMs = {Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY,
                                      Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};

    public void showKeyPress(NoteType type, boolean left, double atMs) {
        int slot = (type == NoteType.DON ? 0 : 2) + (left ? 0 : 1);
        pressMs[slot] = atMs;
        mascot.onDrumHit(left, atMs);
    }

    private void drawNote(GraphicsContext graphics, double x, double y, boolean don, double scale) {
        drawSprite(graphics, don ? RED_NOTE : BLUE_NOTE, x, y, scale);
    }

    // Hit-note flight: a hit note arcs back in the direction it came from, grows
    // slightly and fades out. The method skeleton (flight map, noteHit, timing) was
    // AI-assisted 2026-11-04; the arc/scale motion via Matrix3x3 is the student's work.
    private static final double FLIGHT_MS = 450;
    private final Map<Integer, double[]> hitFlights = new HashMap<>();   // id -> {hitMs, hitX}

    public void noteHit(Note note, double atMs) {
        if (note == null) return;
        double hitX = viewWidth() * 0.18 +(note.timeMs() - atMs) * speed;
        hitFlights.put(note.id(), new double[] {atMs, hitX});
    }

    private void drawHitFlights(GraphicsContext graphics, double targetX, double laneY, double width) {
        for (Note note : notes) {
            double[] flight = hitFlights.get(note.id());
            if (flight == null) continue;
            double t = (songTime - flight[0]) / FLIGHT_MS;
            if (t < 0 || t > 1) continue;
            double ease = 1 - (1 - t) * (1 - t);                 // fast start, slows down

            double D = width * 0.30;
            double H = 150;

            double x0 = flight[1];
            double y0 = laneY;
            double dx = D*t; // x movement over time
            double dy = 4*H*t*(1-t); // arch movment
            double s = 1 - 0.3 * t; // Size behavior on hit


            //Matrix Chain
            Matrix3x3 m = Matrix3x3.translate(x0-dx, y0-dy).multiply(Matrix3x3.scale(s,s)).multiply(Matrix3x3.translate(-x0,-y0));
            double[] p = m.transform(x0,y0);


            boolean don = note.noteType() == NoteType.DON;
            graphics.setGlobalAlpha(1-t*t*t);
            drawNote(graphics, p[0], p[1], don, s);
            graphics.setGlobalAlpha(1);
        }
    }

    public void start(List<Note> notes, DoubleSupplier clock, DoubleConsumer onFrame) {
        this.notes = notes;
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                songTime = clock.getAsDouble();
                onFrame.accept(songTime);
                draw();
            }
        };
        timer.start();
    }

    public void stop() {
        if (timer != null) {
            timer.stop();
        }
    }

    private Judgement lastJudgement;
    private double lastJudgementMs;
    private int score;
    private int combo;

    public void showJudgement(Judgement judgement, double atMs) {
        lastJudgement = judgement;
        lastJudgementMs = atMs;
        if (judgement == Judgement.MISS) mascot.onMiss(atMs);
    }

    public void setStats(int score, int combo) {
        this.score = score;
        this.combo = combo;
    }

    // Per-map settings, AI-assisted 2026-11-09.
    public void setScrollSpeed(double pixelsPerMs) { this.speed = pixelsPerMs; }

    public void setTiming(double bpm, double gridStartMs) { scene.setTiming(bpm, gridStartMs); }

    private ChartInfo chartInfo;
    private String skipHint;

    /** Text of the skip hint in the corner, or null to hide it. */
    public void setSkipHint(String hint) { this.skipHint = hint; }

    /** Beat timing and kiai sections of the chart being played. */
    public void setChartInfo(ChartInfo info) {
        this.chartInfo = info;
        scene.setTiming(info.bpm(), info.gridStartMs());
    }

    public void setGauge(HealthGauge gauge) { this.gauge = gauge; }



}
