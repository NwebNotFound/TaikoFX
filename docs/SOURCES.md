# Sources

- User-supplied Taiko2D project specification, 13 October 2026, copied unchanged to PROJECT_SPEC.md.
- Gradle 9.1.0: https://docs.gradle.org/9.1.0/release-notes.html (Apache-2.0).
  Distribution and wrapper hashes verified against https://gradle.org/release-checksums/.
- OpenJFX 25.0.4: https://openjfx.io/openjfx-docs/ (GPLv2 with Classpath Exception).
- OpenJFX Gradle plugin 0.1.0: https://plugins.gradle.org/plugin/org.openjfx.javafxplugin (BSD-3-Clause).
- Gson 2.14.0: https://github.com/google/gson (Apache-2.0).
- JUnit Jupiter 6.0.3: https://docs.junit.org/6.0.3/release-notes.html (EPL-2.0).

- Note and hit-area pixel art (32x32): made by the student as pixel grids in code
  (`RED_NOTE`, `BLUE_NOTE`, `HIT_AREA` in `render/GameCanvas.java`). The earlier Aseprite
  PNGs in src/main/resources/de/thu/taiko2d/sprites are the student's first drafts and
  are no longer used by the game.
- Festival scenery and mascot pixel art (`render/PixelArt.java`, `render/FestivalScene.java`):
  AI-generated placeholder art, see AI_USAGE.md.
- Songs/maps: none bundled. Players import osu! beatmaps themselves; the music and charts
  belong to their respective artists and mappers and are not part of this repository.

No external artwork, music or reused game implementation is bundled.
Canvas shapes and CSS were generated for this bootstrap. Inspect upstream notices
before distributing dependency libraries or a bundled runtime. No project license
has been selected on the student's behalf.

