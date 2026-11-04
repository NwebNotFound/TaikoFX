package de.thu.taiko2d.game;

import java.util.HashSet;
import java.util.Set;

public final class InputState<K> {
    private final Set<K> down = new HashSet<>();

    public boolean press(K key) {
        return down.add(key);
    }

    public void release(K key) {
        down.remove(key);
    }

    public void clear() {
        down.clear();
    }
}