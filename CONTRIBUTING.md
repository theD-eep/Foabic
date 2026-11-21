# Contributing

Thanks for helping. Foabic is deliberately small: read the scope rules before writing code.

## Requirements

| Dependency | Version | Notes |
| --- | --- | --- |
| JDK | **25** | everything is compiled with `--release 25` |
| Minecraft server bundle | **26.1** | official `server.jar`, already unobfuscated |
| Fabric Loader | **0.20.1** | compile and runtime dependency |
| Fabric API | **0.145.1+26.1** | the build extracts its nested modules |

No Gradle, Maven, Loom, mappings or remap step. The build is `javac` + `jar` driven by `build.ps1`.

## Build and test

The build script is PowerShell and currently Windows-oriented:

```bat
build.cmd ^
  -JavaHome "C:\path\to\jdk-25" ^
  -McJar "C:\path\to\server.jar" ^
  -FabricLoaderJar "C:\path\to\fabric-loader-0.20.1.jar" ^
  -FabricApiJar "C:\path\to\fabric-api-0.145.1+26.1.jar"
```

The script extracts the Minecraft bundle and Fabric API modules, compiles the API, loader and Fabric adapter, packages the jars, then runs the core test suite. Expected tail:

```text
PASS versions
PASS events
PASS scheduler
PASS commands
ALL TESTS PASSED
```

`-SkipTests` skips the suite. Artifacts land in `dist/`:

```text
foabic-api-0.2.0.jar                  compile-time API
foabic-fabric-0.2.0.jar               Fabric mod (API + loader + adapter)
foabic-example-smoketest-0.2.0.jar    runnable block/signal example
```

The build never downloads anything; point it at jars you obtained yourself. The CI workflow downloads the exact versions above from Mojang/Fabric Maven on a Windows runner.

## Scope rules

Beta 0.2 accepts:

- fixes to the Fabric adapter, registry bridge, `submit`, `connect` and `UseBlockSignal`;
- loader, dependency resolution, event, command and scheduler fixes;
- documentation, tests and examples.

Do **not** add, on this branch: NeoForge, a signal standard library, a Gradle/Maven build plugin, Mixin, cross-platform packaging, networking, GUI APIs or model/creative-tab generation. They are deferred to 0.3 — see [docs/ROADMAP.md](docs/ROADMAP.md).

Architecture rule: `src/api` must not import Minecraft or Fabric classes. Platform code belongs in `src/fabric`.

## Style

- 4 spaces, UTF-8, no BOM; event types are immutable `record`s.
- No third-party runtime dependency beyond the JDK and the Fabric API the user installs.
- No classpath scanning or annotation reflection; the loader only reads `foabic.mod`.
- Keep the public API small. If a new public type is not needed by the example or a test, do not add it.
- Public API changes must update [docs/API.md](docs/API.md) and [CHANGELOG.md](CHANGELOG.md).

## Version numbers

The version appears in four places and must match:

1. `build.ps1` → `$Version`
2. `src/api/java/dev/foabic/api/Foabic.java` → `VERSION`
3. `src/fabric/resources/fabric.mod.json` → `"version"`
4. `CHANGELOG.md` → the section heading

Tags use a `v` prefix, for example `v0.2.0`; the release workflow rejects a tag that disagrees with `build.ps1`.

## Commits and pull requests

1. Fork the repository and branch from `main`: `fix/...`, `feat/...`, `docs/...`.
2. Run the full build and test suite.
3. Do not commit `build/`, `run/`, `client-run/`, `world/`, `logs/`, `server.jar` or any game file; they are ignored.
4. `dist/` jars are release artifacts kept in the repository for the beta. Maintainers regenerate them when cutting a release; a normal pull request should not include binary changes.
5. One concern per pull request; keep formatting or renaming in a separate change.

## Reporting

- Use the Bug Report template with the full startup log and the exact Fabric Loader / Fabric API versions.
- Security issues: do not open a public issue, follow [SECURITY.md](SECURITY.md).
- Usage questions: read the [README](README.md), [docs/API.md](docs/API.md) and [docs/FABRIC.md](docs/FABRIC.md) first.

## License

By contributing you agree to license your work under [MIT](LICENSE).
