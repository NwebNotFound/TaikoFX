package de.thu.taiko2d;

import javafx.application.Application;

/** AI-assisted bootstrap, 2026-10-13. See docs/AI_USAGE.md. */
public final class Launcher {
    private Launcher() { }

    public static void main(String[] args) {
        Application.launch(TaikoApplication.class, args);
    }
}
