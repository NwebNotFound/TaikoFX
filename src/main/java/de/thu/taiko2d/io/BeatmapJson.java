package de.thu.taiko2d.io;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.thu.taiko2d.model.MapManifest;
import de.thu.taiko2d.model.Note;
import de.thu.taiko2d.model.NoteType;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class BeatmapJson {
    private BeatmapJson() {}

    public static List<Note> loadNotes(Path chartFile) throws IOException {
        try (Reader reader = Files.newBufferedReader(chartFile)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            long offsetMs = root.get("offsetMs").getAsLong();
            List<Note> notes = new ArrayList<>();
            int id = 0;
            for (JsonElement element : root.getAsJsonArray("notes")) {
                JsonObject n = element.getAsJsonObject();
                NoteType type = n.get("type").getAsString().equals("don") ? NoteType.DON : NoteType.KA;
                notes.add(new Note(id++, n.get("timeMs").getAsLong() + offsetMs, type));
            }
            return notes;
        }
    }

    public static MapManifest loadManifest(Path mapFolder) throws IOException {
        try (Reader reader = Files.newBufferedReader(mapFolder.resolve("manifest.json"))) {
            return new Gson().fromJson(reader, MapManifest.class);
        }
    }
}
