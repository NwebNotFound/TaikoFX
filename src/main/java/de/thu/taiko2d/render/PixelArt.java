package de.thu.taiko2d.render;

import java.io.InputStream;
import java.util.Map;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

/**
 * AI-generated 2026-11-09; see docs/AI_USAGE.md.
 *
 * Builds placeholder pixel-art sprites from text rows, one character per pixel.
 * Every placeholder can be replaced by the student's own Aseprite PNG: if
 * {@code sprites/<name>.png} exists in the resources, that file wins.
 */
final class PixelArt {
    /** Shared festival palette. '.' is always transparent. */
    static final Color INK = Color.web("#1a1423");
    static final Color CREAM = Color.web("#f6e7c8");
    static final Color RED = Color.web("#e8433a");
    static final Color DARK_RED = Color.web("#a4262c");
    static final Color GOLD = Color.web("#f7b733");
    static final Color WOOD = Color.web("#7a4b2a");
    static final Color BLUE = Color.web("#3fa7e8");
    static final Color PINK = Color.web("#f59bb0");
    static final Color SKIN = Color.web("#f2c9a0");

    private PixelArt() {}

    static Image fromRows(Map<Character, Color> palette, String... rows) {
        int width = rows[0].length();
        WritableImage image = new WritableImage(width, rows.length);
        PixelWriter writer = image.getPixelWriter();
        for (int y = 0; y < rows.length; y++) {
            if (rows[y].length() != width) {
                throw new IllegalArgumentException("Row " + y + " has length " + rows[y].length() + ", expected " + width);
            }
            for (int x = 0; x < width; x++) {
                char c = rows[y].charAt(x);
                if (c == '.') continue;
                Color color = palette.get(c);
                if (color == null) throw new IllegalArgumentException("No colour for '" + c + "'");
                writer.setColor(x, y, color);
            }
        }
        return image;
    }

    /** The student's PNG if it exists, else the placeholder. */
    static Image override(String name, Image placeholder) {
        InputStream in = PixelArt.class.getResourceAsStream("/de/thu/taiko2d/sprites/" + name + ".png");
        return in == null ? placeholder : new Image(in);
    }
}
