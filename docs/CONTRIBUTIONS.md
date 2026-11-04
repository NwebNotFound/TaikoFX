# Contributions

The user chose the project scope and requested the TaikoFX folder, development
setup and GitHub repository. Design choices are documented in PROJECT_SPEC.md.

Codex generated the initial Gradle configuration, JavaFX launcher/window/Canvas,
CSS, developer script and setup documentation on 13 October 2026. These are
AI-assisted contributions, not independently authored student implementation.

Future student-authored gameplay, audio, editor and format work should be recorded
here as it is completed. No gameplay or full editor exists in this bootstrap.

## Update 4 November 2026

Student-written: the bodies of `render/Matrix3x3.java` (identity, translate, rotate,
scale, shear, multiply, transform) and the tests for scale, shear and multiply order
in `Matrix3x3Test.java`, then revised after review feedback. The class skeleton and
the first three tests were AI-generated, see AI_USAGE.md.

Student-written: the hit-note flight motion in `GameCanvas.drawHitFlights` (half-arch
path, growth and fade, built from the student's own `Matrix3x3` chain), tuned by the
student after AI feedback on wrong constants.

AI-assisted, not student-written: the song-select redesign, the surrounding flight
scaffold (flight map, `noteHit`, `lastHitNote()` getter), see AI_USAGE.md. Planned
student work: pixel-art assets and a pivot-based drum pulse.
