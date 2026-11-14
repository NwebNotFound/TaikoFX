package de.thu.taiko2d.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** AI-generated 2026-11-14 with MapImporter; see docs/AI_USAGE.md. */
class MapImporterTest {
    private static final String MANIFEST = """
            {"formatVersion":1,"id":"Test Song!","title":"Test","artist":"Me","mapper":"Me",
             "audio":"audio/song.wav","previewStartMs":0,
             "difficulties":[{"id":"easy","name":"Easy","level":1,"chart":"charts/easy.json"}]}""";
    private static final String CHART = """
            {"bpm":120,"offsetMs":0,"notes":[{"timeMs":1000,"type":"don"},{"timeMs":1500,"type":"ka"}]}""";

    @TempDir Path temp;

    private Map<String, String> validMap() {
        Map<String, String> files = new LinkedHashMap<>();
        files.put("manifest.json", MANIFEST);
        files.put("charts/easy.json", CHART);
        files.put("audio/song.wav", "RIFF fake audio");
        files.put("LICENSES.txt", "test");
        return files;
    }

    private Path zip(String name, Map<String, String> files) throws IOException {
        Path zip = temp.resolve(name);
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(zip))) {
            for (var e : files.entrySet()) {
                out.putNextEntry(new ZipEntry(e.getKey()));
                out.write(e.getValue().getBytes(StandardCharsets.UTF_8));
                out.closeEntry();
            }
        }
        return zip;
    }

    private Path folder(Map<String, String> files) throws IOException {
        Path dir = temp.resolve("src-folder");
        for (var e : files.entrySet()) {
            Path f = dir.resolve(e.getKey());
            Files.createDirectories(f.getParent());
            Files.writeString(f, e.getValue());
        }
        return dir;
    }

    private MapImporter importer() { return new MapImporter(temp.resolve("library")); }

    private long libraryEntries() throws IOException {
        try (Stream<Path> s = Files.list(temp.resolve("library"))) { return s.count(); }
    }

    @Test
    void importsTmapIntoLibraryFolderNamedAfterId() throws IOException {
        Path imported = importer().importMap(zip("song.tmap", validMap()));
        assertEquals("test-song", imported.getFileName().toString());
        assertEquals("Test", BeatmapJson.loadManifest(imported).title());
        assertTrue(Files.exists(imported.resolve("audio/song.wav")));
        assertEquals(1, libraryEntries());          // staging was cleaned up
    }

    @Test
    void acceptsZipWithOneEnclosingFolder() throws IOException {
        Map<String, String> nested = new LinkedHashMap<>();
        validMap().forEach((k, v) -> nested.put("my-map/" + k, v));
        Path imported = importer().importMap(zip("song.zip", nested));
        assertTrue(Files.exists(imported.resolve("charts/easy.json")));
    }

    @Test
    void importsFolderAndSkipsUnrelatedFiles() throws IOException {
        Map<String, String> files = validMap();
        files.put("notes-for-me.docx", "junk");
        Path imported = importer().importMap(folder(files).resolve("manifest.json"));
        assertFalse(Files.exists(imported.resolve("notes-for-me.docx")));
        assertTrue(Files.exists(imported.resolve("manifest.json")));
    }

    @Test
    void refusesSecondImportOfSameMap() throws IOException {
        importer().importMap(zip("a.tmap", validMap()));
        var e = assertThrows(MapImporter.ImportException.class, () -> importer().importMap(zip("b.tmap", validMap())));
        assertTrue(e.getMessage().contains("already imported"));
        assertEquals(1, libraryEntries());
    }

    @Test
    void rejectsPathTraversal() throws IOException {
        Map<String, String> files = validMap();
        files.put("../evil.txt", "x");
        assertThrows(MapImporter.ImportException.class, () -> importer().importMap(zip("bad.tmap", files)));
        assertFalse(Files.exists(temp.resolve("evil.txt")));
        assertEquals(0, libraryEntries());
    }

    @Test
    void rejectsUnexpectedFilesAndCaseCollisions() throws IOException {
        Map<String, String> exe = validMap();
        exe.put("audio/run.exe", "x");
        assertThrows(MapImporter.ImportException.class, () -> importer().importMap(zip("exe.tmap", exe)));

        Map<String, String> dup = validMap();
        dup.put("charts/EASY.json", CHART);
        assertThrows(MapImporter.ImportException.class, () -> importer().importMap(zip("dup.tmap", dup)));
    }

    @Test
    void rejectsMissingAudioAndBrokenChart() throws IOException {
        Map<String, String> noAudio = validMap();
        noAudio.remove("audio/song.wav");
        var e = assertThrows(MapImporter.ImportException.class, () -> importer().importMap(zip("na.tmap", noAudio)));
        assertTrue(e.getMessage().contains("Audio"));

        Map<String, String> broken = validMap();
        broken.put("charts/easy.json", "{ not json");
        assertThrows(MapImporter.ImportException.class, () -> importer().importMap(zip("br.tmap", broken)));
        assertEquals(0, libraryEntries());
    }

    @Test
    void rejectsOversizedEntryWhileReading() throws IOException {
        Path zip = temp.resolve("big.tmap");
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(zip))) {
            out.putNextEntry(new ZipEntry("manifest.json"));
            OutputStream o = out;
            byte[] spaces = new byte[1 << 20];
            java.util.Arrays.fill(spaces, (byte) ' ');
            for (int i = 0; i < 3; i++) o.write(spaces);   // 3 MiB > 2 MiB JSON limit
            out.closeEntry();
        }
        assertThrows(MapImporter.ImportException.class, () -> importer().importMap(zip));
    }

    private static String osu(String audio) {
        return String.join("\r\n", "osu file format v14", "", "[General]", "AudioFilename: " + audio, "Mode: 1", "",
                "[Metadata]", "Title:Osu Song", "Artist:Someone", "Creator:mapper", "Version:Oni", "",
                "[TimingPoints]", "0,500,4,2,0,100,1,0", "", "[HitObjects]",
                "256,192,1000,1,0,0:0:0:0:", "256,192,1500,1,2,0:0:0:0:", "");
    }

    @Test
    void convertsAndImportsOsz() throws IOException {
        Map<String, String> files = new LinkedHashMap<>();
        files.put("a.osu", osu("song.mp3"));
        files.put("song.mp3", "fake mp3");
        files.put("bg.jpg", "background the game does not use");
        Path imported = importer().importMap(zip("set.osz", files));
        assertEquals("osu-someone-osu-song", imported.getFileName().toString());
        assertEquals(2, BeatmapJson.loadNotes(imported.resolve(
                BeatmapJson.loadManifest(imported).difficulties().getFirst().chart())).size());
        assertEquals(1, libraryEntries());          // conversion temp folder was cleaned up
    }

    @Test
    void importsOsuSongFolder() throws IOException {
        Map<String, String> files = new LinkedHashMap<>();
        files.put("x [Oni].osu", osu("audio.mp3"));
        files.put("audio.mp3", "fake mp3");
        Path imported = importer().importMap(folder(files));
        assertTrue(Files.exists(imported.resolve("audio/song.mp3")));
    }

    @Test
    void refusesOggOsuMapWithExplanation() throws IOException {
        Map<String, String> files = new LinkedHashMap<>();
        files.put("a.osu", osu("song.ogg"));
        files.put("song.ogg", "fake ogg");
        var e = assertThrows(MapImporter.ImportException.class, () -> importer().importMap(zip("ogg.osz", files)));
        assertTrue(e.getMessage().contains(".ogg"));
        assertEquals(0, libraryEntries());
    }
}
