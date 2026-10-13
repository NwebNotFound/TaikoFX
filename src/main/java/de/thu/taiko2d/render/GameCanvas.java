package de.thu.taiko2d.render;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/** Static, resizable lane preview. AI-assisted bootstrap; see docs/AI_USAGE.md. */
public final class GameCanvas extends Canvas {
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
        graphics.setFill(Color.web("#202938"));
        graphics.fillRoundRect(0, laneY - 52, width, 104, 18, 18);
        graphics.setStroke(Color.web("#38455a"));
        graphics.setLineWidth(1);
        graphics.strokeLine(0, laneY, width, laneY);
        graphics.setStroke(Color.web("#f5ce80"));
        graphics.setLineWidth(3);
        graphics.strokeOval(targetX - 33, laneY - 33, 66, 66);
        graphics.setFill(Color.web("#f5ce80"));
        graphics.setFont(Font.font("Segoe UI", 12));
        graphics.fillText("HIT", targetX - 11, laneY + 78);
        graphics.setFill(Color.web("#ef766f"));
        graphics.fillOval(width * 0.55 - 22, laneY - 22, 44, 44);
        graphics.setFill(Color.web("#68b9da"));
        graphics.fillOval(width * 0.75 - 22, laneY - 22, 44, 44);
        graphics.setFill(Color.web("#b3bdce"));
        graphics.fillText("Static lane preview", 16, laneY - 72);
    }
}
