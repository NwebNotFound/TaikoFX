# TaikoFX development

Read `docs/PROJECT_SPEC.md` for the design and `docs/TEST_REPORT.md` for verified setup.
The repository is a single non-modular Java 25 / Gradle Groovy / JavaFX project.
The project name is TaikoFX; `de.thu.taiko2d` remains the Java package from the specification.

Preserve the student's existing work and implement only the requested stage.
Keep gameplay/judging, rendering, audio, editor and package I/O separate.
Do not generate dozens of empty classes. No osu! importing or optional holds yet.
Mark generated source and update `docs/AI_USAGE.md` honestly. The separate Moodle
AI policy still needs review; follow its required format once supplied.
Do not publish or submit to Moodle without an explicit user request.

On Windows use `powershell -ExecutionPolicy Bypass -File .\dev.ps1 clean test build`
or select JDK 25 in the terminal before using `gradlew.bat`. Use real deterministic
headless tests when a pure-Java model exists; do not manufacture bootstrap coverage.
Distinguish builds, visible GUI checks, audio checks and GitHub push verification.
