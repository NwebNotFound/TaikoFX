package de.thu.taiko2d.render;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/** Static, resizable lane preview. AI-assisted bootstrap; see docs/AI_USAGE.md. */
public final class GameCanvas extends Canvas {
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
        graphics.setFont(Font.font("Segoe UI", 12));
        graphics.fillText("HIT", targetX - 11, laneY + 78);
        drawNote(graphics, width * 0.55, laneY, CENTRE, CENTRE_BORDER);
        drawNote(graphics, width * 0.75, laneY, RIM, RIM_BORDER);
        graphics.setFill(TARGET);
        graphics.fillText("Static lane preview", 16, laneY - 72);
    }

    private void drawNote(GraphicsContext graphics, double x, double y, Color fill, Color border) {
        graphics.setFill(fill);
        graphics.fillOval(x - 22, y - 22, 44, 44);
        graphics.setStroke(border);
        graphics.setLineWidth(3);
        graphics.strokeOval(x - 22, y - 22, 44, 44);
    }
}
