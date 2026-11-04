package de.thu.taiko2d.render;

import de.thu.taiko2d.game.Judgement;
import de.thu.taiko2d.model.Note;
import de.thu.taiko2d.model.NoteType;
import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

/**
 * Static, resizable lane preview. AI-assisted bootstrap; see docs/AI_USAGE.md.
 */
public final class GameCanvas extends Canvas {
    private double songTime;
    private AnimationTimer timer;
    private static final double speed = 0.4;

    //test notes
    private List<Note> notes = List.of();

    // User-selected palettes, applied with AI assistance on 2026-10-20.
    private static final Color LANE = Color.web("#393E46");
    private static final Color MUTED = Color.web("#948979");
    private static final Color TARGET = Color.web("#DFD0B8");
    private static final Color CENTRE = Color.web("#F95454");
    private static final Color CENTRE_BORDER = Color.web("#C62E2E");
    private static final Color RIM = Color.web("#0D92F4");
    private static final Color RIM_BORDER = Color.web("#77CDFF");

    public GameCanvas() {
        widthProperty().addListener(observable -> draw());
        heightProperty().addListener(observable -> draw());
    }

    private void draw() {
        double width = getWidth();
        double height = getHeight();
        if (width <= 0 || height <= 0) return;
        GraphicsContext graphics = getGraphicsContext2D();
        graphics.clearRect(0, 0, width, height);
        double laneY = height / 2;
        double targetX = width * 0.18;
        graphics.setFill(LANE);
        graphics.fillRoundRect(0, laneY - 52, width, 104, 18, 18);
        graphics.setStroke(MUTED);
        graphics.setLineWidth(1);
        graphics.strokeLine(0, laneY, width, laneY);
        graphics.setStroke(TARGET);
        graphics.setLineWidth(3);
        graphics.strokeOval(targetX - 33, laneY - 33, 66, 66);
        graphics.setFill(TARGET);
        for (Note note : notes) {
            if (hitFlights.containsKey(note.id())) continue;   // drawn as a flying hit below
            double x = Note.noteX(targetX, note.timeMs(), songTime, speed);
            if (x < -30 || x > width + 30) continue;
            boolean don = note.noteType() == NoteType.DON;
            drawNote(graphics, x, laneY, don ? CENTRE : RIM, don ? CENTRE_BORDER : RIM_BORDER, 1);
        }
        drawHitFlights(graphics, targetX, laneY, width);
        graphics.setFill(TARGET);
        if (lastJudgement != null && songTime - lastJudgementMs < 500) {   // fades out after 0.5 s
        graphics.setFill(switch (lastJudgement) {
            case PERFECT -> TARGET;
            case GOOD -> RIM_BORDER;
            case MISS -> CENTRE_BORDER;
        });
        graphics.setFont(Font.font("Segoe UI", 28));
        graphics.fillText(lastJudgement.name(), targetX - 45, laneY - 70);
        }
        graphics.setFill(TARGET);
        graphics.setFont(Font.font("Segoe UI", 18));
        graphics.fillText("Score " + score, width - 150, 32);
        if (combo > 1) {
            graphics.fillText(combo + "x combo", 16, height - 24);
        }
    }

    private void drawNote(GraphicsContext graphics, double x, double y, Color fill, Color border, double scale) {
        double r = 22 * scale;
        graphics.setFill(fill);
        graphics.fillOval(x - r, y - r, 2 * r, 2 * r);
        graphics.setStroke(border);
        graphics.setLineWidth(3);
        graphics.strokeOval(x - r, y - r, 2 * r, 2 * r);
    }

    // Hit-note flight: a hit note arcs back in the direction it came from, grows
    // slightly and fades out. The method skeleton (flight map, noteHit, timing) was
    // AI-assisted 2026-11-04; the arc/scale motion via Matrix3x3 is the student's work.
    private static final double FLIGHT_MS = 450;
    private final Map<Integer, double[]> hitFlights = new HashMap<>();   // id -> {hitMs, hitX}

    public void noteHit(Note note, double atMs) {
        if (note == null) return;
        double hitX = getWidth() * 0.18 + (note.timeMs() - atMs) * speed;
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
            double s = 1 + 0.3 * ease; // Size behavior on hit


            //Matrix Chain
            Matrix3x3 m = Matrix3x3.translate(x0-dx, y0-dy).multiply(Matrix3x3.scale(s,s)).multiply(Matrix3x3.translate(-x0,-y0));
            double[] p = m.transform(x0,y0);


            boolean don = note.noteType() == NoteType.DON;
            graphics.setGlobalAlpha(1-t*t*t);
            drawNote(graphics, p[0], p[1], don ? CENTRE : RIM, don ? CENTRE_BORDER : RIM_BORDER, s);
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
    }

    public void setStats(int score, int combo) {
        this.score = score;
        this.combo = combo;
    }
}
