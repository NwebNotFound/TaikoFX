package de.thu.taiko2d.game;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class InputStateTest {
    @Test
    void repeatedPressIsIgnoredUntilRelease() {
        InputState<String> input = new InputState<>();
        assertTrue(input.press("F"));
        assertFalse(input.press("F"));     // key repeat
        input.release("F");
        assertTrue(input.press("F"));      // fresh press again
    }

    @Test
    void clearForgetsHeldKeys() {
        InputState<String> input = new InputState<>();
        input.press("F");
        input.clear();
        assertTrue(input.press("F"));
    }

    @Test
    void differentKeysAreIndependent() {
        InputState<String> input = new InputState<>();
        assertTrue(input.press("F"));
        assertTrue(input.press("J"));
    }
}