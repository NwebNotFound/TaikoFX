package de.thu.taiko2d.tools.osu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI-generated (Claude Code, 2026-11-09). See docs/AI_USAGE.md.
 *
 * Developer tool only, not part of the game: parses one osu! difficulty (.osu text)
 * into plain data for {@link ConvertOsu}. TaikoFX v1 has normal don/ka notes only.
 *
 * <ul>
 *   <li>osu!taiko (mode 1): hit circles become notes; whistle/clap means ka.
 *       Drumrolls (sliders), dendens (spinners) and big-note flags are dropped.</li>
 *   <li>osu!standard / osu!catch (mode 0 / 2): circles and slider starts become notes;
 *       spinners are dropped. Ka comes from whistle/clap hitsounds; if the map uses
 *       too few of those to give a mix, notes on the right half of the playfield
 *       become ka instead.</li>
 *   <li>osu!mania (mode 3) is not supported.</li>
 * </ul>
 */
public record OsuChart(Map<String, String> general, Map<String, String> metadata,
        double bpm, long gridStartMs, List<OsuNote> notes, int skippedObjects, List<KiaiSection> kiai) {

    public record OsuNote(long timeMs, boolean ka) {}

    /** A kiai ("highlight") section from the timing points, start inclusive, end exclusive. */
    public record KiaiSection(long startMs, long endMs) {}

    private static final int EFFECT_KIAI = 1;

    private static final int TYPE_CIRCLE = 1;
    private static final int TYPE_SLIDER = 2;
    private static final int SOUND_WHISTLE = 2;
    private static final int SOUND_CLAP = 8;
    private static final int PLAYFIELD_CENTRE_X = 256;
    /** Below this share of hitsound kas, a standard map uses position-based colours. */
    static final double MIN_HITSOUND_KA_SHARE = 0.15;

    public String general(String key) { return general.getOrDefault(key, ""); }
    public String metadata(String key) { return metadata.getOrDefault(key, ""); }

    public int mode() { return parseMode(general); }

    /**
     * The map's own scroll speed relative to the osu! default slider multiplier 1.4,
     * clamped to a playable range. Per-section speed changes (green lines) are ignored.
     */
    public double scrollMultiplier() {
        try {
            double sliderMultiplier = Double.parseDouble(general("SliderMultiplier"));
            return Math.max(0.6, Math.min(2.0, sliderMultiplier / 1.4));
        } catch (NumberFormatException e) {
            return 1.0;
        }
    }

    public boolean isTaiko() { return mode() == 1; }

    public boolean isSupportedMode() { return mode() >= 0 && mode() <= 2; }

    public static OsuChart parse(String text) {
        if (!text.isEmpty() && text.charAt(0) == '﻿') text = text.substring(1);
        Map<String, String> general = new HashMap<>();
        Map<String, String> metadata = new HashMap<>();
        List<long[]> objects = new ArrayList<>(); // time, x, hitsound-ka (0/1)
        double beatLength = Double.NaN;
        long gridStart = 0;
        int skipped = 0;
        String section = "";
        List<KiaiSection> kiai = new ArrayList<>();
        long kiaiStart = -1;   // -1 = not in kiai

        for (String raw : text.split("\r?\n")) {
            String line = raw.strip();
            if (line.isEmpty() || line.startsWith("//")) continue;
            if (line.startsWith("[") && line.endsWith("]")) {
                section = line.substring(1, line.length() - 1);
                continue;
            }
            switch (section) {
                case "General", "Difficulty" -> putKeyValue(general, line);   // both are key: value
                case "Metadata" -> putKeyValue(metadata, line);
                case "TimingPoints" -> {
                    String[] p = line.split(",");
                    // Kiai flag (effects bit 0) on red and green lines; a change starts or ends a section.
                    if (p.length >= 8) {
                        long time = Math.round(Double.parseDouble(p[0].strip()));
                        boolean on = (Integer.parseInt(p[7].strip()) & EFFECT_KIAI) != 0;
                        if (on && kiaiStart < 0) kiaiStart = time;
                        if (!on && kiaiStart >= 0) {
                            if (time > kiaiStart) kiai.add(new KiaiSection(kiaiStart, time));
                            kiaiStart = -1;
                        }
                    }
                    // First uninherited (red) timing point: positive beat length.
                    if (Double.isNaN(beatLength) && p.length >= 2) {
                        double length = Double.parseDouble(p[1].strip());
                        boolean uninherited = p.length < 7 || p[6].strip().equals("1");
                        if (uninherited && length > 0) {
                            beatLength = length;
                            gridStart = Math.round(Double.parseDouble(p[0].strip()));
                        }
                    }
                }
                case "HitObjects" -> {
                    String[] p = line.split(",");
                    if (p.length < 5) { skipped++; continue; }
                    int type = Integer.parseInt(p[3].strip());
                    // In taiko a slider is a drumroll; in standard/catch its start is a hit.
                    boolean slidersCount = parseMode(general) != 1;
                    boolean isNote = (type & TYPE_CIRCLE) != 0 || (slidersCount && (type & TYPE_SLIDER) != 0);
                    if (!isNote) { skipped++; continue; }
                    int sound = Integer.parseInt(p[4].strip());
                    objects.add(new long[] {
                        Long.parseLong(p[2].strip()),
                        Math.round(Double.parseDouble(p[0].strip())),
                        (sound & (SOUND_WHISTLE | SOUND_CLAP)) != 0 ? 1 : 0});
                }
                default -> { }
            }
        }

        boolean useHitsounds = parseMode(general) == 1 || hitsoundKaShare(objects) >= MIN_HITSOUND_KA_SHARE;
        List<OsuNote> notes = new ArrayList<>(objects.size());
        for (long[] o : objects) {
            boolean ka = useHitsounds ? o[2] == 1 : o[1] >= PLAYFIELD_CENTRE_X;
            notes.add(new OsuNote(o[0], ka));
        }
        if (kiaiStart >= 0) {   // kiai still on at the end of the map: lasts until after the last note
            long end = objects.stream().mapToLong(o -> o[0]).max().orElse(kiaiStart) + 1000;
            kiai.add(new KiaiSection(kiaiStart, Math.max(end, kiaiStart + 1)));
        }
        double bpm = Double.isNaN(beatLength) ? 120 : 60_000.0 / beatLength;
        return new OsuChart(general, metadata, bpm, gridStart, List.copyOf(notes), skipped, List.copyOf(kiai));
    }

    private static double hitsoundKaShare(List<long[]> objects) {
        if (objects.isEmpty()) return 0;
        return objects.stream().filter(o -> o[2] == 1).count() / (double) objects.size();
    }

    private static int parseMode(Map<String, String> general) {
        try { return Integer.parseInt(general.getOrDefault("Mode", "0").strip()); }
        catch (NumberFormatException e) { return 0; }
    }

    private static void putKeyValue(Map<String, String> target, String line) {
        int colon = line.indexOf(':');
        if (colon > 0) target.put(line.substring(0, colon).strip(), line.substring(colon + 1).strip());
    }
}
