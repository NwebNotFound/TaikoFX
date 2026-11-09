package de.thu.taiko2d.tools.osu;

// AI-generated (Claude Code, 2026-11-09). See docs/AI_USAGE.md.

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OsuChartTest {
    private static String osu(int mode, String version, String hitObjects) {
        String nl = "\r\n";
        return "﻿osu file format v14" + nl + nl + "[General]" + nl + "AudioFilename: song.mp3" + nl
                + "PreviewTime: 1234" + nl + "Mode: " + mode + nl + nl
                + "[Difficulty]" + nl + "SliderMultiplier:2.1" + nl + nl
                + "[Metadata]" + nl + "Title:Test Song" + nl + "Artist:Some Artist" + nl + "Creator:mapper" + nl
                + "Version:" + version + nl + nl
                + "[TimingPoints]" + nl + "500,500,4,2,0,100,1,0" + nl + "1000,-100,4,2,0,100,0,0" + nl + nl
                + "[HitObjects]" + nl
                + hitObjects;
    }

    @Test
    void taikoUsesHitsoundsAndDropsDrumrollsAndSpinners() {
        OsuChart chart = OsuChart.parse(osu(1, "Oni", """
                256,192,1000,1,0,0:0:0:0:
                256,192,1500,1,2,0:0:0:0:
                256,192,2000,1,8,0:0:0:0:
                256,192,2500,1,4,0:0:0:0:
                256,192,3000,2,0,L|300:192,1,100
                256,192,4000,12,0,5000,0:0:0:0:
                """));
        assertEquals(1, chart.mode());
        assertEquals(120.0, chart.bpm(), 1e-9);
        assertEquals(500, chart.gridStartMs());
        assertEquals(List.of(new OsuChart.OsuNote(1000, false), new OsuChart.OsuNote(1500, true),
                new OsuChart.OsuNote(2000, true), new OsuChart.OsuNote(2500, false)), chart.notes());
        assertEquals(2, chart.skippedObjects());
    }

    @Test
    void standardKeepsSliderStartsAndUsesPositionWhenHitsoundsAreSparse() {
        OsuChart chart = OsuChart.parse(osu(0, "Insane", """
                100,192,1000,1,0,0:0:0:0:
                400,192,1250,5,0,0:0:0:0:
                50,100,1500,2,0,L|300:192,1,100
                300,100,2000,6,0,L|300:192,1,100
                256,192,3000,12,0,4000,0:0:0:0:
                """));
        assertEquals(List.of(new OsuChart.OsuNote(1000, false), new OsuChart.OsuNote(1250, true),
                new OsuChart.OsuNote(1500, false), new OsuChart.OsuNote(2000, true)), chart.notes());
        assertEquals(1, chart.skippedObjects());
    }

    @Test
    void standardUsesHitsoundsWhenTheyGiveAMix() {
        OsuChart chart = OsuChart.parse(osu(0, "Hard", """
                400,192,1000,1,0,0:0:0:0:
                400,192,1500,1,2,0:0:0:0:
                100,192,2000,1,8,0:0:0:0:
                """));
        assertEquals(List.of(new OsuChart.OsuNote(1000, false), new OsuChart.OsuNote(1500, true),
                new OsuChart.OsuNote(2000, true)), chart.notes());
    }

    @Test
    void maniaIsNotSupported() {
        assertFalse(OsuChart.parse(osu(3, "4K", "64,192,1000,1,0,0:0:0:0:\n")).isSupportedMode());
        assertTrue(OsuChart.parse(osu(2, "Salad", "64,192,1000,1,0,0:0:0:0:\n")).isSupportedMode());
    }

    @Test
    void kiaiSectionsComeFromTimingPointEffects() {
        String text = String.join("\n",
                "[General]", "Mode: 1", "[TimingPoints]",
                "0,500,4,2,0,100,1,0",
                "2000,-100,4,2,0,100,0,1",      // kiai on (green line)
                "4000,-100,4,2,0,100,0,1",      // still on, no new section
                "6000,-100,4,2,0,100,0,0",      // off
                "8000,500,4,2,0,100,1,1",       // on again until the end
                "[HitObjects]", "256,192,9000,1,0,0:0:0:0:");
        assertEquals(List.of(new OsuChart.KiaiSection(2000, 6000), new OsuChart.KiaiSection(8000, 10000)),
                OsuChart.parse(text).kiai());
    }

    @Test
    void convertsOszIntoMapFolderTheGameCanRead(@TempDir Path dir) throws Exception {
        Path osz = dir.resolve("set.osz");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(osz))) {
            zip.putNextEntry(new ZipEntry("a.osu"));
            zip.write(osu(1, "Oni", "256,192,1000,1,0,0:0:0:0:\n256,192,1500,1,2,0:0:0:0:\n256,192,2000,1,0,0:0:0:0:\n").getBytes());
            zip.putNextEntry(new ZipEntry("b.osu"));
            zip.write(osu(0, "Easy", "100,192,1000,1,0,0:0:0:0:\n").getBytes());
            zip.putNextEntry(new ZipEntry("song.mp3"));
            zip.write(new byte[] {1, 2, 3});
        }

        Path folder = ConvertOsu.convert(osz, dir.resolve("out"));

        assertEquals("osu-some-artist-test-song", folder.getFileName().toString());
        assertEquals(3, Files.size(folder.resolve("audio/song.mp3")));
        JsonObject manifest = new Gson().fromJson(Files.readString(folder.resolve("manifest.json")), JsonObject.class);
        assertEquals(1234, manifest.get("previewStartMs").getAsLong());
        var diffs = manifest.getAsJsonArray("difficulties");
        assertEquals(2, diffs.size());
        assertEquals("Easy (std)", diffs.get(0).getAsJsonObject().get("name").getAsString());
        assertEquals("Oni", diffs.get(1).getAsJsonObject().get("name").getAsString());
        JsonObject oni = new Gson().fromJson(
                Files.readString(folder.resolve(diffs.get(1).getAsJsonObject().get("chart").getAsString())), JsonObject.class);
        assertEquals(3, oni.getAsJsonArray("notes").size());
        assertEquals(1.5, oni.get("scrollMultiplier").getAsDouble(), 1e-9);
        assertEquals("ka", oni.getAsJsonArray("notes").get(1).getAsJsonObject().get("type").getAsString());
        assertTrue(Files.exists(folder.resolve("LICENSES.txt")));
    }
}
