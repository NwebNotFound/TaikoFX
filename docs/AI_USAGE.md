# AI usage

Date: 13 October 2026. Tool: OpenAI Codex desktop; exact model identifier is not
provided to this project log. Purpose: development environment and bootstrap only.

Generated: build.gradle, settings.gradle, .gitignore, AGENTS.md, README.md,
dev.ps1, Launcher.java, TaikoApplication.java, render/GameCanvas.java, css/app.css
and docs/{SOURCES,CONTRIBUTIONS,AI_USAGE,TEST_REPORT}. Gradle generated its official
wrapper scripts/JAR/properties; PROJECT_SPEC.md is a copy of the user-supplied guide.

Also generated .gitattributes for portable wrapper line endings. The distribution
launcher selects software rendering after the hardware launch rendered blank;
development run remains on its working default pipeline.

Scope: minimal static JavaFX window. No music, scoring, note model, game engine,
editor or archive importer has been implemented. Source comments identify the
bootstrap as AI-assisted. Actual verification is recorded in TEST_REPORT.md.

The separate Moodle AI policy remains unread and must be reviewed by the student.
These markings may need adjustment to the lecturer's required format. This log
does not establish what AI assistance the assignment permits.

## 20 October 2026 - requested colour palettes

Codex applied the user's two supplied palettes to `render/GameCanvas.java` and
`css/app.css`. Centre notes use #F95454 with #C62E2E borders; rim notes use
#0D92F4 with #77CDFF borders. The UI uses #222831, #393E46, #948979 and #DFD0B8.
The existing CSS colour customizations were replaced at the user's request;
other source files were preserved. No gameplay or editor was implemented.
The three supplied lecture PDFs were reviewed to advise the student on where
to start and how the planned game relates to the lecture topics.
Visual verification also exposed a Canvas/parent preferred-size feedback issue
when resizing. Codex set an explicit preferred size on the lane container and
raised the minimum window height in `TaikoApplication.java` so the preview and
controls remain visible. Offline build/installDist passed; no unit tests exist.

## 27 October 2026 - map example

Codex created examples/heart-crush manifest, placeholder chart, license note and README as a reference for implementing the v1 loader. Note times and BPM are illustrative, not synchronized to the supplied song. Audio is not bundled. No game code was changed or playback verified.

## 4 November 2026 - song select, hit animation, matrix scaffold

Tool: Claude Code (Anthropic Claude Sonnet 5.5).

- Song select screen: AI redesigned `showSongSelection()` in `TaikoApplication.java`
  and the matching rules in `css/app.css` in an osu!lazer style (card carousel,
  details panel, search box, bottom bar). A later CSS fix gave the cards room so
  the selected card is no longer clipped when it slides out.
- Matrix3x3: AI generated only the class skeleton (signatures, TODO comments) and
  the first three tests in `Matrix3x3Test.java` (translate, rotate, pivot rotation)
  and fixed their compile errors. AI reviewed the student's finished implementation
  and tests for correctness and pointed out weaknesses in some of the new tests.
- Verification: `dev.ps1 clean test build` passed. The new screens and animation
  were not visually checked by AI.
- Sprites (2026-11-05): student drew the 32x32 sprites and exported the PNGs. AI copied them into resources and changed `GameCanvas` to draw notes and the hit area as images (`loadSprite`/`drawSprite`) instead of ovals; the student's own loader draft was merged into it. Build passed; the result was not visually checked by AI.
- Sprite scaling and key feedback (2026-11-05): AI changed `GameCanvas` to an integer `SPRITE_SCALE` (3x) with whole-pixel snapping, a lane that grows with the sprites, and `showKeyPress` for the four hit sprites, called from the key handler in `TaikoApplication`. Build passed; not visually checked by AI.

## 9 November 2026 - developer-only osu! map converter

Tool: Claude Code (Anthropic Claude Opus 5.5). At the student's request, and as a
deliberate exception to the spec's "no osu! importing" rule, AI generated a
**separate developer tool** instead of an in-game importer. The game itself still
has no osu! code or format dependency.

- Generated: `src/tools/java/de/thu/taiko2d/tools/osu/{OsuChart,ConvertOsu}.java`,
  `src/test/java/de/thu/taiko2d/tools/osu/OsuChartTest.java`, the `tools` source
  set and `convertOsu` task in `build.gradle`, and `.gitignore` entries.
- Converts `.osz`/`.osu` (osu!taiko, osu!standard, osu!catch; not mania) into
  `examples/osu-<artist-title>/` with normal don/ka notes only. Drumrolls,
  spinners and big-note flags are dropped. Standard/catch slider starts become notes;
  ka comes from whistle/clap hitsounds, or from playfield position when hitsounds are sparse.
- Converted maps contain third-party songs/charts and are git-ignored; they must
  not be committed, published or submitted.
- Verification: `dev.ps1 clean test build` passed (31 tests, 5 new, headless with
  a synthetic .osz). The tool source is not in the game jar or distribution.
  It was not yet run on a real downloaded map, and gameplay with converted maps was not checked.

## 9 November 2026 - festival gameplay screen, health/fever, results, pause

Tool: Claude Code (Anthropic Claude Opus 5.5). Kept short at the student's request.

- AI-generated: `game/HealthGauge` (health + fever rules chosen by the student),
  `game/ComboMilestone`, `model/ChartInfo` (BPM-based scroll speed),
  `BeatmapJson.loadChartInfo`, `JudgementEngine.finished()/lastNoteMs()`,
  `render/{PixelArt,FestivalScene}` with placeholder pixel art (lanterns, crowd,
  yagura, mascot frames), the new gameplay layout in `GameCanvas.draw()`, one
  press slot per drum half, difficulty choice + speed setting in song select,
  pause menu, results screen, hidden editor button and the new pixel CSS theme.
  The converter now also stores the osu! slider multiplier as `scrollMultiplier`.
- Scaffold only, student to write: `WaveHealthBar.waveOffset/snap` and
  `Mascot.idleTransform/hitTransform/flipTransform` (Matrix3x3 chains). Until then
  they are flat/identity placeholders. The bar drawing loop and mascot drawing are AI.
- Unchanged student code: `Matrix3x3`, `drawHitFlights`, the 32x32 sprites.
- Follow-up: fever meter drains when idle and during fever; `hype` grows the crowd, which
  is scattered on a jittered grid over the lower screen and walks in/out from the sides;
  fever glowstick dance (AI).
- Student-written since: `WaveHealthBar.waveOffset` (two travelling sines) and `snap`, and
  the three `Mascot` transforms (beat squash, hit lean, pivot flip).
- Tests: HealthGauge, ComboMilestone, ChartInfo and converter tests added. Gameplay
  view checked only as offscreen canvas snapshots, not played with audio.

## 9 November 2026 - anti-spam judging, fever on easy maps, kiai

Tool: Claude Code (Anthropic Claude Opus 5.5). AI-generated at the student's request:
- `JudgementEngine.missNearestNote`: a wrong-drum press, or one 101-150 ms early, now
  misses the nearest note (osu!taiko-style). This deliberately changes the spec's initial
  rule "wrong type does not consume the note"; `JudgementTest` was updated accordingly.
- `HealthGauge(noteCount, length)`: fever needs ~1/10 of the notes (10..30 hits) and the
  idle grace is 3 average note gaps; kiai doubles fever charge.
- Kiai sections: parsed from osu! timing points by the converter, `ChartInfo.kiai`,
  fireworks/sky flash/lit lanterns/jumping crowd/flashing lane border during kiai.
- Kiai toned down after play-testing: no lane-border or sky flash; one small, dim firework
  per bar in the top sky band.
- `GameCanvas.drawDrumHalf`: left presses draw only the left half of the student's drum-hit
  sprite, right presses only the right half, so simultaneous presses do not overlap.
- Judging changed again after play-testing a dense converted map (Cycle Hit, 86 ms streams):
  in-order instead of nearest-note matching, passed notes may be skipped, upcoming notes
  block; wrong-colour presses only cost upcoming notes. Replaces the spec's "nearest" rule.
- `game/LeadInClock` (+ tests with a fake audio): artificial silent intro so the first notes
  scroll in, Space skips long intros; the lane no longer changes colour in fever.
- Health softened after play-testing ("fails too fast"): miss 0.08 -> 0.05, perfect/good
  gain 0.01/0.005 -> 0.02/0.015, misses cost half below 30 % health (26 straight misses fail
  instead of 13). Space also skips long breaks between notes (`LeadInClock.skipTargetAt`).
- Song select regrouped osu!-style: the selected song opens and lists its difficulties
  underneath (up/down walk difficulties, left/right jump songs). Local leaderboard per
  difficulty: `model/ScoreEntry`, `model/Leaderboard`, `io/ScoreStore` (~/.taikofx/scores.json,
  broken files moved aside) + tests; cleared plays are saved from the results screen.
- Kiai made visible again: fireworks every two beats with a rising rocket and two rings,
  plus two fountains on the yagura roof for 3.5 s when a kiai section starts
  (`ChartInfo.lastKiaiStart`, ignores sub-second kiai flashes). Placeholder effects in the
  AI-generated `FestivalScene`; the student may replace them.

## 10 November 2026 - window-size scaling

Tool: Claude Code (Anthropic Claude Opus 5.5). AI-generated at the student's request
("when resizing or going from full screen to windowed a lot looks bad or too small"):
- `render/ViewScale` (+ `ViewScaleTest`): one uniform scale from the window size, based on
  an 800 x 560 design size; the gameplay variant snaps to thirds so the 32 px sprites stay crisp.
- `GameCanvas.draw` now applies that scale to the GraphicsContext and draws the scene in
  design units (`viewWidth()/viewHeight()`), so lane, notes, crowd, mascot and text grow
  with the window. The student's hit-flight Matrix3x3 chain is unchanged.
- `ScaledPane` scales the menus, song select, results and pause overlay the same way.
- `Mascot.draw`: `g.setTransform(M)` -> `g.transform(M)`, so the student's mascot matrix is
  multiplied onto the window scale instead of replacing it (the mascot did not scale).
- The gameplay canvas is now unmanaged in its StackPane; before, its bound size became the
  pane's minimum size and the window could not shrink back after being maximised.
