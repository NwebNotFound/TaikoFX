# Taiko2D: project specification and Codex setup guide

Solo THU Computer Graphics 2D assignment, winter semester 2026. Prepared 13 October 2026. Planning target: mid-November 2026; confirm the exact deadline and upload dates in Moodle.

This document is a specification and a set of prompts for your Codex agent. It does not install anything or create the game. Start with the bootstrap prompt in section 12, then implement the game in small stages.

## 1. Project idea and confirmed scope

Build a small Taiko-inspired rhythm game in JavaFX with an integrated beatmap editor. Notes travel horizontally toward a hit marker; the player hits the appropriate key in time with the music. The editor creates the same maps the game plays.

Your confirmed choices:

- Work solo and aim to finish by mid-November 2026.
- Use JavaFX to keep menus, drawing, file dialogs, and editor controls manageable.
- Start with normal notes only. Proposed controls: F/J for centre notes and D/K for rim notes. These are configurable design defaults, not course requirements.
- Use your own JSON beatmaps inside `.tmap` ZIP packages.
- Do not implement osu! map importing.
- A Space-bar hold note is the first optional extension after the MVP works.

The retrieved earlier chat contains the requirements PDF and your JavaFX/file-format questions, but not the complete original idea or readable earlier assistant answers. This specification uses the available material and your clarifications in this chat. Other design choices below are recommendations.

## 2. What the assignment actually requires

Source: your supplied `2D-Projects-Requirements-WS2026.pdf`, page 1, “Requirements for 2D Projects in Computer Graphics / Winter Semester 2026 / Prof. Franz.”

| PDF requirement | Project action |
| --- | --- |
| Register the project name in advance in Moodle | Register the chosen name yourself before the required date. |
| Implement and present a custom 2D game or scene in Java or C++/OpenGL | JavaFX implements the Java option; deliver playable 2D graphics. |
| Mention sources and mark your contributions | Maintain `SOURCES.md`, `CONTRIBUTIONS.md`, and AI attribution. |
| Open a topic in the Moodle 2D-project forum | Include a screenshot, description, own contributions, and source attachment or repository link. |
| Runnable binaries are appreciated | Supply a tested Windows distribution if feasible. |
| Give a 2½-minute pitch | Demonstrate gameplay, graphics, and the editor concisely. |
| Record a silent screencast smaller than 50 MB | Show visible hit feedback and editor actions; audio is unnecessary in the submitted video. |
| Upload presentation material in advance | Upload the video; a PDF slide is optional. Confirm the upload dates. |
| Give assigned-partner feedback | Address graphics, improvements, compilation/running, and source-code remarks. |
| Mark AI-generated code and follow the separate Moodle AI notes | Read those notes before accepting generated implementation; use their required marking format. |
| Have appropriate rights for open-source publication | Check both code and all bundled media before publishing. |

The PDF includes a sentence referring to a “3D project” within the 2D requirements. This appears inconsistent with the surrounding text; confirm any ambiguity with the lecturer. The PDF does not specify the deadline, require a beatmap editor, or require JavaFX specifically. It does not establish the detailed AI policy: that separate Moodle document remains to be checked.

For a graphics assignment, make your visible work clear: draw the lane, animated notes, target, timed feedback, score/combo, and an editable timeline. Explain coordinate transforms, frame-independent movement, clipping, and visual states in your pitch. Do not spend the whole project on file handling.

## 3. Recommended development stack

Use ordinary Java SE, not Android Java, Java EE, or a web framework. Use one build system: Gradle.

| Component | Recommended baseline | Purpose and verification |
| --- | --- | --- |
| JDK | Eclipse Temurin **25 LTS**, Windows x64, current 25.x security patch | Free OpenJDK distribution. Your existing **Oracle OpenJDK 25** also works. The Adoptium release listing checked for this guide reports 25.0.4.1+1; check the installer offered when setting up. [Temurin releases](https://adoptium.net/temurin/releases/?arch=any&mode=filter&version=25). |
| JavaFX | **25.0.4** | Pin this compatible release line. Its published minimum is JDK 23, so JDK 25 is suitable. JavaFX 27 is also listed, but a major upgrade adds no essential feature for this assignment. [Gluon roadmap](https://gluonhq.com/products/javafx/). |
| Gradle | Wrapper **9.1.0** as a conservative initial baseline | Official Java 25 support starts here. This is a reproducibility choice, not a claim that it is the newest Gradle. A newer stable version is acceptable after verification. [Release notes](https://docs.gradle.org/9.1.0/release-notes.html). |
| JavaFX Gradle plugin | `org.openjfx.javafxplugin` **0.1.0** | Resolves JavaFX dependencies and platform libraries. [Plugin portal](https://plugins.gradle.org/plugin/org.openjfx.javafxplugin). |
| JSON | Gson **2.14.0** | Read/write plain JSON; explicitly use strict parsing and validate the model. [Release](https://github.com/google/gson/releases/tag/gson-parent-2.14.0). |
| Tests | JUnit Jupiter **6.0.3** via BOM | Verified stable baseline compatible with JDK 25; newer releases exist. [Release notes](https://docs.junit.org/6.0.3/release-notes.html). |
| IDE | Stable IntelliJ IDEA with Java and Gradle support | Use the free Java features. Set both project SDK and Gradle JVM to JDK 25. [Gradle setup](https://www.jetbrains.com/help/idea/gradle.html). |

Versions above were checked against official project/distributor pages on 13 October 2026 where feasible. They have **not** been resolved together or compiled in this task. Bootstrap must verify artifact availability and the complete build; never silently substitute another major version. Online “current” documentation can move ahead of this pinned baseline.

Required JavaFX modules: `javafx.controls` and `javafx.media`; controls brings in graphics/base. Use `Canvas` for gameplay and the timeline, standard controls for menus and forms, and CSS for UI styling. Skip FXML initially; it adds another file/controller layer without helping the MVP.

Use JDK `java.util.zip` for packages, `java.nio.file` for files, and Java Sound only if the timing spike shows that the simpler JavaFX audio approach is insufficient. No database, server, login, networking, Spring, LWJGL, or game engine is needed. JSON Schema can document the format without adding a runtime schema library: a small explicit validator is sufficient initially.

Maven is a valid alternative if you already know it: use the same JDK and dependencies, a Maven wrapper, and the official JavaFX Maven plugin. Do not maintain both Maven and Gradle builds. This guide and its commands use Gradle. [Official JavaFX getting started](https://openjfx.io/openjfx-docs/).

## 4. What to choose in IntelliJ's New Project window

For the screen you shared:

1. Select **Java** in the left sidebar.
2. Name the project **Taiko2D**.
3. Select **Gradle**, rather than the currently selected IntelliJ build system.
4. If a Gradle DSL choice appears, select **Groovy**. Keep the programming language Java.
5. Choose **JDK 25**. The selected `openjdk-25 / Oracle OpenJDK 25` is usable. For a new download, select Eclipse Temurin 25.
6. Enable **Create Git repository** unless the destination already belongs to a repository.
7. Leave **Add sample code** unchecked if available. If IntelliJ generates sample code anyway, let Codex inspect it before replacing it.
8. Click **Create**. JavaFX can be added afterward through Gradle; a special JavaFX project generator is unnecessary.

Prefer a local development folder such as `C:\Users\nw05\Documents\Taiko2D`. Your displayed iCloud folder can work, but actively syncing build/cache files can complicate development. If you use it, keep `.gradle/` and `build/` out of version control and ensure the project is locally available. Git ignores do not disable iCloud syncing.

Open the resulting folder in Codex and provide this Markdown file. Do not ask Codex to create another project if IntelliJ already created one.

## 5. MVP and optional features

### MVP: the complete first deliverable

- Main menu, local map selection, difficulty selection, and controls/help.
- One horizontal lane, clearly distinguishable centre/rim normal notes, and a hit marker.
- F/J centre, D/K rim; key-press edges only, with key-repeat suppression.
- Audio playback at rate 1.0, chart offset, local calibration offset, and a timing diagnostic.
- Perfect/Good/Miss feedback, score, combo, and a result screen.
- Restart and exit. For the first version, pause freezes the run but resuming restarts the attempt; state this clearly in the UI.
- Integrated editor: choose a WAV, edit title/artist/mapper, set constant BPM and offset, add/remove/move normal notes, snap, zoom/scroll, save/open, export/import `.tmap`, and test the current chart without losing edits.
- At least one short, rights-cleared demo track/chart, ideally 60–90 seconds, with two difficulties sharing the same audio.
- A tested source build, readable README, attributions, screenshot, and presentation materials.

### Stretch goals, in order

1. Space-bar hold notes, including editor authoring and judging.
2. Undo/redo, waveform overview, local high scores, remappable controls.
3. Smooth pause/resume, short practice sections, additional visual polish.
4. Large notes or drum rolls only if core timing, saving, and packaging remain reliable.

No osu! importing, online ranking, multiplayer, automatic chart generation, or custom binary format is planned. Cut optional features before reducing editor reliability or the ability to build/run the submission.

### Suggested scoring rules

Use global rules rather than difficulty-controlled judgement windows:

| Absolute timing error | Judgement | Score | Combo |
| --- | --- | --- | --- |
| Up to and including 45 ms | Perfect | 300 | +1 |
| Over 45 ms, up to and including 100 ms | Good | 100 | +1 |
| Past 100 ms without a valid hit | Miss | 0 | Reset |

These windows are proposed starting values, not Taiko's official rules. An input outside a window or of the wrong type gives visible feedback but does not consume the pending note. Ignore unmatched extra presses for score initially. For each press, choose the nearest unresolved note of the matching type inside the window; break ties by earlier time, then ID. One edge consumes at most one note. Mark a note missed only once.

Accuracy = `100 * (300 * perfect + 100 * good) / (300 * totalNotes)`. Reject empty playable charts to avoid division by zero. Display counts and maximum combo; skip health/failure mechanics for the MVP.

## 6. Folder and code architecture

Start with one Gradle project and a non-modular application. Avoid a large framework or one class containing the game, editor, audio, and files.

```text
Taiko2D/
  settings.gradle
  build.gradle
  gradlew
  gradlew.bat
  gradle/wrapper/                 # Commit wrapper JAR and properties
  .gitignore
  README.md
  AGENTS.md
  docs/
    PROJECT_SPEC.md               # Copy of this guide
    SOURCES.md
    CONTRIBUTIONS.md
    AI_USAGE.md
    TEST_REPORT.md
    schemas/
      manifest.schema.json
      difficulty.schema.json
  src/main/java/de/thu/taiko2d/
    Launcher.java                 # Ordinary main(), calls Application.launch
    TaikoApplication.java         # JavaFX Application; composition/root lifecycle
    model/                        # Plain records/classes; no JavaFX imports
      MapManifest.java
      Difficulty.java
      Note.java
      NoteType.java
    game/
      GameSession.java            # Run state, resolved notes, results
      JudgementEngine.java        # Deterministic matching and scoring
      InputState.java             # Press/release edges and focus handling
    audio/
      AudioTransport.java         # Load/play/pause/seek/position/dispose contract
      JavaFxAudioTransport.java
      SongClock.java              # Media-position sampling/interpolation
    render/
      GameCanvas.java
      TimelineCanvas.java
    editor/
      EditorDocument.java         # Chart + dirty state, separate from a game run
      EditorController.java       # Selection, snapping, editing, test-play
    io/
      BeatmapJson.java
      BeatmapValidator.java
      TmapPackageIO.java          # Bounded ZIP import/export
    ui/
      ScreenRouter.java
      MainMenuView.java
      MapSelectView.java
      EditorView.java
      ResultsView.java
  src/main/resources/
    css/app.css
    demo/                         # Only small, rights-cleared demo assets
  src/test/java/de/thu/taiko2d/
    game/
    io/
    editor/
  src/test/resources/maps/        # Small valid and deliberately invalid fixtures
```

Keep imported maps and settings outside `src/`, the application installation, and `build/`. Suggested runtime location: `%LOCALAPPDATA%\Taiko2D\` with `maps/`, `settings.json`, and disposable import staging. Never hardcode your personal absolute path into Java code.

Dependencies flow from UI/controllers to the plain model and services. The renderer reads a session snapshot; it does not decide judgements. The game and editor share timing conversion, validation, rendering helpers, and audio transport. Test-play uses an immutable snapshot so scoring cannot modify the editor's chart.

Run drawing and UI updates on the JavaFX application thread. Do ZIP/JSON/audio-file loading on a worker and send only completed results back. Dispose old players, stop timers on screen changes, and close workers on exit. Do not block the UI thread on file I/O.

## 7. Timing and audio synchronization

### Time definitions

Store note positions as integer **milliseconds from the beginning of the audio file**. Keep nanoseconds only for monotonic interpolation and convert deliberately.

```text
scheduledNoteMs = note.timeMs + difficulty.offsetMs
errorMs = inputAudioPositionMs - localInputOffsetMs - scheduledNoteMs
```

Positive chart offset moves every note later in the music. Positive local input offset compensates a measured late press by subtracting that delay. Example: a note at 1000 ms, chart offset +20 ms, press measured at 1050 ms, and local input offset +30 ms gives error 0 ms. Local calibration belongs in settings, never in an exported chart.

For display, optionally use `visualAudioMs = songClockMs + localVisualOffsetMs`; positive visual offset advances the visible lane. It must not change judging. Start with visual offset zero and expose it only if necessary.

```text
noteX = hitX + (scheduledNoteMs - visualAudioMs) * pixelsPerMs
```

Future notes appear to the right of the target and move left. Calculate this position every frame from time; do not subtract a fixed number of pixels per frame. Use `AnimationTimer` for painting, not as the master song clock. Clip the lane and render only notes in its visible time range.

### Recommended first audio approach

Use JavaFX `MediaPlayer` for music and short preloaded `AudioClip`s for optional feedback. For demo content prefer **PCM WAV, 16-bit signed, 44.1 kHz, mono or stereo**. WAV is larger but avoids compressed-audio encoder delay and simplifies testing. MP3 support can be added after device testing; OGG support must not be assumed. [JavaFX supported media](https://openjfx.io/javadoc/25/javafx.media/javafx/scene/media/package-summary.html).

Load validated audio from a local path and create its URI with `Path.toUri()`. A file inside a ZIP must first be safely staged/extracted. Wait for readiness; handle load errors, stalls, and end-of-media. Playback and seeking are asynchronous. [MediaPlayer API](https://openjfx.io/javadoc/25/javafx.media/javafx/scene/media/MediaPlayer.html).

`getCurrentTime()` is a media-position estimate, not a guaranteed sample-accurate clock. First test it directly with a click track. If updates visibly step, implement `SongClock`:

1. On each fresh media-position sample, record its position and `System.nanoTime()`.
2. While playback is actually advancing at rate 1.0, interpolate from that anchor for smooth rendering/input timestamps.
3. Bound extrapolation to a short interval, initially 50 ms, and regularly re-anchor to fresh media samples. Do not treat every repeated unchanged value as a new sample.
4. Freeze or invalidate the attempt on a stall; never keep judging against a clock running ahead of silent audio. After a discontinuity, cancel/restart the scored attempt rather than issuing artificial misses.
5. Reset anchors after seeking, stopping, replaying, or changing track. During an editor seek, suspend test-play judgement until the new position is confirmed.

Timestamp key edges when the JavaFX event handler receives them, using the same clock mapping as rendering. This includes event-queue delay and is not a hardware key timestamp. Keep UI work short and use wired audio during calibration where practical. A fixed offset can correct stable delay; it cannot correct jitter.

Handle key release to clear the pressed-key set, ignore repeated press events, and clear it on focus loss. Losing focus pauses/cancels the run. Do not allow held keys to generate hits when restarting.

### Early timing spike and fallback

Before building the full editor, play a rights-cleared 60-second click track. Place notes exactly on clicks and inspect alignment at the start and end. Measure reported position versus monotonic time, frame gaps, and signed hit errors. Repeat at several window sizes and under modest load. Proposed target: no accumulating drift above roughly 20 ms over one minute and stable enough input for the chosen windows. This is a project acceptance target, not a JavaFX guarantee.

If JavaFX timing is unusable on the target machine, keep JavaFX for UI/drawing and replace only `AudioTransport` with a **Java Sound `SourceDataLine` PCM implementation**. A dedicated worker writes audio; derive position from frames actually processed, not bytes submitted. Track the line frame baseline and seek base. On seek, stop/flush and restart from the chosen sample; use whole frames. The line position still does not guarantee when sound physically reaches the listener, so calibrate device latency and test the mixer. [SourceDataLine](https://docs.oracle.com/en/java/javase/25/docs/api/java.desktop/javax/sound/sampled/SourceDataLine.html), [frame-position semantics](https://docs.oracle.com/en/java/javase/26/docs/api/java.desktop/javax/sound/sampled/DataLine.html).

Do not implement both audio backends before the spike proves a need. Avoid sleeping threads to schedule every note; scoring is based on the current audio position and timestamped inputs.

## 8. Editor behavior

The editor is part of the MVP, but should remain small:

1. Create a document and choose a supported local WAV.
2. Set metadata, one constant BPM, grid start, and chart offset.
3. Show a time ruler, beat grid, normal-note track, playhead, and a small note list for exact numeric editing.
4. Place notes with the current centre/rim tool; select, drag, change type, and delete them.
5. Toggle snapping, choose whole/half/quarter-beat divisions, zoom, and scroll.
6. Preview music, seek, test-play a snapshot, and return to unchanged editing state.
7. Save a local working map folder; export a portable `.tmap`; reopen it and verify the chart matches.
8. Warn about unsaved changes before closing/opening another map. Save via a temporary sibling file and atomic move where supported; preserve the previous file if validation or writing fails.

Use one editor transform in both directions:

```text
audioMs = viewportStartAudioMs + (mouseX - timelineLeftX) / pixelsPerMs
storedNoteMs = audioMs - difficulty.offsetMs
beatDurationMs = 60000.0 / bpm
snapStepMs = beatDurationMs / subdivisionsPerBeat
snappedMs = gridStartMs + round((storedNoteMs - gridStartMs) / snapStepMs) * snapStepMs
```

Round to integer milliseconds only when storing. Draw the beat grid at `gridStartMs + offsetMs + n * snapStepMs` so the editor and game agree. Reject negative stored positions and notes scheduled outside the audio. Test coordinate inversion and snapping with a nonzero offset.

MVP BPM is constant. BPM controls editor grid spacing; stored note timestamps remain authoritative for gameplay. Do not introduce variable BPM or store competing beat and millisecond positions in version 1. Empty charts may exist while editing but cannot be exported as playable maps.

## 9. Beatmap format: JSON plus `.tmap`

`.tmap` is your project's ZIP extension, not a Taiko/osu! standard. Define it simply enough that users can inspect it with a text editor and ZIP tool.

### Working folder and portable package

Use the same relative structure in the editor's working folder and ZIP root:

```text
example.tmap                    # ZIP file, with these root entries:
  manifest.json
  audio/song.wav
  charts/easy.json
  charts/normal.json
  images/cover.png               # Optional; omit field if absent
  LICENSES.txt
```

Do not put an enclosing `example/` directory inside the ZIP. One package contains one song and one or more difficulty files. Schema definitions belong in the source repository, not every package. Keep account data, personal absolute paths, caches, executables, and scripts out of packages.

### Manifest example (`manifest.json`)

The names and credits below describe an illustrative self-created demo; replace them with true attribution before distribution. The referenced audio and chart files must actually exist.

```json
{
  "formatVersion": 1,
  "id": "demo-click-track",
  "title": "Demo Click Track",
  "artist": "Project author",
  "mapper": "Project author",
  "audio": "audio/song.wav",
  "previewStartMs": 1000,
  "licenseFile": "LICENSES.txt",
  "difficulties": [
    {"id": "easy", "name": "Easy", "level": 2, "chart": "charts/easy.json"},
    {"id": "normal", "name": "Normal", "level": 4, "chart": "charts/normal.json"}
  ]
}
```

`level` is a manually assigned integer 1–10, not an automatic star calculation. Add optional `cover: "images/cover.png"` if supplied. Do not trust a stored audio duration: obtain it from the decoder and validate chart bounds after loading.

### Difficulty example (`charts/easy.json`)

```json
{
  "formatVersion": 1,
  "difficultyId": "easy",
  "bpm": 120,
  "gridStartMs": 0,
  "offsetMs": 0,
  "notes": [
    {"id": "n1", "timeMs": 1000, "type": "don"},
    {"id": "n2", "timeMs": 1500, "type": "ka"},
    {"id": "n3", "timeMs": 2000, "type": "don"},
    {"id": "n4", "timeMs": 2500, "type": "ka"}
  ]
}
```

`don` = centre note; `ka` = rim note. A normal chart uses the same structure with `difficultyId: "normal"` and its own note list. Note IDs stay stable during editing; sorted order is `(timeMs, id)`. Use Java `long` for timestamps, not floating-point values or string durations.

### JSON Schema contracts

Save the following blocks as the files shown in their headings. They use JSON Schema draft 2020-12. Gson does not enforce JSON Schema automatically; implement equivalent explicit validation and semantic checks below.

#### `docs/schemas/manifest.schema.json`

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "title": "Taiko2D manifest v1",
  "type": "object",
  "additionalProperties": false,
  "required": ["formatVersion", "id", "title", "artist", "mapper", "audio", "licenseFile", "difficulties"],
  "properties": {
    "formatVersion": {"const": 1},
    "id": {"$ref": "#/$defs/id"},
    "title": {"$ref": "#/$defs/text"},
    "artist": {"$ref": "#/$defs/text"},
    "mapper": {"$ref": "#/$defs/text"},
    "audio": {"type": "string", "pattern": "^audio/[a-z0-9_-]+\\.wav$"},
    "cover": {"type": "string", "pattern": "^images/[a-z0-9_-]+\\.png$"},
    "previewStartMs": {"type": "integer", "minimum": 0, "maximum": 600000},
    "licenseFile": {"const": "LICENSES.txt"},
    "difficulties": {
      "type": "array", "minItems": 1, "maxItems": 10,
      "items": {
        "type": "object", "additionalProperties": false,
        "required": ["id", "name", "level", "chart"],
        "properties": {
          "id": {"$ref": "#/$defs/id"},
          "name": {"$ref": "#/$defs/text"},
          "level": {"type": "integer", "minimum": 1, "maximum": 10},
          "chart": {"type": "string", "pattern": "^charts/[a-z0-9_-]+\\.json$"}
        }
      }
    }
  },
  "$defs": {
    "id": {"type": "string", "minLength": 1, "maxLength": 64, "pattern": "^[a-z0-9][a-z0-9_-]*$"},
    "text": {"type": "string", "minLength": 1, "maxLength": 200}
  }
}
```

#### `docs/schemas/difficulty.schema.json`

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "title": "Taiko2D difficulty v1",
  "type": "object",
  "additionalProperties": false,
  "required": ["formatVersion", "difficultyId", "bpm", "gridStartMs", "offsetMs", "notes"],
  "properties": {
    "formatVersion": {"const": 1},
    "difficultyId": {"type": "string", "minLength": 1, "maxLength": 64, "pattern": "^[a-z0-9][a-z0-9_-]*$"},
    "bpm": {"type": "number", "minimum": 20, "maximum": 400},
    "gridStartMs": {"type": "integer", "minimum": 0, "maximum": 600000},
    "offsetMs": {"type": "integer", "minimum": -5000, "maximum": 5000},
    "notes": {
      "type": "array", "minItems": 1, "maxItems": 20000,
      "items": {
        "type": "object", "additionalProperties": false,
        "required": ["id", "timeMs", "type"],
        "properties": {
          "id": {"type": "string", "minLength": 1, "maxLength": 64, "pattern": "^[a-z0-9][a-z0-9_-]*$"},
          "timeMs": {"type": "integer", "minimum": 0, "maximum": 600000},
          "type": {"enum": ["don", "ka"]}
        }
      }
    }
  }
}
```

### Semantic validation beyond the schemas

- Strict UTF-8 JSON only. Reject comments, trailing garbage, duplicate object keys, unknown fields, unknown versions, non-finite numbers, fractional integer fields, null required values, and wrong types. Gson DTO defaults must never turn missing required fields into valid zeros. Detect duplicate keys while streaming before conversion to records.
- Enforce nonblank trimmed display text; render it as text, never markup or a command.
- Manifest difficulty IDs and chart paths must each be unique. Every chart's `difficultyId` must match its manifest entry.
- All referenced files must exist exactly once, with the permitted formats and paths. No remote URL, absolute path, or fallback search outside the package.
- Note IDs must be unique, order canonical, and simultaneous notes disallowed in v1. Reject any two normal notes at the same scheduled time, even of different types. Extremely close notes can produce an editor warning rather than silently changing the chart.
- Validate `timeMs + offsetMs` against decoded audio duration. Require scheduled notes to be at least 100 ms after audio start and at least 100 ms before its end so full judgement windows are available. Require preview start and shifted grid start to fall inside audio duration.
- Limit audio to 10 minutes. Keep application import limits and schema bounds aligned.
- Export normal-note maps as version 1. Never write optional hold fields into v1; versioning must reflect changed semantics.

## 10. `.tmap` ZIP security and import/export

Treat all imported maps as untrusted data. These are proposed application limits, not course rules.

| Resource | Initial limit |
| --- | --- |
| ZIP file size | 128 MiB |
| All uncompressed entries combined | 128 MiB |
| Entry count, including directories | 32 |
| WAV file | 110 MiB; decoded duration ≤10 minutes |
| Each JSON file | 2 MiB |
| Cover PNG | 4 MiB; dimensions ≤2048×2048 and ≤4,194,304 pixels |
| License text | 64 KiB |
| JSON nesting | 16 levels |
| Notes per chart | 20,000 |

1. Inspect the ZIP before publishing any imported map. Allow only the fixed manifest/license names and expected `audio/*.wav`, `charts/*.json`, and optional `images/*.png` layout. Reject nested archives, executables, links, encrypted entries, and unsupported compression methods.
2. Use strict portable names matching the schemas. Reject `..`, `.`, backslashes, leading slashes, drive letters, colons/Windows alternate streams, UNC paths, NULs, reserved Windows device stems such as `con` or `nul`, trailing dots/spaces, and excessive path lengths. Check every directory component too.
3. Reject duplicate ZIP names and collisions after slash normalization and case folding with `Locale.ROOT`, even if they differ in capitalization. Never decode percent escapes in entry names.
4. Extract into a new private staging directory. Resolve/normalize each destination and require it to remain under that root; never follow symbolic links. A safe-looking extension alone does not make extraction safe. Prefer an allowlist extractor that creates only directories and regular files, never links from ZIP metadata.
5. Enforce byte totals while actually reading every entry; do not trust declared sizes. Bound compression ratio, initially 100:1, and enforce a cancellation/time budget for import. Enforce limits on the small manifest before parsing it.
6. Validate WAV headers/encoding and image dimensions before costly decoding. Reject unsupported formats cleanly. A file named `.wav` or `.png` may contain something else.
7. Validate JSON structure and cross-file semantics. Reject unreferenced data entries, except permitted explicit directory entries and the mandatory license text. Use no Java object deserialization, reflection class names, scripts, or plugins supplied by maps.
8. Once fully valid, move staging into an app-managed map folder. If the map ID already exists, report a conflict or import with a new local instance ID; do not silently overwrite another map. Clean up failed staging without touching other folders.
9. Export to a temporary ZIP, reread it through the same importer/validator, then complete the save. Confirm replacement of an existing user-selected target through the editor's normal save flow. Avoid partial archives and preserve originals on failure.

Use streaming reads and explicit counters. Security tests must cover traversal, case collisions, duplicate names, malformed JSON, excessive expansion, missing media, and mismatched difficulty IDs. A checksum may detect corruption but does not make a package trusted.

## 11. Optional Space-bar hold note

Implement only after the normal-note MVP passes its tests and editor round-trip checks. A hold is a separate visual track below the main lane so Space does not conflict with centre/rim notes.

Suggested rules:

- Press Space within the head's normal judgement window.
- Keep Space down until the tail. An early release more than 100 ms before the tail fails the hold.
- Release within ±100 ms of the tail for completion; remaining held beyond tail +100 ms fails completion.
- A held Space before the head is not a new press and cannot trigger it. Focus loss cancels the attempt safely.
- Score one note after the tail. Head and tail each receive Perfect/Good windows; the worse judgement is the final judgement. A failed head, broken sustain, or missing tail yields one Miss and one combo reset. Do not score per rendered frame.
- No overlapping holds and no new hold head until the previous tail +200 ms. Centre/rim notes may occur during holds.

Use **formatVersion 2** for both manifest and difficulty when holds are introduced. Support loading v1 without rewriting its meaning; an MVP-only loader must reject v2 clearly.

```json
{"id": "h1", "timeMs": 4000, "type": "hold", "endTimeMs": 6000}
```

For the v2 schema, use `oneOf` to distinguish normal notes (no `endTimeMs`) and holds (required integer `endTimeMs`). Validate `endTimeMs > timeMs`, minimum duration 200 ms, shifted head/tail audio bounds, and overlap rules in code. Offset applies to both endpoints.

Add a hold tool to the editor: drag from head to tail, snap both, resize endpoints, and edit exact times. During gameplay Space is hold-only. In the editor use a Play button or Ctrl+Space so authored holds do not collide with preview shortcuts.

## 12. Windows bootstrap: prompt for Codex

First create/open the IntelliJ project described in section 4. Copy this file to `docs/PROJECT_SPEC.md`, or attach it to Codex. Give Codex the project folder and the following prompt. This deliberately sets up the project quickly without implementing every game system for you.

```text
Set up this existing Java project for my solo THU Computer Graphics 2D assignment.
Read docs/PROJECT_SPEC.md (or the attached specification) and inspect the existing
project before editing. Preserve my files and existing work. Do not create a
second nested project. Use Java SE 25, Gradle Groovy DSL, and JavaFX.

Bootstrap only:
1. Confirm Windows architecture, installed JDKs, project SDK and Gradle JVM.
   Use my OpenJDK 25 if it is installed; Temurin 25 is also suitable.
2. Configure a Gradle wrapper with a verified JDK-25-compatible stable version;
   baseline 9.1.0. Commit all wrapper files and verify its distribution checksum.
   Do not assume gradlew exists before generating/restoring the wrapper.
3. Pin JavaFX 25.0.4, OpenJFX plugin 0.1.0, Gson 2.14.0,
   and JUnit Jupiter 6.0.3. Verify resolution; report any unavailable pin.
4. Add only the minimal JavaFX launcher and window: a title, a Canvas displaying
   a lane and target, and placeholder buttons for Play, Editor, and Exit.
   No gameplay, full editor, or hold implementation yet.
5. Use a non-modular single Gradle project and package de.thu.taiko2d.
   Create the small package structure from the specification without dozens of
   empty classes. Add CSS, .gitignore, and concise Windows README instructions.
6. Create docs/SOURCES.md, CONTRIBUTIONS.md and AI_USAGE.md. Record the code you
   generated as AI-assisted; do not label it as my independent contribution.
   Flag the separate Moodle AI policy as still needing review.
7. Add one meaningful headless smoke test for the initial pure-Java model only
   if a model is created; do not create meaningless tests to claim coverage.
8. Run the build, launch the actual JavaFX window if the environment allows it,
   and report what passed and what remains unverified. Distinguish automated
   tests from GUI/audio checks. Stop when bootstrap is ready so I can implement.

No osu! imports, no network features, no assets without clear rights,
no publication, and no Moodle submissions. Normal notes are the MVP;
Space-bar holds are optional later. Keep changes small and explain each file
I need to work in next.
```

### Suggested initial Gradle files

`settings.gradle`:

```groovy
rootProject.name = 'Taiko2D'
```

`build.gradle`:

```groovy
plugins {
    id 'java'
    id 'application'
    id 'org.openjfx.javafxplugin' version '0.1.0'
}

group = 'de.thu'
version = '0.1.0'

repositories { mavenCentral() }

java {
    toolchain { languageVersion = JavaLanguageVersion.of(25) }
}

javafx {
    version = '25.0.4'
    modules = ['javafx.controls', 'javafx.media']
}

dependencies {
    implementation 'com.google.code.gson:gson:2.14.0'
    testImplementation platform('org.junit:junit-bom:6.0.3')
    testImplementation 'org.junit.jupiter:junit-jupiter'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

application {
    mainClass = 'de.thu.taiko2d.Launcher'
    applicationDefaultJvmArgs = ['--enable-native-access=ALL-UNNAMED']
}

tasks.withType(JavaCompile).configureEach {
    options.encoding = 'UTF-8'
}

tasks.named('test') { useJUnitPlatform() }

// JavaFX's development run may place JavaFX on the module path.
tasks.named('run', JavaExec) {
    jvmArgs '--enable-native-access=ALL-UNNAMED,javafx.graphics,javafx.media'
}
```

This is a starting configuration for the agent to verify, not an already-tested project. Keep the ordinary `Launcher.main()` separate from the `Application` subclass to support classpath distribution launch. In `Launcher`, call `Application.launch(TaikoApplication.class, args)`. Do not add `module-info.java` for this baseline.

JavaFX needs native libraries matching the OS/architecture. Let Gradle resolve them; do not mix a manually downloaded SDK into the same build. Verify native-access flags in both development and packaged launch paths; the packaged classpath launcher uses `ALL-UNNAMED`.

### Windows commands after bootstrap

Run in the project directory in PowerShell, one line at a time:

```powershell
java -version
javac -version
Get-Command java
$env:JAVA_HOME
.\gradlew.bat --version
.\gradlew.bat clean test build
.\gradlew.bat run
.\gradlew.bat installDist
.\build\install\Taiko2D\bin\Taiko2D.bat
```

`run` remains active until the window closes. Use a second terminal if needed. `JAVA_HOME` must point to the JDK root, not its `bin` directory. Both `java` and `javac` should report 25; IntelliJ's Gradle JVM must agree. A toolchain can select the compiler, but an incompatible JVM can still prevent Gradle itself from starting.

The first dependency download needs internet. After resolving dependencies, verify `.\gradlew.bat --offline test build` for offline rebuilding on that machine. A fresh machine still needs cached dependencies or an initial download.

## 13. Follow-up Codex prompts for implementation and testing

Use one stage at a time. If you want to write the gameplay yourself, ask Codex for scaffolding or review rather than a complete implementation. Keep all generated-code attribution accurate.

### Stage A: audio/timing spike

```text
Read docs/PROJECT_SPEC.md. Implement only the audio/timing spike: AudioTransport,
JavaFX audio playback, a song-position display, a Canvas lane driven by audio
position, and a rights-cleared or self-generated click-track fixture. Use a
monotonic clock only for bounded interpolation, never a frame counter.
Show readiness, errors, and stalls. Test the 60-second drift target and record
actual measurements on Windows if possible. Keep the backend replaceable.
Do not build the full editor or add holds. Report unverified audio behavior.
Update AI attribution and tell me where to implement the judgement engine next.
```

### Stage B: normal-note gameplay

```text
Implement/review the normal-note MVP from docs/PROJECT_SPEC.md using the existing
timing code. Separate a pure-Java JudgementEngine from JavaFX rendering/input.
Implement don/ka press edges, nearest matching-note selection, repeat suppression,
single resolution per note, misses, score/combo and results. Use exact documented
offset signs and inclusive timing boundaries. Test boundary errors, wrong keys,
tie breaking, duplicate presses, frame-independent position and restart/focus loss.
Use a FakeSongClock for deterministic tests; do not rely on Thread.sleep.
Do not implement Space holds yet. Preserve my code and label generated changes.
```

### Stage C: format and integrated editor

```text
Implement/review v1 manifest/difficulty JSON and secure .tmap handling exactly as
docs/PROJECT_SPEC.md defines. Strictly validate before accepting imported data.
Add the small editor with audio selection, metadata/BPM/offset, normal-note tools,
snap, zoom/scroll, exact-time editing, dirty-state protection, save/open, export,
and snapshot test-play. Share timing/transforms with gameplay.
Test valid round trips, nonzero offsets, invalid schema/semantic cases, traversal,
duplicate/case-colliding ZIP entries, size/expansion limits and failed-save recovery.
Verify that exported maps play and reopening retains the same chart.
No osu! imports and no hold fields in v1. Record generated code and actual checks.
```

### Stage D: Windows build and handoff

```text
Prepare a reproducible Windows submission build for this project. Read the spec
and attribution files. Run clean tests/build, create installDist and test its
launcher from a path containing spaces outside the source directory.
Then, if practical, create a jpackage app-image with a bundled runtime and verify
it on Windows without depending on a globally installed Java. Include JavaFX
native libraries and license notices. Confirm audio, resource paths, map import,
editor export/reopen and shutdown in the packaged build. Do not claim success
for tests or UI actions you could not perform. Write docs/TEST_REPORT.md with
versions, commands, results and limitations. Do not publish or submit to Moodle.
```

### Stage E: optional hold note

```text
The normal-note MVP is complete. Add the optional Space hold from section 11.
Introduce version 2 with a conditional hold schema and continue to load v1.
Add hold-track rendering and editor drag/resize tools. Implement head, sustain
and tail state transitions independently of frame rate, with one final judgement
per hold. Test early/late head, early release, correct tail, never releasing,
Space already held, focus loss, seek/restart and overlapping hold rejection.
Keep centre/rim input working while holding Space. Update AI attribution.
```

## 14. Packaging and verification checklist

`installDist` provides scripts and dependency JARs but normally still needs an installed Java runtime. A plain application JAR alone is not a complete JavaFX Windows distribution.

For a self-contained Windows folder, use JDK 25 `jpackage --type app-image`. Build it on Windows for Windows. The JDK tool supports app images and native packages; installer creation has extra platform prerequisites. Avoid spending the MVP schedule on MSI/EXE installers. [Packaging overview](https://docs.oracle.com/en/java/javase/25/jpackage/packaging-overview.html).

After a verified `installDist`, a baseline command is:

```powershell
jpackage --type app-image --name Taiko2D --app-version 0.1.0 --input ".\build\install\Taiko2D\lib" --main-jar "Taiko2D-0.1.0.jar" --main-class de.thu.taiko2d.Launcher --add-modules java.se --java-options "--enable-native-access=ALL-UNNAMED" --dest ".\build\package"
```

The agent must confirm the actual JAR filename, JavaFX platform JAR contents, and runtime requirements before treating this as complete. `java.se` intentionally favors completeness over minimum image size; JavaFX JARs/natives remain in the input. If the distribution layout differs, adapt the command and document the result. The output folder may need a new destination for a repeated packaging run.

Test `build\package\Taiko2D\Taiko2D.exe`, then distribute the **whole app-image folder**, usually zipped, with licenses and README. Test after extraction into another folder and on the feedback partner's machine where possible. Avoid paths tied to your working directory.

### Automated checks

- [ ] Strict JSON parsing, required fields, supported versions, IDs, bounds, ordering.
- [ ] Timing boundaries at ±45 ms and ±100 ms, offset signs, misses counted once.
- [ ] Equivalent note position for the same song time at different frame rates.
- [ ] Editor coordinate inversion/snap with nonzero chart offset.
- [ ] Save/load and package round trips retain semantic chart equality.
- [ ] Unsafe ZIP paths/collisions/oversized entries fail without outside writes.
- [ ] Interrupted or failed save preserves the previous valid document.

### Manual Windows checks

- [ ] Clean build and actual launcher open successfully.
- [ ] Audio plays; first/last click alignment remains stable.
- [ ] Both normal-note types respond and holding a key does not auto-hit.
- [ ] Window resizing preserves usable lane/editor coordinates.
- [ ] Import → select → play → results works.
- [ ] Edit → save → export → reopen → test-play works without losing edits.
- [ ] Missing/invalid audio or maps show understandable errors.
- [ ] Focus loss/restart does not leave keys or audio running.
- [ ] Packaged application works with a path containing spaces and no global JDK dependency when using app-image.
- [ ] No leaked players, timers, file locks, or staging folders after repeated use.

A passed build is not evidence that sound, timing, or the editor was tested. Record those checks separately, with failures and remaining limitations.

## 15. Licensing, attribution, and assignment compliance

Create original visual assets and use an original project name for the submission. Describe the mechanics as Taiko-inspired. Do not bundle commercial songs, official Taiko/osu! graphics, characters, logos, or extracted game assets without permission. No osu! import code or format dependency is needed.

Prefer your own short music/click track or clearly licensed material that permits the intended redistribution. “Royalty free” alone is not a usable license statement. Record title, creator, source URL, exact license, required credit, and edits for every external asset. If redistribution rights are unclear, leave the asset out of the published package. Keep each package's `LICENSES.txt` truthful and readable.

Dependencies carry their own terms. OpenJFX uses GPLv2 with the Classpath Exception; Gson uses Apache-2.0. Include applicable upstream notices when distributing libraries/runtime and inspect the actual shipped versions' license files. Do not infer that one dependency's exception removes all redistribution obligations. [OpenJFX license](https://github.com/openjdk/jfx/blob/master/LICENSE), [Gson license](https://github.com/google/gson/blob/main/LICENSE).

Choose a license for your own code only if you intend to publish it and have the necessary rights. Do not apply your code license to assets you do not own. For uncertain license combinations, check the terms or obtain guidance before publication.

Attribution files:

| File | Contents |
| --- | --- |
| `SOURCES.md` | Libraries, tutorials, reused snippets and assets; URLs, versions/licenses, and where used. |
| `CONTRIBUTIONS.md` | Your authored work and design decisions, clearly distinguished from reused/generated work. |
| `AI_USAGE.md` | Tool/model if known, dates, purpose, files/sections generated, review/changes, and tests actually performed. |
| `TEST_REPORT.md` | Environment, build commands, automated results, manual GUI/audio checks, limitations. |

Use the lecturer's required code markings from the separate Moodle AI document when available. A log is useful but may not satisfy that policy by itself. You should understand and be able to explain submitted code; do not describe AI-generated code as independently authored. This guide does not confirm that all forms of AI assistance are permitted.

## 16. Suggested schedule to mid-November

These are self-imposed milestones in Europe/Berlin, not official Moodle deadlines. Adjust around your other coursework.

| Dates, 2026 | Deliverable |
| --- | --- |
| 13–18 October | Bootstrap; actual JavaFX launch; timing spike; final MVP scope. |
| 19–25 October | Normal-note gameplay, judgement tests, restart/results, v1 loading. |
| 26 October–1 November | Small integrated editor and save/open/export round trips. |
| 2–8 November | Secure imports, visual polish, Windows distribution, optional holds only if MVP is stable. |
| 9–12 November | Feature freeze; regression checks; partner run instructions; screenshot and attribution review. |
| 13–15 November | Buffer; silent screencast under 50 MB; rehearse 2½-minute pitch; prepare uploads before the actual deadlines. |

If short of time: cut holds, waveform, fancy effects, and persistent scores. Keep normal-note gameplay, a usable editor, reliable saving, and reproducible launch.

Pitch outline: about 20 seconds for the idea/controls, 50 seconds for gameplay and visible graphics, 45 seconds for placing/exporting/test-playing a note, and 35 seconds for your contributions, timing approach, and limitations. Explain the rhythm visually because the required screencast has no sound.

## 17. Ready-to-start checklist

- [ ] Create/open **Java → Gradle → Groovy → JDK 25** in IntelliJ.
- [ ] Open that same folder in Codex and provide this specification.
- [ ] Send the **bootstrap-only prompt** from section 12.
- [ ] Verify the real window and build before adding gameplay.
- [ ] Read the separate Moodle AI policy and confirm exact deadlines.
- [ ] Implement the timing spike, then normal notes, then editor; add Space holds only if time remains.

