# TaikoFX

Solo JavaFX taiko-style rhythm game (and planned beatmap editor). Based on the
Taiko2D specification in [docs/PROJECT_SPEC.md](docs/PROJECT_SPEC.md); the
repository/app name is **TaikoFX**. Package names remain `de.thu.taiko2d`.

You pick a song and difficulty, notes scroll along a pixel lane, and you hit
don/ka with the keyboard. The festival gameplay screen has a liquid health/fever
bar, a drum mascot that bobs, leans, flips on combo milestones and shakes on
misses, lanterns/crowd/yagura scenery, kiai fireworks, hit-note flight arcs,
pause, results and a local leaderboard. Song select is card-based with search
and per-song difficulty lists. Maps are JSON charts under `examples/`; an
optional developer tool in `src/tools` converts osu! `.osz`/`.osu` into that
format (converted maps are git-ignored and must not be submitted).

Own contributions and AI usage are logged in
[docs/CONTRIBUTIONS.md](docs/CONTRIBUTIONS.md) and
[docs/AI_USAGE.md](docs/AI_USAGE.md). Sources are in
[docs/SOURCES.md](docs/SOURCES.md).

## Features

- Gameplay: don/ka notes, judgement windows, combo, health + fever, lead-in /
  skip for long intros and breaks, pause and results.
- Rendering: Canvas lane with student-drawn 32×32 sprites, hit-note flights via
  `Matrix3x3`, `WaveHealthBar`, `Mascot` transforms, `FestivalScene` pixel art.
- Song select: osu!-style cards, difficulty list under the selected song, speed
  setting, preview audio, local scores in `~/.taikofx/scores.json`.
- Charts: bundled `examples/heart-crush` reference map; developer `convertOsu`
  Gradle task for personal practice maps.
- Tests: JUnit for matrix, judgement, health, combo, chart info, lead-in,
  leaderboard, score store, beatmap JSON and the osu converter (70 tests).

The integrated beatmap editor is still a placeholder / hidden; packaging with a
bundled runtime is not done yet.

## Open in IntelliJ

1. Open this **folder**, not an individual Java file, and import the Gradle project.
2. Set **Project SDK = JDK 25**. Existing Oracle OpenJDK 25 or Temurin 25 works.
3. In **Settings → Build, Execution, Deployment → Build Tools → Gradle**, use the
   **Gradle wrapper** and set **Gradle JVM = Project SDK (25)**.
4. Run the Gradle `application → run` task. Or run `Launcher.main()` after import.
   For a direct Java run configuration add `--enable-native-access=ALL-UNNAMED`.

Do not add a separate JavaFX SDK or `module-info.java`. Gradle manages JavaFX natives.

## Windows terminal

Install a Windows x64 **JDK 25**, not a JRE: [Temurin download](https://adoptium.net/temurin/releases/?version=25).
No global Gradle installation is required. In this folder:

```powershell
powershell -ExecutionPolicy Bypass -File .\dev.ps1 --version
powershell -ExecutionPolicy Bypass -File .\dev.ps1 clean test build
powershell -ExecutionPolicy Bypass -File .\dev.ps1 run
powershell -ExecutionPolicy Bypass -File .\dev.ps1 installDist
powershell -ExecutionPolicy Bypass -File .\dev.ps1 --offline test build
```

`dev.ps1` finds JDK 25 in `JAVA_HOME`, `%USERPROFILE%\.jdks`, or standard Java
installation folders. It selects Java only for that command and restores the
parent environment. With `JAVA_HOME` correctly set, normal `./gradlew.bat`
commands work too. Close the window to end `run`.

After `installDist`, launch `build\install\TaikoFX\bin\TaikoFX.bat` with JDK 25
in `JAVA_HOME`. The distribution needs an installed JDK/runtime; it is not a
self-contained Windows executable. The first build needs internet; a verified
cached build can run offline.

The distribution uses JavaFX software rendering because its hardware launch
rendered blank on the setup machine. Development `run` uses its working default
pipeline. Recheck acceleration during the timing spike and final packaging.

Pinned stack: Java 25, Gradle 9.1.0, JavaFX 25.0.4, OpenJFX plugin 0.1.0,
Gson 2.14.0 and JUnit Jupiter 6.0.3. See the early bootstrap notes in
[docs/TEST_REPORT.md](docs/TEST_REPORT.md).

### Developer osu! converter (optional)

```powershell
powershell -ExecutionPolicy Bypass -File .\dev.ps1 convertOsu --args="C:\path\to\map.osz"
```

Writes into `examples/osu-<artist-title>/`. Those folders are git-ignored;
do not commit, publish or submit converted third-party songs/charts.

## Linux / macOS (Gradle wrapper)

```bash
export JAVA_HOME=/path/to/jdk-25
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew --no-daemon clean test
./gradlew --no-daemon run
```

## Work on another PC

```powershell
git clone https://github.com/NwebNotFound/TaikoFX.git
cd TaikoFX
powershell -ExecutionPolicy Bypass -File .\dev.ps1 clean build
```

The repository is public, so cloning needs no GitHub sign-in. Install JDK 25 on that PC; sign in to GitHub when pushing your changes.
Open the cloned folder in IntelliJ and set both SDK and Gradle JVM to 25.
Before switching machines, commit and push your work. On the other machine,
`git pull --ff-only` before editing. Build caches and personal IDE settings are ignored.

## Where to look in the code

- `TaikoApplication.java`: window, song select, pause, results, input wiring.
- `render/GameCanvas.java`: lane, judgements, flights, health bar and mascot draw.
- `render/Matrix3x3.java`, `WaveHealthBar.java`, `Mascot.java`, `FestivalScene.java`:
  transforms and festival visuals (see CONTRIBUTIONS / AI_USAGE for authorship).
- `game/`: `JudgementEngine`, `HealthGauge`, `ComboMilestone`, `LeadInClock`, `InputState`.
- `model/` + `io/`: notes, chart info, manifests, JSON load, local scores.
- `src/tools/.../osu/`: separate developer converter (not in the game jar).
- `src/main/resources/css/app.css`: colours and UI styling.
- `Launcher.java`: ordinary Java entry point; normally needs no changes.

Review the separate Moodle AI policy and exact assignment dates yourself.
Attribution is in `docs/SOURCES.md`, `docs/CONTRIBUTIONS.md`, and `docs/AI_USAGE.md`.
