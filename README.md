# TaikoFX

Starter for a solo JavaFX rhythm game and beatmap editor. Based on the Taiko2D
specification in [docs/PROJECT_SPEC.md](docs/PROJECT_SPEC.md); the repository/app
name is **TaikoFX**. Package names remain `de.thu.taiko2d`.

The window contains a resizable Canvas lane, hit target and static example notes.
Play and Editor are placeholders; Exit works. Gameplay, audio and map editing
are intentionally left for the next development stages.

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

Pinned stack: Java 25, Gradle 9.1.0, JavaFX 25.0.4, OpenJFX plugin 0.1.0,
Gson 2.14.0 and JUnit Jupiter 6.0.3. There are no tests yet because no pure-Java
model has been created. See [test report](docs/TEST_REPORT.md).

## Work on another PC

```powershell
git clone https://github.com/NwebNotFound/TaikoFX.git
cd TaikoFX
powershell -ExecutionPolicy Bypass -File .\dev.ps1 clean build
```

Sign in to GitHub for the private repository and install JDK 25 on that PC.
Open the cloned folder in IntelliJ and set both SDK and Gradle JVM to 25.
Before switching machines, commit and push your work. On the other machine,
`git pull --ff-only` before editing. Build caches and personal IDE settings are ignored.

## Start implementing

- `TaikoApplication.java`: main window composition and placeholder navigation.
- `render/GameCanvas.java`: lane drawing; later calculate note X from song time.
- `Launcher.java`: ordinary Java entry point; normally needs no changes.
- `src/main/resources/css/app.css`: colours and UI styling.
- Create `audio/AudioTransport.java` and a JavaFX implementation first, then test
  a generated click track and timing as described in section 7 of the specification.
- Then add plain `model` types and a deterministic `game/JudgementEngine`, followed
  by the integrated editor and bounded `.tmap` I/O. Empty package directories are
  prepared locally; Git tracks files, so create packages as you add classes.

Review the separate Moodle AI policy and exact assignment dates yourself.
Attribution is in `docs/SOURCES.md`, `docs/CONTRIBUTIONS.md`, and `docs/AI_USAGE.md`.
