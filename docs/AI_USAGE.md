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
- Hit animation: AI added `noteHit`/`drawHitFlights` in `render/GameCanvas.java`
  (hit notes fly left in an arc, grow and fade) and a `lastHitNote()` getter in
  `game/JudgementEngine.java`, plus the call in `TaikoApplication.java`. The
  student plans to replace the hand-written flight math with their own matrices.
- Matrix3x3: AI generated only the class skeleton (signatures, TODO comments) and
  the first three tests in `Matrix3x3Test.java` (translate, rotate, pivot rotation)
  and fixed their compile errors. AI reviewed the student's finished implementation
  and tests for correctness and pointed out weaknesses in some of the new tests.
- Verification: `dev.ps1 clean test build` passed. The new screens and animation
  were not visually checked by AI.
