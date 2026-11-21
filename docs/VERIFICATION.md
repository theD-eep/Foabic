# Foabic 1.1 verification

## Contract

On Minecraft 26.3, one platform-neutral Foabic mod starts on Fabric and on NeoForge, registers a block, block item, block-entity type and sound event, generates its block/item assets, receives the startup, tick, interact and break signals, and produces identical Foabic observations when the run is repeated.

The mod is also tested with:
- duplicate mod ids, to prove discovery skips instead of crashing;
- a missing/incompatible `fabric-api` dependency, to prove the failure is clear and happens before classloading;
- a fake player on each platform, to exercise the real block-entity creation and interaction bridges.

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
| Foabic | 1.1.0 |

## Build

```bat
gradlew.bat build
```

Core tests are part of `build` via `:coreTest`:

```text
PASS events
PASS failure isolation
PASS versions
PASS legacy use-block signal
PASS player-interact-block signal
PASS player-break-block signal
PASS server-tick signal
PASS loader defenses
ALL TESTS PASSED (8)
```

The loader-defenses test covers:
- duplicate ids (first jar retained, duplicate skipped, ERROR captured);
- malformed metadata (skipped, ERROR captured);
- missing Fabric API on NeoForge (clear `DependencyException`);
- provided Fabric API version (resolved);
- generated blockstate/model/texture copy;
- missing-texture fallback.

Gradle artifacts:

| File | Bytes |
| --- | ---: |
| `foabic-api-1.1.0.jar` | 50,087 |
| `foabic-fabric-1.1.0.jar` | 108,206 |
| `foabic-neoforge-1.1.0.jar` | 108,724 |
| `foabic-example-minimal-1.1.0.jar` | 7,568 |
| `foabic-example-chat-1.1.0.jar` | 2,707 |

Checksums: `dist/SHA256SUMS`.

## Runtime procedure

The Gradle-built jars were installed into prepared dedicated-server runtimes:

1. Fabric: Fabric API + `foabic-fabric-1.1.0.jar` in `mods/`, the example in `foabic-mods/`.
2. NeoForge: `foabic-neoforge-1.1.0.jar` in `mods/`, the example in `foabic-mods/`.
3. Start the server, wait for `Done (`, `FOABIC_SIGNAL` and `FOABIC_TICK`.
4. Stop the process; repeat from a fresh JVM.

The same example jar is used on both platforms. It was started twice per platform with the final built jars.

## Evidence

Evidence index: [verification/README.md](verification/README.md).

Fabric: [verification/v11-fabric.txt](verification/v11-fabric.txt)

```text
FOABIC_REGISTER platform=fabric block=foabic_test:test_block item=foabic_test:test_block
FOABIC_V11_REGISTER platform=fabric block_entity=foabic_test:test_block_entity sound=foabic_test:test_sound
FOABIC_READY platform=fabric sound=foabic_test:test_sound
FOABIC_SIGNAL name=server_started platform=fabric mc=26.3 mods=1
FOABIC_STATUS platform=fabric block_registered=true item_registered=true block_entity_registered=true sound_registered=true
FOABIC_TICK platform=fabric tick=20 players=0
```

NeoForge: [verification/v11-neoforge.txt](verification/v11-neoforge.txt)

```text
FOABIC_REGISTER platform=neoforge block=foabic_test:test_block item=foabic_test:test_block
FOABIC_V11_REGISTER platform=neoforge block_entity=foabic_test:test_block_entity sound=foabic_test:test_sound
FOABIC_READY platform=neoforge sound=foabic_test:test_sound
FOABIC_SIGNAL name=server_started platform=neoforge mc=26.3 mods=1
FOABIC_STATUS platform=neoforge block_registered=true item_registered=true block_entity_registered=true sound_registered=true
FOABIC_TICK platform=neoforge tick=20 players=0
```

Both runs on both platforms produced the same Foabic lines (excluding per-run timing/paths).

## New-signal and block-entity evidence

A temporary platform-specific test harness (not shipped) placed the Foabic test block with a fake player and invoked the real platform callbacks/events.

Fabric:

```text
SELFTEST_BLOCK_ENTITY=example.MinimalMod$TestBlockEntity
SELFTEST_INTERACT=Success[swingSource=PREDICTED, itemContext=...]
SELFTEST_BREAK_ALLOWED=true
FOABIC_INTERACT player=[Minecraft] block=foabic_test:test_block pos=0,100,0 hand=MAIN_HAND
FOABIC_BREAK player=[Minecraft] block=foabic_test:test_block pos=0,100,0
```

NeoForge:

```text
SELFTEST_BLOCK_ENTITY=example.MinimalMod$TestBlockEntity
SELFTEST_INTERACT canceled=true result=Success[swingSource=PREDICTED, itemContext=...]
SELFTEST_BREAK canceled=false
FOABIC_INTERACT player=[Minecraft] block=foabic_test:test_block pos=0,100,0 hand=MAIN_HAND
FOABIC_BREAK player=[Minecraft] block=foabic_test:test_block pos=0,100,0
```

The NeoForge `canceled=true` is the expected mapping for `PlayerInteractBlockSignal.Result.SUCCESS`; the break signal did not cancel.

## Defensive-loading evidence

### Duplicate id

A byte-identical copy of the example jar was added under a second filename. The server continued to start, logged one ERROR, and loaded exactly one copy:

```text
严重: 发现重复的 Mod ID: foabic_test，已跳过加载 (...\zzz-duplicate.jar); 已加载自 ...\foabic-example-minimal-1.1.0.jar
信息: Foabic loaded 1 mod(s) in 72 ms on fabric
信息: FOABIC_STATUS platform=fabric block_registered=true item_registered=true block_entity_registered=true sound_registered=true
```

No exception was thrown.

### Missing/incompatible dependency

A temporary Foabic mod declared `depends=fabric-api@>=99.0.0`; the installed Fabric API was `0.166.2+26.3`. The loader stopped before classloading:

```text
严重: Foabic 依赖检查失败，启动中止: mod 'needs_new_fabric' requires 'fabric-api' >=99.0.0, but Fabric API provides 0.166.2. Install a matching version.
```

No `NoClassDefFoundError` was involved.

## Generated resource pack

Both servers generated the same five files:

```text
pack.mcmeta                                                      94 bytes
assets/foabic_test/blockstates/test_block.json                   59 bytes
assets/foabic_test/models/block/test_block.json                  88 bytes
assets/foabic_test/models/item/test_block.json                   42 bytes
assets/foabic_test/textures/block/test_block.png                127 bytes
```

The PNG SHA-256 matches the source texture:

```text
03D7595C5B72EB37E95729202706AFB6B8C6367E0356290BCA9372023A82B1B0
```

Fabric registration uses `ResourceLoader.registerBuiltinPack` with a synthetic generated-mod root. NeoForge uses `AddPackFindersEvent` and a fixed `PathPackResources` with `PackSource.BUILT_IN`, required and top position. Registration is client-only; the dedicated servers verify generation, not the client resource-reload path.

## What this proves

- The final Gradle-built Fabric jar loads the same v1.1 example jar, registers all four content types, evaluates deferred factories, posts `ServerStartedSignal` and `ServerTickSignal`, and generates assets.
- The final Gradle-built NeoForge jar does the same through `RegisterEvent`, including the block-entity type after the block registration.
- Block-entity creation is real: a fake player placed the block and `getBlockEntity` returned `TestBlockEntity`.
- `PlayerInteractBlockSignal` and `PlayerBreakBlockSignal` are emitted by both adapters; the Fabric result and NeoForge cancellation mapping are correct.
- Duplicate ids are skipped with an ERROR, without a crash.
- Missing/incompatible dependencies fail before entrypoint classloading with an actionable message.
- The generated blockstate, block model, item model and PNG are valid files and match on both platforms.
- Repeating the startup produces identical Foabic observations.

## What this does not prove

- **A rendered client.** No graphical client was launched. The resource pack was not visually inspected in-game; its generated files and namespace discovery were verified, but the client resource-reload path was not executed. This is not a rendering-layer test.
- **Creative-tab UI.** Creative-tab insertion is implemented and compiles, but the dedicated-server test has no creative inventory. A client is required to inspect the tab visually.
- **Actual audio playback.** The custom sound event is registered, but no `.ogg`/`sounds.json` asset was supplied by the example and no client was launched.
- **Mixin, a Foabic Gradle plugin, universal packaging, networking, GUI or data generation.** These are explicitly outside 1.1.
- **Any Minecraft version other than 26.3, or any Fabric/NeoForge version other than the table above.**
- **Binary compatibility across future Foabic releases.**

The NeoForge runtime emits Netty epoll/kqueue Windows environment warnings. They are runtime noise, not Foabic load failures; the server still reaches `Done` and the Foabic assertions pass.
