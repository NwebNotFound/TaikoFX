package de.thu.taiko2d.io;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.thu.taiko2d.model.MapManifest;
import de.thu.taiko2d.model.Note;
import de.thu.taiko2d.model.NoteType;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class BeatmapJsonTest {
    private static final Path MAP = Path.of("examples/heart-crush");

    @Test
    void loadsAllNotesFromChart() throws IOException {
        List<Note> notes = BeatmapJson.loadNotes(MAP.resolve("charts/test.json"));
        assertEquals(8, notes.size());
        assertEquals(1000, notes.get(0).timeMs());
        assertEquals(NoteType.DON, notes.get(0).noteType());
        assertEquals(NoteType.KA, notes.get(1).noteType());
    }

    @Test
    void loadsManifest() throws IOException {
        MapManifest manifest = BeatmapJson.loadManifest(MAP);
        assertEquals("HEART CRUSH!", manifest.title());
        assertEquals("audio/song.wav", manifest.audio());
        assertEquals(1000, manifest.previewStartMs());
        assertEquals("charts/test.json", manifest.difficulties().get(0).chart());
    }
}