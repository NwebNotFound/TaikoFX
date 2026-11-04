package de.thu.taiko2d.model;

import java.util.List;

public record MapManifest(String id, String title, String artist, String mapper, String audio, long previewStartMs, List<DifficultyRef> difficulties) {
    public record DifficultyRef(String id, String name, int level, String chart) {}
}
