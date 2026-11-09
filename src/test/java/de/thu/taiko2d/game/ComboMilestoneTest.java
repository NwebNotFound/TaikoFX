package de.thu.taiko2d.game;

// AI-generated (Claude Code, 2026-11-09). See docs/AI_USAGE.md.

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ComboMilestoneTest {
    @Test
    void flipsOnceAtFiftyAndTwiceAtHundreds() {
        assertEquals(0, ComboMilestone.flipSpins(48, 49));
        assertEquals(1, ComboMilestone.flipSpins(49, 50));
        assertEquals(0, ComboMilestone.flipSpins(50, 51));
        assertEquals(2, ComboMilestone.flipSpins(99, 100));
        assertEquals(1, ComboMilestone.flipSpins(149, 150));
        assertEquals(2, ComboMilestone.flipSpins(199, 200));
    }

    @Test
    void comboBreakOrJumpOverMilestoneIsHandled() {
        assertEquals(0, ComboMilestone.flipSpins(60, 0));     // combo broke
        assertEquals(1, ComboMilestone.flipSpins(48, 52));    // two hits in one frame still flips
    }
}
