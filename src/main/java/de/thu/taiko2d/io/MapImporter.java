package de.thu.taiko2d.io;

import de.thu.taiko2d.model.MapManifest;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * Copies a TaikoFX map into the user's map library (~/.taikofx/maps). Accepts a .tmap/.zip
 * package, a map folder, or that folder's manifest.json. AI-generated 2026-11-14; see
 * docs/AI_USAGE.md.
 *
 * Everything is treated as untrusted: only the allowlisted layout (manifest.json, LICENSES.txt,
 * README.md, audio/*.wav|mp3, charts/*.json, images/*.png) is extracted into a private staging
 * folder, sizes are counted while reading, and the map is only moved into the library after
 * the manifest, charts and audio have been checked. An existing map is never overwritten.
 */
public final class MapImporter {
    static final long MAX_ZIP_BYTES = 128L << 20;
    static final long MAX_TOTAL_BYTES = 128L << 20;
    static final int MAX_ENTRIES = 32;
    static final long MAX_AUDIO_BYTES = 110L << 20;
    static final long MAX_JSON_BYTES = 2L << 20;
    static final long MAX_IMAGE_BYTES = 4L << 20;
    static final long MAX_TEXT_BYTES = 64L << 10;
    static final int MAX_RATIO = 100;

    /** Thrown with a message that can be shown to the player as is. */
    public static final class ImportException extends IOException {
        public ImportException(String message) { super(message); }
    }

    private final Path library;

    public MapImporter(Path library) { this.library = library; }

    /** ~/.taikofx/maps */
    public static Path defaultLibrary() {
        return Path.of(System.getProperty("user.home"), ".taikofx", "maps");
    }

    public Path library() { return library; }

    /** True if the path looks like something {@link #importMap} can handle (used for drag-over). */
    public static boolean looksImportable(Path path) {
        String name = path.getFileName() == null ? "" : path.getFileName().toString().toLowerCase(Locale.ROOT);
        return Files.isDirectory(path) || name.endsWith(".tmap") || name.endsWith(".zip")
                || name.equals("manifest.json") || name.endsWith(".osz") || name.endsWith(".osu");
    }

    /** Imports one map and returns its new folder inside the library. */
    public Path importMap(Path source) throws IOException {
        String name = source.getFileName() == null ? "" : source.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.equals("manifest.json") && Files.isRegularFile(source)) source = source.getParent();
        if (!Files.exists(source)) throw new ImportException("File not found: " + source);
        if (isOsuMap(source, name)) return importOsu(source);

        Files.createDirectories(library);
        Path staging = library.resolve(".import-" + UUID.randomUUID());
        Files.createDirectory(staging);
        try {
            if (Files.isDirectory(source)) copyFolder(source, staging);
            else if (name.endsWith(".tmap") || name.endsWith(".zip")) extractZip(source, staging);
            else throw new ImportException(source.getFileName() + " is not a map. Drop a .tmap/.zip package or a map folder.");

            MapManifest manifest = validate(staging);
            Path target = library.resolve(folderName(manifest));
            if (Files.exists(target)) {
                throw new ImportException("\"" + manifest.title() + "\" is already imported (" + target.getFileName() + ").");
            }
            Files.move(staging, target);
            return target;
        } finally {
            deleteTree(staging);   // no-op after a successful move
        }
    }

    // ---- osu! maps (added 2026-11-14 at the student's request) -----------------------------

    /** .osz/.osu files, osu! song folders (.osu files, no manifest) and zips holding .osu files. */
    private static boolean isOsuMap(Path source, String name) throws IOException {
        if (name.endsWith(".osz") || name.endsWith(".osu")) return true;
        if (Files.isDirectory(source)) {
            if (Files.exists(source.resolve("manifest.json"))) return false;
            try (var files = Files.list(source)) {
                return files.anyMatch(f -> f.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".osu"));
            }
        }
        if (name.endsWith(".zip")) {
            try (ZipFile zip = new ZipFile(source.toFile())) {
                boolean manifest = zip.stream().anyMatch(e -> e.getName().endsWith("manifest.json"));
                return !manifest && zip.stream().anyMatch(e -> e.getName().toLowerCase(Locale.ROOT).endsWith(".osu"));
            } catch (ZipException e) {
                return false;   // the normal path reports the broken zip
            }
        }
        return false;
    }

    /** Converts with the osu! converter into a private temp folder, then imports that like any map folder. */
    private Path importOsu(Path source) throws IOException {
        if (Files.isRegularFile(source) && Files.size(source) > MAX_ZIP_BYTES) {
            throw new ImportException("osu! map is larger than 128 MiB.");
        }
        Files.createDirectories(library);
        Path temp = library.resolve(".convert-" + UUID.randomUUID());
        Files.createDirectory(temp);
        try {
            Path converted;
            try {
                converted = de.thu.taiko2d.tools.osu.ConvertOsu.convert(source.toAbsolutePath(), temp);
            } catch (ZipException e) {
                throw new ImportException("Not a valid .osz file: " + e.getMessage());
            } catch (IOException | RuntimeException e) {
                throw new ImportException("Could not convert " + source.getFileName() + ": " + e.getMessage());
            }
            return importMap(converted);
        } finally {
            deleteTree(temp);
        }
    }

    // ---- reading --------------------------------------------------------------------------

    private void extractZip(Path zipPath, Path staging) throws IOException {
        if (Files.size(zipPath) > MAX_ZIP_BYTES) throw new ImportException("Package is larger than 128 MiB.");
        try (ZipFile zip = new ZipFile(zipPath.toFile())) {
            List<? extends ZipEntry> entries = java.util.Collections.list(zip.entries());
            if (entries.size() > MAX_ENTRIES) throw new ImportException("Package has too many files (max " + MAX_ENTRIES + ").");
            String prefix = commonRootFolder(entries);
            Set<String> seen = new HashSet<>();
            long[] total = {0};
            for (ZipEntry entry : entries) {
                String raw = entry.getName();
                if (raw.contains("\\") || raw.startsWith("/") || raw.contains(":")) {
                    throw new ImportException("Unsafe file name in package: " + raw);
                }
                String rel = raw.substring(prefix.length());
                if (entry.isDirectory() || rel.isEmpty()) continue;
                if (!seen.add(rel.toLowerCase(Locale.ROOT))) throw new ImportException("Duplicate file in package: " + rel);
                long limit = limitFor(rel);
                if (limit < 0) throw new ImportException("Unexpected file in package: " + rel);
                Path out = safeResolve(staging, rel);
                Files.createDirectories(out.getParent());
                try (InputStream in = zip.getInputStream(entry)) {
                    long written = copyLimited(in, out, limit, total, rel);
                    long packed = Math.max(1, entry.getCompressedSize());
                    if (written > 1024 * 1024 && written / packed > MAX_RATIO) {
                        throw new ImportException("Suspicious compression ratio for " + rel);
                    }
                }
            }
        } catch (ZipException e) {
            throw new ImportException("Not a valid ZIP package: " + e.getMessage());
        }
    }

    /** "song/" when every entry sits in one enclosing folder (a zipped map folder), else "". */
    private static String commonRootFolder(List<? extends ZipEntry> entries) {
        boolean rootManifest = entries.stream().anyMatch(e -> e.getName().equals("manifest.json"));
        if (rootManifest || entries.isEmpty()) return "";
        String first = entries.getFirst().getName();
        int slash = first.indexOf('/');
        if (slash <= 0) return "";
        String prefix = first.substring(0, slash + 1);
        for (ZipEntry e : entries) if (!e.getName().startsWith(prefix)) return "";
        return prefix;
    }

    private void copyFolder(Path source, Path staging) throws IOException {
        List<Path> files = new ArrayList<>();
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (attrs.isRegularFile()) files.add(file);   // links are not followed
                return FileVisitResult.CONTINUE;
            }
        });
        long[] total = {0};
        int copied = 0;
        for (Path file : files) {
            String rel = source.relativize(file).toString().replace('\\', '/');
            long limit = limitFor(rel);
            if (limit < 0) continue;                  // folders may contain other stuff; only take map files
            if (++copied > MAX_ENTRIES) throw new ImportException("Map folder has too many files (max " + MAX_ENTRIES + ").");
            Path out = safeResolve(staging, rel);
            Files.createDirectories(out.getParent());
            try (InputStream in = Files.newInputStream(file, LinkOption.NOFOLLOW_LINKS)) {
                copyLimited(in, out, limit, total, rel);
            }
        }
    }

    /** Byte limit for an allowed path, or -1 if the path is not part of the map layout. */
    static long limitFor(String rel) {
        String lower = rel.toLowerCase(Locale.ROOT);
        if (rel.equals("manifest.json")) return MAX_JSON_BYTES;
        if (rel.equals("LICENSES.txt") || rel.equals("README.md")) return MAX_TEXT_BYTES;
        String[] parts = rel.split("/");
        if (parts.length != 2 || parts[1].isEmpty() || parts[1].startsWith(".")) return -1;
        if (!parts[1].matches("[A-Za-z0-9._ ()\\-]+")) return -1;
        return switch (parts[0]) {
            case "audio" -> lower.endsWith(".wav") || lower.endsWith(".mp3") ? MAX_AUDIO_BYTES : -1;
            case "charts" -> lower.endsWith(".json") ? MAX_JSON_BYTES : -1;
            case "images" -> lower.endsWith(".png") ? MAX_IMAGE_BYTES : -1;
            default -> -1;
        };
    }

    private static Path safeResolve(Path root, String rel) throws ImportException {
        Path out = root.resolve(rel).normalize();
        if (!out.startsWith(root) || out.equals(root)) throw new ImportException("Unsafe file name: " + rel);
        return out;
    }

    private static long copyLimited(InputStream in, Path out, long limit, long[] total, String rel) throws IOException {
        byte[] buffer = new byte[64 * 1024];
        long written = 0;
        try (OutputStream os = Files.newOutputStream(out)) {
            int n;
            while ((n = in.read(buffer)) > 0) {
                written += n;
                total[0] += n;
                if (written > limit) throw new ImportException(rel + " is too large.");
                if (total[0] > MAX_TOTAL_BYTES) throw new ImportException("Map is larger than 128 MiB.");
                os.write(buffer, 0, n);
            }
        }
        return written;
    }

    // ---- checking -------------------------------------------------------------------------

    private static MapManifest validate(Path folder) throws IOException {
        if (!Files.isRegularFile(folder.resolve("manifest.json"))) {
            throw new ImportException("No manifest.json found. A map needs manifest.json, charts/ and audio/.");
        }
        MapManifest m;
        try {
            m = BeatmapJson.loadManifest(folder);
        } catch (RuntimeException e) {
            throw new ImportException("manifest.json is not valid JSON: " + e.getMessage());
        }
        if (m == null) throw new ImportException("manifest.json is empty.");
        requireText(m.title(), "title");
        requireText(m.artist(), "artist");
        requireText(m.audio(), "audio");
        if (m.mapper() == null) throw new ImportException("manifest.json is missing \"mapper\".");
        if (m.audio().toLowerCase(Locale.ROOT).endsWith(".ogg")) {
            throw new ImportException("\"" + m.title() + "\" uses .ogg audio, which JavaFX cannot play. "
                    + "Convert the song to .mp3 or .wav first.");
        }
        if (limitFor(m.audio()) < 0 || !m.audio().startsWith("audio/")) {
            throw new ImportException("Audio must be a .wav or .mp3 file in audio/, not " + m.audio());
        }
        if (!Files.isRegularFile(folder.resolve(m.audio()))) throw new ImportException("Audio file is missing: " + m.audio());
        if (m.difficulties() == null || m.difficulties().isEmpty()) throw new ImportException("The map has no difficulties.");
        Set<String> charts = new HashSet<>();
        for (MapManifest.DifficultyRef d : m.difficulties()) {
            if (d == null) throw new ImportException("Empty difficulty entry in manifest.json.");
            requireText(d.name(), "difficulty name");
            requireText(d.chart(), "difficulty chart");
            if (!d.chart().startsWith("charts/") || limitFor(d.chart()) < 0) {
                throw new ImportException("Chart must be a .json file in charts/, not " + d.chart());
            }
            if (!charts.add(d.chart().toLowerCase(Locale.ROOT))) throw new ImportException("Chart used twice: " + d.chart());
            Path chart = folder.resolve(d.chart());
            if (!Files.isRegularFile(chart)) throw new ImportException("Chart file is missing: " + d.chart());
            try {
                if (BeatmapJson.loadNotes(chart).isEmpty()) throw new ImportException("Chart has no notes: " + d.chart());
                BeatmapJson.loadChartInfo(chart);
            } catch (RuntimeException e) {
                throw new ImportException("Chart " + d.chart() + " is broken: " + e.getMessage());
            }
        }
        return m;
    }

    private static void requireText(String value, String field) throws ImportException {
        if (value == null || value.isBlank()) throw new ImportException("manifest.json is missing \"" + field + "\".");
    }

    /** Library folder name from the map id (or title), reduced to a-z, 0-9 and dashes. */
    static String folderName(MapManifest m) {
        String base = m.id() != null && !m.id().isBlank() ? m.id() : m.artist() + "-" + m.title();
        String clean = base.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
        if (clean.length() > 60) clean = clean.substring(0, 60);
        return clean.isEmpty() ? "map-" + Integer.toHexString(base.hashCode()) : clean;
    }

    private static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root, LinkOption.NOFOLLOW_LINKS)) return;
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
