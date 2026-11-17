# ZakoOpt — notes for agents

Client-side Minecraft optimization mod: Fabric and NeoForge, one jar per game version, all built from one repository.
Read [DECISIONS.md](DECISIONS.md) before proposing an optimization: it lists what was measured, what was kept and
what was tried and dropped.

## Layout

- **Version modules** (`1.21.1/`, `1.21.4/`, `1.21.8/`, `1.21.10/`, `1.21.11/`, `26.1.2/`, `26.2/`, `26.3/`) each build one jar
  (1.21.1 also builds `-neoforge`; 26.x jars run on both loaders).
- **Layers** (`common/`, `1.21.x/`, `1.21.4+/`, `1.21.8+/`, `1.21.9+/`, `26.x/`, `26.2+/`, ranges like `1.21.8-1.21.11/` …)
  hold code shared by several modules. A module lists its layers in `gradle.properties` → `layers=`.
  The list of layer names is `layerNames` in the root `build.gradle`.
- A layer is named after the game versions it serves. `1.21.9+` starts at 1.21.9 because the 1.21.10 module also runs there.
- `./gradlew :<version>:layerMap -Pfile=<part of a path>` shows which folder each source file of a version comes from.

## Layer rules

- A module's own file **replaces** the layer file at the same path only in modules with a `renames.txt`
  (1.21.1, 1.21.11, 26.1.2, 26.2, 26.3). In the others both are compiled: never keep a copy of a layer file there.
- Two layers used by one module must not hold the same path: layers do not override each other.
- Move a cross-version copy into a layer only when it is **byte-identical** (check md5) and an existing layer is used by
  **exactly** the modules that carry it. Otherwise leave the copies: most of them differ by Minecraft API on purpose.
- Every layer and module may bring `zakoopt[-<name>].mixins.json`; moving a mixin means moving its entry too.
- `renames.txt` rewrites class names in layer sources for that module (e.g. `Identifier=ResourceLocation` on 1.21.1).
  26.3 renames `com.mojang.blaze3d.vertex.VertexFormat` to `com.mojang.renderpearl.api.vertex.VertexFormat`:
  code in a 26.3 module file must import the renderpearl one.

## Build

- JDK 26: `JAVA_HOME=~/.jdks/openjdk-26.0.2`. Gradle needs a JVM of 25 or newer.
- Build version modules only, never the root `build` (it tries the layers and fails):
  `./gradlew :1.21.1:build :1.21.4:build :1.21.8:build :1.21.10:build :1.21.11:build :26.1.2:build :26.2:build :26.3:build -x test`
  → 9 jars in `*/build/libs/`. There are no tests: a change is checked by building every version and running it.

## Running and checking in game

- `./gradlew :<version>:runClient -Pworld=<save> -Pbench=<ctl dir> -Pjvm="..."`.
- zakobench (`zakobench/`) reads `<ctl dir>/cmd.txt`: first line an id, then one command per line — `/cmd` for game
  commands, `!newworld <name> <seed>`, `!wait <s>`, `!look <yaw> <pitch>`, `!view third`, `!inv`, `!close`,
  `!shot <path>` (a Windows path, `C:/…`), `!quit`. `done.txt` gets the id when finished, `joined.txt` on join.
  The bundled `bench` world has commands off; `!newworld` makes one with commands on.
- Toggle features for a test in `<version>/run/config/zakoopt.json` and put it back afterwards.
- Look for `Mixin`, `InvalidInjection` and `zakoopt … failed` in `<version>/run/logs/latest.log`.
- Measure FPS with the in-game benchmark (several A/B pairs, median), never a single run; see the noise notes in DECISIONS.md.

## Code conventions

- Lombok everywhere it applies; match the surrounding class (`@UtilityClass`, `@FieldDefaults`, fluent accessors in mixins).
- Comments only for a non-obvious trap or constant, one line. No Javadoc that restates the signature.
- A mixin that hooks another mod's event must survive that mod reloading; mixins that can fail at runtime turn their
  feature off and fall back to vanilla (`Guard`), except hot entity/particle handlers (wrapping them cost 5% FPS).
- A feature that changes visuals says so in its tooltip; new settings default to the vanilla look.

## Releases

- `version=` in `gradle.properties`, `release/<version>/changelog.md` (English + Russian), jars copied there
  (`release/` is git-ignored), commit `<version>: <what>`, tag `v<version>`, `gh release create` with the jars.
- No `Co-Authored-By` or tool signature in commits and PRs.
- Issues: answer in the reporter's language (Russian and English when unsure). Close only when the fix is verified;
  otherwise ask the reporter to test and leave it open.
