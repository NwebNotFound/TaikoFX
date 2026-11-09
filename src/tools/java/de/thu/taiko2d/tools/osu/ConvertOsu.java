package de.thu.taiko2d.tools.osu;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * AI-generated (Claude Code, 2026-11-09). See docs/AI_USAGE.md.
 *
 * Developer tool only, not part of the game or its distribution. Converts an
 * osu! .osz (or a folder/.osu file) into a TaikoFX map folder under
 * examples/osu-&lt;name&gt;/ for local testing. Those folders are git-ignored because
 * downloaded maps and songs are third-party material without redistribution rights.
 *
 * Usage: gradlew convertOsu --args="path\to\map.osz [outputParentDir]"
 */
public final class ConvertOsu {
    private static final long MAX_OSU_BYTES = 5L << 20;
    private static final long MAX_AUDIO_BYTES = 100L << 20;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private ConvertOsu() {}

    public static void main(String[] args) throws IOException {
        if (args.length < 1 || args.length > 2) {
            System.err.println("Usage: gradlew convertOsu --args=\"<map.osz|map.osu|folder> [outputParentDir]\"");
            System.exit(2);
        }
        Path input = Path.of(args[0]).toAbsolutePath();
        Path outParent = Path.of(args.length > 1 ? args[1] : "examples");
        Path out = convert(input, outParent);
        System.out.println("Wrote " + out.toAbsolutePath());
    }

    /** Source of .osu texts and the audio file, from a ZIP or a plain folder. */
    private interface Source extends AutoCloseable {
        List<String> osuTexts() throws IOException;
        void copyAudio(String name, Path target) throws IOException;
        @Override void close() throws IOException;
    }

    public static Path convert(Path input, Path outParent) throws IOException {
        try (Source source = open(input)) {
            List<OsuChart> charts = new ArrayList<>();
            for (String text : source.osuTexts()) {
                OsuChart chart = OsuChart.parse(text);
                if (!chart.isSupportedMode()) {
                    System.out.println("Skipping '" + chart.metadata("Version") + "': osu!mania is not supported");
                } else if (chart.notes().isEmpty()) {
                    System.out.println("Skipping '" + chart.metadata("Version") + "': no notes");
                } else {
                    charts.add(chart);
                }
            }
            if (charts.isEmpty()) throw new IOException("No convertible difficulties found in " + input);
            charts.sort(Comparator.comparingInt((OsuChart c) -> c.notes().size()));

            OsuChart first = charts.get(0);
            String title = first.metadata("Title");
            String artist = first.metadata("Artist");
            String slug = "osu-" + slug(artist + "-" + title);
            Path folder = outParent.resolve(slug);
            Files.createDirectories(folder.resolve("charts"));
            Files.createDirectories(folder.resolve("audio"));

            String audioName = Path.of(first.general("AudioFilename")).getFileName().toString();
            String audioRel = "audio/song" + extension(audioName);
            source.copyAudio(audioName, folder.resolve(audioRel));
            if (audioRel.endsWith(".ogg")) {
                System.out.println("Warning: JavaFX cannot play .ogg audio; convert it to .mp3 or .wav.");
            }

            List<Map<String, Object>> difficulties = new ArrayList<>();
            for (int i = 0; i < charts.size(); i++) {
                OsuChart chart = charts.get(i);
                String version = chart.metadata("Version").isBlank() ? "Diff " + (i + 1) : chart.metadata("Version");
                if (!chart.isTaiko()) version += chart.mode() == 2 ? " (catch)" : " (std)";
                String id = uniqueId(slug(version), difficulties);
                String chartRel = "charts/" + id + ".json";
                Files.writeString(folder.resolve(chartRel), GSON.toJson(chartJson(id, chart)), StandardCharsets.UTF_8);
                Map<String, Object> ref = new LinkedHashMap<>();
                ref.put("id", id);
                ref.put("name", version);
                ref.put("level", i + 1);
                ref.put("chart", chartRel);
                difficulties.add(ref);
                System.out.printf(Locale.ROOT, "  %-32s %5d notes (%d unsupported objects dropped)%n",
                        version, chart.notes().size(), chart.skippedObjects());
            }

            Map<String, Object> manifest = new LinkedHashMap<>();
            manifest.put("formatVersion", 1);
            manifest.put("id", slug);
            manifest.put("title", title);
            manifest.put("artist", artist);
            manifest.put("mapper", first.metadata("Creator") + " (converted from osu!)");
            manifest.put("audio", audioRel);
            manifest.put("previewStartMs", Math.max(0, parseLong(first.general("PreviewTime"), 0)));
            manifest.put("licenseFile", "LICENSES.txt");
            manifest.put("difficulties", difficulties);
            Files.writeString(folder.resolve("manifest.json"), GSON.toJson(manifest), StandardCharsets.UTF_8);
            Files.writeString(folder.resolve("LICENSES.txt"), """
                    Converted from a third-party osu! beatmap for private local testing only.
                    Title: %s
                    Artist: %s
                    Mapper: %s
                    osu! beatmap set ID: %s
                    The song and chart belong to their creators. Do not commit, publish or
                    submit this folder unless redistribution rights are confirmed.
                    """.formatted(title, artist, first.metadata("Creator"), first.metadata("BeatmapSetID")),
                    StandardCharsets.UTF_8);
            return folder;
        }
    }

    static Map<String, Object> chartJson(String difficultyId, OsuChart chart) {
        List<Map<String, Object>> notes = new ArrayList<>();
        int n = 1;
        for (OsuChart.OsuNote note : chart.notes()) {
            Map<String, Object> json = new LinkedHashMap<>();
            json.put("id", "n" + n++);
            json.put("timeMs", note.timeMs());
            json.put("type", note.ka() ? "ka" : "don");
            notes.add(json);
        }
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("formatVersion", 1);
        root.put("difficultyId", difficultyId);
        root.put("bpm", Math.round(chart.bpm() * 1000) / 1000.0);
        root.put("gridStartMs", chart.gridStartMs());
        root.put("offsetMs", 0);
        root.put("scrollMultiplier", Math.round(chart.scrollMultiplier() * 100) / 100.0);
        List<Map<String, Object>> kiai = new ArrayList<>();
        for (OsuChart.KiaiSection section : chart.kiai()) {
            Map<String, Object> json = new LinkedHashMap<>();
            json.put("startMs", section.startMs());
            json.put("endMs", section.endMs());
            kiai.add(json);
        }
        root.put("kiai", kiai);
        root.put("notes", notes);
        return root;
    }

    private static Source open(Path input) throws IOException {
        if (Files.isDirectory(input)) return folderSource(input);
        String name = input.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".osu")) return folderSource(input.getParent(), input);
        if (name.endsWith(".osz") || name.endsWith(".zip")) return zipSource(new ZipFile(input.toFile()));
        throw new IOException("Expected a .osz, .osu or folder: " + input);
    }

    private static Source folderSource(Path folder, Path... only) throws IOException {
        return new Source() {
            public List<String> osuTexts() throws IOException {
                List<Path> files;
                if (only.length > 0) {
                    files = List.of(only);
                } else {
                    try (var stream = Files.list(folder)) {
                        files = stream.filter(p -> p.toString().toLowerCase(Locale.ROOT).endsWith(".osu")).sorted().toList();
                    }
                }
                List<String> texts = new ArrayList<>();
                for (Path f : files) texts.add(Files.readString(f, StandardCharsets.UTF_8));
                return texts;
            }
            public void copyAudio(String name, Path target) throws IOException {
                Path audio = folder.resolve(name).normalize();
                if (!audio.startsWith(folder.normalize()) || !Files.isRegularFile(audio)) {
                    throw new IOException("Audio file not found next to the map: " + name);
                }
                Files.copy(audio, target, StandardCopyOption.REPLACE_EXISTING);
            }
            public void close() {}
        };
    }

    private static Source zipSource(ZipFile zip) {
        return new Source() {
            public List<String> osuTexts() throws IOException {
                List<String> texts = new ArrayList<>();
                for (ZipEntry entry : zip.stream().sorted(Comparator.comparing(ZipEntry::getName)).toList()) {
                    if (entry.isDirectory() || !entry.getName().toLowerCase(Locale.ROOT).endsWith(".osu")) continue;
                    try (InputStream in = zip.getInputStream(entry)) {
                        texts.add(new String(in.readNBytes((int) MAX_OSU_BYTES + 1), StandardCharsets.UTF_8));
                    }
                    if (texts.getLast().length() > MAX_OSU_BYTES) throw new IOException("Oversized .osu entry: " + entry.getName());
                }
                return texts;
            }
            public void copyAudio(String name, Path target) throws IOException {
                // Match by file name only; ZIP paths are never used as output paths.
                ZipEntry entry = zip.stream()
                        .filter(e -> !e.isDirectory() && Path.of(e.getName()).getFileName().toString().equalsIgnoreCase(name))
                        .findFirst().orElseThrow(() -> new IOException("Audio file not in archive: " + name));
                try (InputStream in = zip.getInputStream(entry)) {
                    byte[] data = in.readNBytes((int) MAX_AUDIO_BYTES + 1);
                    if (data.length > MAX_AUDIO_BYTES) throw new IOException("Audio file is larger than 100 MB");
                    Files.write(target, data);
                }
            }
            public void close() throws IOException { zip.close(); }
        };
    }

    static String slug(String text) {
        String s = text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
        if (s.length() > 48) s = s.substring(0, 48).replaceAll("-+$", "");
        return s.isEmpty() ? "map" : s;
    }

    private static String uniqueId(String base, List<Map<String, Object>> existing) {
        String id = base;
        for (int i = 2; containsId(existing, id); i++) id = base + "-" + i;
        return id;
    }

    private static boolean containsId(List<Map<String, Object>> existing, String id) {
        return existing.stream().anyMatch(d -> id.equals(d.get("id")));
    }

    private static String extension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot).toLowerCase(Locale.ROOT);
    }

    private static long parseLong(String text, long fallback) {
        try { return Long.parseLong(text.strip()); } catch (NumberFormatException e) { return fallback; }
    }
}
