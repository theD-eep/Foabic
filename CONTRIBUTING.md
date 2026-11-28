# Contributing

Thanks for helping. Foabic is deliberately small: read the scope rules before writing code.

## Requirements

| Dependency | Version | Notes |
| --- | --- | --- |
| JDK | **25** | compiled with `--release 25` |
| Gradle | **9.5.1** | checked-in wrapper |
| Minecraft server bundle | **26.3** | official `server.jar`; class names are already deobfuscated |
| Fabric Loader | **0.20.1** | Fabric compile/runtime dependency |
| Fabric API | **0.166.2+26.3** | nested modules are used at compile time |
| NeoForge | **26.3.0.115** | universal jar + installed libraries |

The build is Gradle. `:fabric` uses the non-remapping `net.fabricmc.fabric-loom` plugin; `:neoforge` uses the `java` plugin against the official NeoForge universal artifact. `foabic-gradle-plugin` is an included build. There are no project-local `.cmd` or `.ps1` build scripts.

## Build and test

```bat
gradlew.bat clean build
```

The build compiles the core API, content API, loader, adapters and examples, runs `:coreTest`, enforces the < 15 KiB API limit and the no-platform-imports rule, and copies six jars to `dist/`. The Foabic Loom demo builds itself into `examples/plugin-demo/dist/`. See [docs/BUILD.md](docs/BUILD.md) for the `.cache` input layout.

Expected test tail:

```text
PASS events
PASS failure isolation
PASS versions
PASS legacy use-block signal
PASS player-interact-block signal
PASS player-break-block signal
PASS server-tick signal
PASS networking round trip
PASS loader defenses
PASS data generation
ALL TESTS PASSED (10)
```

Expected API check:

```text
FOABIC_API_SIZE bytes=14421 limit=15360
```

Artifacts in `dist/`:

```text
foabic-api-1.3.0.jar                  14,421 B   core API, no platform imports
foabic-content-1.3.0.jar              68,428 B   Minecraft-facing API
foabic-fabric-1.3.0.jar              169,137 B   Fabric adapter + core + content + loader
foabic-neoforge-1.3.0.jar            167,772 B   NeoForge adapter + core + content + loader
foabic-example-minimal-1.3.0.jar      10,345 B   common acceptance mod
foabic-example-chat-1.3.0.jar          2,786 B   chat reply example
foabic-example-survival-fabric-1.3.0.jar   17,251 B  survival demo
foabic-example-survival-neoforge-1.3.0.jar 17,341 B  survival demo
```

The Gradle build reads local `.cache/` inputs and does not download game files itself.

For adapter changes, install the Gradle-built jars into the prepared Fabric and NeoForge runtimes and start each dedicated server twice. The exact expected observations are in [docs/VERIFICATION.md](docs/VERIFICATION.md).

## Scope rules

v1.3 accepts:

- fixes to either adapter, the five registry views, `submit`, `submitBlock`,
  `submitBlockEntity`, `submitEntity`, `connect`, lifecycle/subscription cleanup,
  the current signals and the legacy `UseBlockSignal` compatibility path;
- block-entity/entity sync fixes and the typed `Synced*` fields;
- data generation, tags, recipes, loot tables, language and generated data-pack
  registration;
- loader discovery, dependency preflight, JSON metadata, config and network
  bridge fixes;
- Foabic Loom packaging, `foabicData`, launcher icon and metadata fixes;
- generated resource/creative-tab fixes that do not require Mixin or a rendering
  hook;
- documentation, tests and the acceptance examples.

Do **not** add on the 1.3 line: Mixin, universal packaging, a full signal
library, GUI APIs, rendering-layer code, custom entity renderers or a live
client test requirement. Those are explicitly deferred; see
[docs/ROADMAP.md](docs/ROADMAP.md).

Architecture rules:

- `src/api` is the core API and must not import Minecraft, Fabric or NeoForge. It must stay small enough for the < 15 KiB jar check.
- `src/content` may reference Minecraft content types used by the public API
  (`Block`, `Item`, `BlockEntityType`, `EntityType`, `SoundEvent`, `Entity`) but
  must not import Fabric or NeoForge loader classes.
- `src/loader` may compile against Minecraft for API signatures and tests, but the loader runtime must not import Fabric or NeoForge classes.
- Platform-specific code belongs in `src/fabric` or `src/neoforge`.
- A registration factory must not be evaluated during NeoForge mod construction; it must run in `RegisterEvent`.
- Resource/data generation must stay JSON-only. Do not parse complex model JSON
  and do not touch a renderer.
- Build-time data providers must not require a live server; use
  `FoabicDataContext`, not world access.
- Generated packs must use 26.3 `min_format`/`max_format`, not the legacy
  `pack_format` field.
- New platform jars must be produced by `foabic-gradle-plugin`, not by hand-written twin build files.

## Style

- 4 spaces, UTF-8, no BOM (the metadata parser tolerates a BOM, but do not add one).
- No third-party runtime dependency beyond the JDK and the platform loader/API the user installs. Gson is already supplied by Minecraft and may be used by the loader.
- No classpath scanning or annotation reflection; the loader only reads `foabic.mod.json` / legacy `foabic.mod`.
- Keep the public API small. If a new public type is not needed by an example or a test, do not add it.
- Public API changes must update [docs/API.md](docs/API.md) and [CHANGELOG.md](CHANGELOG.md).

## Version numbers

The version appears in these places and must match:

1. `build.gradle.kts` — root `version`
2. `src/api/java/dev/foabic/api/Foabic.java` — `VERSION`
3. `src/fabric/resources/fabric.mod.json` — `"version"`
4. `src/neoforge/resources/META-INF/neoforge.mods.toml` — `version`
5. `foabic-gradle-plugin/build.gradle.kts` — plugin `version`
6. `examples/*/src/main/resources/foabic.mod.json` — `version` and `api`
7. `CHANGELOG.md` — the section heading

Tags use a `v` prefix, for example `v1.3.0`.

## Commits and pull requests

1. Fork the repository and branch from `main`: `fix/...`, `feat/...`, `docs/...`.
2. Run `gradlew.bat clean build`; for adapter changes, repeat the server acceptance described in [docs/VERIFICATION.md](docs/VERIFICATION.md).
3. Do not commit `.cache/`, `.gradle/`, `build/`, game worlds, logs or server jars; they are ignored.
4. `dist/` is ignored and is a release artifact, not source. Maintainers regenerate it when cutting a release.
5. One concern per pull request; keep formatting or renaming in a separate change.

## Reporting

- Use the Bug Report template with the full startup log and the exact Fabric Loader / Fabric API / NeoForge versions.
- Security issues: do not open a public issue, follow [SECURITY.md](SECURITY.md).
- Usage questions: read the [README](README.md), [docs/API.md](docs/API.md), [docs/FABRIC.md](docs/FABRIC.md) and [docs/NEOFORGE.md](docs/NEOFORGE.md) first.

## License

By contributing you agree to license your work under [MIT](LICENSE).
