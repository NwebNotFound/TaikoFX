package de.thu.taiko2d.game;

/** When the mascot celebrates a combo. AI-generated 2026-11-09; see docs/AI_USAGE.md. */
public final class ComboMilestone {
    public static final int STEP = 50;

    private ComboMilestone() {}

    /** 0 = nothing, 1 = a flip at 50x/150x/..., 2 = a double flip at every 100x. */
    public static int flipSpins(int previousCombo, int combo) {
        if (combo <= previousCombo || combo < STEP) return 0;
        if (combo / STEP == previousCombo / STEP) return 0;
        return (combo / STEP) % 2 == 0 ? 2 : 1;
    }
}
