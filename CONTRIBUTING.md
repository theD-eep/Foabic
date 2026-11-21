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

The build is Gradle. `:fabric` uses the non-remapping `net.fabricmc.fabric-loom` plugin; `:neoforge` uses the `java` plugin against the official NeoForge universal artifact. There are no project-local `.cmd` or `.ps1` build scripts.

## Build and test

```bat
gradlew.bat build
```

The build compiles the API, loader, Fabric adapter, NeoForge adapter and minimal example, packages four jars, runs `:coreTest`, and copies the artifacts to `dist/`. See [docs/BUILD.md](docs/BUILD.md) for the `.cache` input layout.

Expected test tail:

```text
PASS events
PASS failure isolation
PASS versions
PASS use-block signal
ALL TESTS PASSED (4)
```

Use `gradlew.bat assemble` to build without running the tests. Artifacts land in `dist/`:

```text
foabic-api-1.0.0.jar                  compile-time API
foabic-fabric-1.0.0.jar               Fabric adapter + API + loader
foabic-neoforge-1.0.0.jar             NeoForge adapter + API + loader
foabic-example-minimal-1.0.0.jar      common acceptance mod
```

The Gradle build reads local `.cache/` inputs and does not download game files itself.

For adapter changes, install the Gradle-built jars into the prepared Fabric and NeoForge runtimes and start each dedicated server twice. The exact expected observations are in [docs/VERIFICATION.md](docs/VERIFICATION.md).

## Scope rules

v1.0 accepts:

- fixes to either adapter, registry factory registration, `submit`, `connect` and the two v1.0 signals;
- loader, dependency resolution and event-bus fixes;
- documentation, tests and the minimal example.

Do **not** add, on the 1.0 branch: Mixin, a Gradle/Maven build plugin, universal packaging, a full signal library, networking, GUI APIs or model/creative-tab generation. Mixin and the build plugin are explicitly v1.1 work — see [docs/ROADMAP.md](docs/ROADMAP.md).

Architecture rules:

- `src/api` may reference Minecraft **content types** used by the public API (`Block`, `Item`), but must not import Fabric or NeoForge loader classes.
- Platform-specific code belongs in `src/fabric` or `src/neoforge`.
- A registration factory must not be evaluated during NeoForge mod construction; it must run in `RegisterEvent`.

## Style

- 4 spaces, UTF-8, no BOM.
- No third-party runtime dependency beyond the JDK and the platform loader/API the user installs.
- No classpath scanning or annotation reflection; the loader only reads `foabic.mod`.
- Keep the public API small. If a new public type is not needed by the example or a test, do not add it.
- Public API changes must update [docs/API.md](docs/API.md) and [CHANGELOG.md](CHANGELOG.md).

## Version numbers

The version appears in these places and must match:

1. `build.gradle.kts` → root `version`
2. `src/api/java/dev/foabic/api/Foabic.java` → `VERSION`
3. `src/fabric/resources/fabric.mod.json` → `"version"`
4. `src/neoforge/resources/META-INF/neoforge.mods.toml` → `version`
5. `CHANGELOG.md` → the section heading

Tags use a `v` prefix, for example `v1.0.0`.

## Commits and pull requests

1. Fork the repository and branch from `main`: `fix/...`, `feat/...`, `docs/...`.
2. Run `gradlew.bat build`; for adapter changes, repeat the server acceptance described in [docs/VERIFICATION.md](docs/VERIFICATION.md).
3. Do not commit `.cache/`, `.gradle/`, `build/`, game worlds, logs or server jars; they are ignored.
4. `dist/` is ignored and is a release artifact, not source. Maintainers regenerate it when cutting a release.
5. One concern per pull request; keep formatting or renaming in a separate change.

## Reporting

- Use the Bug Report template with the full startup log and the exact Fabric Loader / Fabric API / NeoForge versions.
- Security issues: do not open a public issue, follow [SECURITY.md](SECURITY.md).
- Usage questions: read the [README](README.md), [docs/API.md](docs/API.md), [docs/FABRIC.md](docs/FABRIC.md) and [docs/NEOFORGE.md](docs/NEOFORGE.md) first.

## License

By contributing you agree to license your work under [MIT](LICENSE).
