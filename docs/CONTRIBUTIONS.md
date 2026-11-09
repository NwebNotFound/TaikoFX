# Contributions

The user chose the project scope and requested the TaikoFX folder, development
setup and GitHub repository. Design choices are documented in PROJECT_SPEC.md.

Codex generated the initial Gradle configuration, JavaFX launcher/window/Canvas,
CSS, developer script and setup documentation on 13 October 2026. These are
AI-assisted contributions, not independently authored student implementation.

Student-authored gameplay, audio, editor and format work is recorded here as it is
completed. The bootstrap itself had no gameplay; later updates below cover what was added.

## Update 4 November 2026

Student-written: the bodies of `render/Matrix3x3.java` (identity, translate, rotate,
scale, shear, multiply, transform) and the tests for scale, shear and multiply order
in `Matrix3x3Test.java`, then revised after review feedback. The class skeleton and
the first three tests were AI-generated, see AI_USAGE.md.

Student-written: the hit-note flight motion in `GameCanvas.drawHitFlights` (half-arch
path, growth and fade, built from the student's own `Matrix3x3` chain), tuned by the
student after AI feedback on wrong constants.

AI-assisted, not student-written: the song-select redesign, the surrounding flight
scaffold (flight map, `noteHit`, `lastHitNote()` getter), see AI_USAGE.md.

Student-written: the 32x32 note / hit-area / hit-effect sprites (drawn in Aseprite,
exported to PNG under `src/main/resources/de/thu/taiko2d/sprites`).

## Update 9 November 2026

Student-written: `WaveHealthBar.waveOffset` (two travelling sines) and `snap` (art-pixel
grid snap). Student-written: the `Mascot` motion transforms
`idleTransform` (beat squash about the feet), `hitTransform` (lean + squash fading out),
`flipTransform` (parabola jump + rotation about the body-centre pivot:
`translate(pivot) * rotate * translate(-pivot)`), `sadTransform` (horizontal shake fading
out) and `onMiss` (stores miss time; wired from `GameCanvas.showJudgement` on MISS).

AI-assisted, not student-written: the festival gameplay screen scaffold (`HealthGauge`,
`ComboMilestone`, `ChartInfo`, `PixelArt`, `FestivalScene`, `WaveHealthBar` drawing loop,
`Mascot` state/drawing/pixel frames), pause/results, song-select difficulty grouping,
leaderboard, osu! converter tool, kiai effects and judging tweaks — see AI_USAGE.md.
