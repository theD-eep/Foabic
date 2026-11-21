# Foabic 1.0 verification

## Contract

On Minecraft 26.3, one minimal Foabic mod starts on Fabric and on NeoForge, registers one block and its block item, receives one signal, and produces identical Foabic observations when the run is repeated.

This is the release gate for 1.0. It is not a client-rendering test and not a full platform compatibility matrix.

## Environment

| Item | Value |
| --- | --- |
| Date | 2026-11-21 |
| OS | Windows 11 |
| Java | Eclipse Temurin 25.0.5.1+2 |
| Gradle | 9.5.1 (wrapper) |
| Minecraft | Java Edition 26.3 |
| Fabric Loader | 0.20.1 |
| Fabric API | 0.166.2+26.3 |
| NeoForge | 26.3.0.115 |
| Foabic | 1.0.0 |

## Build

```bat
gradlew.bat build
```

Core tests are part of `build` via the `:coreTest` task:

```text
PASS events
PASS failure isolation
PASS versions
PASS use-block signal
ALL TESTS PASSED (4)
```

Gradle artifacts:

| File | Bytes |
| --- | ---: |
| `foabic-api-1.0.0.jar` | 34,396 |
| `foabic-fabric-1.0.0.jar` | 69,385 |
| `foabic-neoforge-1.0.0.jar` | 70,592 |
| `foabic-example-minimal-1.0.0.jar` | 4,029 |

Checksums: `dist/SHA256SUMS`.

## Runtime procedure

The Gradle-built jars were installed into the prepared dedicated-server runtimes:

1. Fabric: Fabric API + `foabic-fabric-1.0.0.jar` in `mods/`, example in `foabic-mods/`.
2. NeoForge: `foabic-neoforge-1.0.0.jar` in `mods/`, example in `foabic-mods/`.
3. Start the dedicated server, wait for `Done (` and `FOABIC_SIGNAL`.
4. Stop the server.
5. Repeat from a fresh process.

The first run and the second run on each platform produced the three lines below. The Gradle-built jars are the ones measured above; no pre-Gradle jar was used.

## Evidence

Fabric: [verification/fabric.txt](verification/fabric.txt)

```text
FOABIC_REGISTER platform=fabric block=foabic_test:test_block item=foabic_test:test_block
FOABIC_SIGNAL name=server_started platform=fabric mc=26.3 mods=1
FOABIC_STATUS platform=fabric block_registered=true item_registered=true
```

NeoForge: [verification/neoforge.txt](verification/neoforge.txt)

```text
FOABIC_REGISTER platform=neoforge block=foabic_test:test_block item=foabic_test:test_block
FOABIC_SIGNAL name=server_started platform=neoforge mc=26.3 mods=1
FOABIC_STATUS platform=neoforge block_registered=true item_registered=true
```

Both runs on both platforms produced exactly these three lines.

## What this proves

- The Gradle-built Fabric jar starts, loads a Foabic mod, evaluates the block/item factories, registers both objects, and posts `ServerStartedSignal`.
- The Gradle-built NeoForge jar starts, loads the same mod jar, defers block/item construction to `RegisterEvent`, registers both objects, and posts the same signal.
- Registry lookups at signal time return both objects on both platforms.
- The signal bus, mod discovery, metadata parser, dependency check and classloader path all work in a real server process.
- The same platform-neutral example jar is used on both platforms.
- Repeating the process produces identical Foabic observations.

## What this does not prove

- **Client rendering, assets, or a real right-click.** The v1.0 acceptance contract uses `ServerStartedSignal` precisely so it does not depend on a client. The `UseBlockSignal` bridge compiles and is unit-tested, but it is not part of the server acceptance evidence.
- **Mixin, a build plugin for mod authors, universal packaging, or a full signal library.** These are v1.1/later by design.
- **Any Minecraft version other than 26.3, or any Fabric/NeoForge version other than the table above.**
- **Binary compatibility across future Foabic releases.**

The NeoForge test runtime emits Netty epoll/kqueue environment warnings on Windows because its installed classpath contains non-Windows native transports. They are platform noise, not Foabic load failures; the server still reaches `Done` and the Foabic assertions pass.
