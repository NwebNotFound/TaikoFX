# Bootstrap verification — 13 October 2026

Project: `C:\Users\nw05\Documents\Code\TaikoFX`. Windows 11 x64.
IntelliJ IDEA Ultimate 2025.2 imported the Gradle project successfully; its log
records successful external project resolution and the main/test modules with
JDK `openjdk-25`. Both Project SDK and Gradle JVM are configured to that JDK.
Local `.idea` files are excluded from Git so another PC can select its own SDK.

## Verified environment

- Oracle OpenJDK 25, build 25+36-3489, at `%USERPROFILE%\.jdks\openjdk-25`.
- Gradle wrapper 9.1.0; launcher and daemon both report JVM 25.
- JavaFX controls/media 25.0.4 and matching Windows native libraries.
- OpenJFX plugin 0.1.0; Gson 2.14.0; JUnit BOM/Jupiter/launcher 6.0.3.
- Gradle ZIP SHA-256: `a17ddd85a26b6a7f5ddb71ff8b05fc5104c0202c6e64782429790c933686c806`.
- Official wrapper JAR SHA-256: `76805e32c009c0cf0dd5d206bddc9fb22ea42e84db904b764f3047de095493f3`.
  Both hashes checked against Gradle's official distribution endpoints. The
  distribution checksum is pinned in `gradle-wrapper.properties`.

The previous user JAVA_HOME selected Java 21. It now selects the installed
OpenJDK 25. Existing terminals/apps need restarting to inherit this setting.
The machine PATH still contains Java 21 first, so a bare `java -version` can
report 21. `dev.ps1` temporarily puts JDK 25 first and Gradle's wrapper uses
JAVA_HOME. No other JDK/JRE installations were removed or machine PATH changed.

## Build checks

Commands below were run through `powershell -NoProfile -ExecutionPolicy Bypass
-File .\dev.ps1`:

| Arguments | Result |
| --- | --- |
| `--version` | Gradle 9.1.0; launcher and daemon JDK 25. |
| `clean test build resolveDependencies installDist --console plain` | Passed; all pinned runtime/test dependencies resolved. |
| `--offline clean test build resolveDependencies --console plain` | Passed, including compilation from clean outputs without downloading dependencies. |
| `--offline installDist --console plain` | Passed; regenerated distribution after clean build. |
| `run --console plain` | Actual JavaFX window opened on Windows. |

`test` reports **NO-SOURCE**. No model or unit tests exist yet. Dependency
resolution and a successful build are not unit-test coverage.

## Visible GUI checks

- Development JavaFX window opened: title, CSS, Canvas lane, target and two static
  coloured example notes rendered correctly.
- Play and Editor buttons update their respective placeholder status messages.
- A copied `installDist` launcher opened from `work\Launcher Path Test\TaikoFX`
  outside the source folder, with spaces in its path and a different working directory.
- The classpath distribution emits JavaFX's 'Unsupported JavaFX configuration:
  classes were loaded from unnamed module' warning. The window still opens;
  there was no native-access failure. This follows the non-modular baseline.

## Scope and remaining checks

No audio transport, music, animation clock, hit judging, map loading, editor or
packaging with a bundled runtime exists yet. Audio and one-minute drift targets
are **unverified**. Interactive window resizing and another physical PC have not
been tested. `installDist` still requires Java 25 on the destination machine.

Before the next stage, review the separate Moodle AI policy and assignment dates.
Next implement the audio/timing spike in PROJECT_SPEC section 7, then a plain-Java
note model and deterministic judgement tests.
