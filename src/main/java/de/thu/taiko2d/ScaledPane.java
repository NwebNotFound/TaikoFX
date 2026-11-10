package de.thu.taiko2d;

import de.thu.taiko2d.render.ViewScale;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.transform.Scale;

/**
 * Lays a menu out at the design size and scales it to fill the window, so text, buttons and
 * cards grow when the window is maximised or full screen.
 * AI-generated (Claude Code, 2026-11-10); see docs/AI_USAGE.md.
 */
final class ScaledPane extends Pane {
    private final Region content;
    private final Scale scale = new Scale(1, 1, 0, 0);

    ScaledPane(Region content) {
        this.content = content;
        content.getTransforms().add(scale);
        getChildren().add(content);
        setMinSize(0, 0);
        setPickOnBounds(false);   // a hidden overlay must not swallow clicks
    }

    @Override
    protected void layoutChildren() {
        double s = ViewScale.fit(getWidth(), getHeight());
        scale.setX(s);
        scale.setY(s);
        content.resizeRelocate(0, 0, getWidth() / s, getHeight() / s);
    }
}
