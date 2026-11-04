package de.thu.taiko2d.audio;

import java.nio.file.Path;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

/** Plays a looping song-select preview starting at the map's previewStartMs. */
public final class SongPreview {
    private static final double LENGTH_MS = 15_000;
    private MediaPlayer player;

    public void play(Path audioFile, long previewStartMs) {
        stop();
        MediaPlayer created;
        try {
            created = new MediaPlayer(new Media(audioFile.toUri().toString()));
        } catch (RuntimeException e) {
            System.err.println("Preview unavailable: " + e.getMessage());
            return;
        }
        player = created;
        created.setVolume(0.6);
        created.setCycleCount(MediaPlayer.INDEFINITE);
        created.setOnError(() -> System.err.println("Preview error: " + created.getError()));
        created.setOnReady(() -> {
            double total = created.getTotalDuration().toMillis();
            double start = previewStartMs < total ? previewStartMs : 0;
            double end = Math.min(total, start + LENGTH_MS);
            created.setStartTime(Duration.millis(start));
            created.setStopTime(Duration.millis(end));
            if (player == created) created.play();
        });
    }

    public void stop() {
        if (player != null) {
            player.dispose();
            player = null;
        }
    }
}
