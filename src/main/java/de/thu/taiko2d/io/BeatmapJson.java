package de.thu.taiko2d.io;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.thu.taiko2d.model.ChartInfo;
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

    /** BPM, grid start and optional scroll multiplier of a chart. AI-assisted 2026-11-09. */
    public static ChartInfo loadChartInfo(Path chartFile) throws IOException {
        try (Reader reader = Files.newBufferedReader(chartFile)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            double bpm = root.has("bpm") ? root.get("bpm").getAsDouble() : 120;
            long grid = root.has("gridStartMs") ? root.get("gridStartMs").getAsLong() : 0;
            long offset = root.has("offsetMs") ? root.get("offsetMs").getAsLong() : 0;
            double scroll = root.has("scrollMultiplier") ? root.get("scrollMultiplier").getAsDouble() : 1.0;
            List<ChartInfo.Section> kiai = new ArrayList<>();
            if (root.has("kiai")) {
                for (JsonElement element : root.getAsJsonArray("kiai")) {
                    JsonObject k = element.getAsJsonObject();
                    kiai.add(new ChartInfo.Section(k.get("startMs").getAsLong() + offset,
                                                   k.get("endMs").getAsLong() + offset));
                }
            }
            return new ChartInfo(bpm, grid + offset, scroll, kiai);
        }
    }

    public static MapManifest loadManifest(Path mapFolder) throws IOException {
        try (Reader reader = Files.newBufferedReader(mapFolder.resolve("manifest.json"))) {
            return new Gson().fromJson(reader, MapManifest.class);
        }
    }
}
