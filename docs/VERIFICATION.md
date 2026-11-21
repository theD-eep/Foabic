# Foabic 1.2 verification

## Contract

On Minecraft 26.3, one platform-neutral Foabic mod starts on Fabric and on NeoForge, registers a block, block item, block-entity type and sound event, generates its block/item assets, reads a JSON config, registers a network packet, receives the startup/tick/interact/break signals, removes its own subscription, and leaves no signal listeners after server stop.

The same final 1.2 jars were started twice per platform. The Foabic Loom demo jars were also loaded once on each platform.

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
| Foabic | 1.2.0 |

## Build

```bat
gradlew.bat clean build
```

Result:

```text
FOABIC_API_SIZE bytes=14420 limit=15360
...
ALL TESTS PASSED (9)
BUILD SUCCESSFUL
```

Core tests cover:

- event dispatch, unsubscribe and clear;
- listener failure isolation;
- version/range parsing including `26.3.0.115`;
- legacy and v1.2 signal data classes;
- network encode/decode/dispatch with a fake transport;
- duplicate-id and malformed-metadata handling;
- JSON `foabic.mod.json` parsing, including entrypoint, dependencies, texture and icon;
- missing Fabric API diagnostics and provided-version resolution;
- registry-freeze exception and exact ERROR line;
- config POJO/record save and reload;
- generated blockstate/model/texture copy and fallback.

Artifacts:

| File | Bytes |
| --- | ---: |
| `foabic-api-1.2.0.jar` | 14,420 |
| `foabic-content-1.2.0.jar` | 32,219 |
| `foabic-fabric-1.2.0.jar` | 124,592 |
| `foabic-neoforge-1.2.0.jar` | 124,183 |
| `foabic-example-minimal-1.2.0.jar` | 10,344 |
| `foabic-example-chat-1.2.0.jar` | 2,786 |

The core jar is under the 15 KiB (15,360 byte) limit. `verifyApiContract` also scans `src/api` and `src/content`; no `net.fabricmc.*` or `net.neoforged.*` import is present.

## Runtime procedure

The Gradle-built jars were installed into prepared dedicated-server runtimes:

1. Fabric: Fabric API + `foabic-fabric-1.2.0.jar` in `mods/`, the example in `foabic-mods/`.
2. NeoForge: `foabic-neoforge-1.2.0.jar` in `mods/`, the example in `foabic-mods/`.
3. Start the server, wait for `Done (`.
4. Send `stop` through the server's stdin and wait for exit code 0.
5. Repeat from a fresh JVM.

This exercises the real server-stopped callback and the `FOABIC_STOP_CLEAN` cleanup line, not a forced process kill.

## Evidence

The two final Fabric runs produced the same Foabic observations:

```text
FOABIC_CONFIG type=example.MinimalMod$TestConfig file=...\config\foabic\foabic_test\TestConfig.json
FOABIC_REGISTER platform=fabric block=foabic_test:test_block item=foabic_test:test_block
FOABIC_V12_REGISTER platform=fabric block_entity=foabic_test:test_block_entity sound=foabic_test:test_sound
FOABIC_CONFIG platform=fabric greeting=hello-v1.2
FOABIC_NETWORK platform=fabric packet=example.MinimalMod$StatusPacket
FOABIC_READY platform=fabric sound=foabic_test:test_sound
FOABIC_RESOURCES files=5 textures=1 root=...\.foabic\generated-resources
FOABIC_SIGNAL name=server_started platform=fabric mc=26.3 mods=1
FOABIC_STATUS platform=fabric block_registered=true item_registered=true block_entity_registered=true sound_registered=true
严重: [Foabic] 模组 foabic_test 试图在注册表冻结后注册物品 foabic_test:late_item，已被拦截。
FOABIC_FROZEN_GUARD platform=fabric caught=true object=foabic_test:late_item
FOABIC_TICK platform=fabric tick=20 players=0
FOABIC_UNSUBSCRIBE platform=fabric tick=40
FOABIC_STOP_CLEAN platform=fabric tick_listeners=false
```

The two final NeoForge runs produced the same set with `platform=neoforge`:

```text
FOABIC_CONFIG type=example.MinimalMod$TestConfig file=...\config\foabic\foabic_test\TestConfig.json
FOABIC_REGISTER platform=neoforge block=foabic_test:test_block item=foabic_test:test_block
FOABIC_V12_REGISTER platform=neoforge block_entity=foabic_test:test_block_entity sound=foabic_test:test_sound
FOABIC_CONFIG platform=neoforge greeting=hello-v1.2
FOABIC_NETWORK platform=neoforge packet=example.MinimalMod$StatusPacket
FOABIC_READY platform=neoforge sound=foabic_test:test_sound
FOABIC_RESOURCES files=5 textures=1 root=...\.foabic\generated-resources
FOABIC_SIGNAL name=server_started platform=neoforge mc=26.3 mods=1
FOABIC_STATUS platform=neoforge block_registered=true item_registered=true block_entity_registered=true sound_registered=true
严重: [Foabic] 模组 foabic_test 试图在注册表冻结后注册物品 foabic_test:late_item，已被拦截。
FOABIC_FROZEN_GUARD platform=neoforge caught=true object=foabic_test:late_item
FOABIC_TICK platform=neoforge tick=20 players=0
FOABIC_UNSUBSCRIBE platform=neoforge tick=40
FOABIC_STOP_CLEAN platform=neoforge tick_listeners=false
```

Full logs:

- [Fabric](verification/v12-fabric.txt)
- [NeoForge](verification/v12-neoforge.txt)

## Config evidence

Both servers created:

```text
config/foabic/foabic_test/TestConfig.json
```

```json
{
  "greeting": "hello-v1.2",
  "exampleNumber": 42
}
```

The second run read the same file. The unit test additionally mutates a POJO, calls `save`, recreates the manager, and verifies the values; it also saves and reloads a record.

## Subscription cleanup evidence

`MinimalMod` unsubscribes its own tick listener at tick 40:

```text
FOABIC_UNSUBSCRIBE ... tick=40
```

The adapter's server-stopped callback then clears the session bus and logs:

```text
FOABIC_STOP_CLEAN platform=fabric|neoforge tick_listeners=false
```

The false value is read after `FoabicInternal.onServerStopped()`, proving the listener was removed by the lifecycle hook, not merely by process termination.

## Registry-freeze evidence

At `ServerStartedSignal`, the example deliberately submits a late item:

```text
严重: [Foabic] 模组 foabic_test 试图在注册表冻结后注册物品 foabic_test:late_item，已被拦截。
FOABIC_FROZEN_GUARD ... caught=true object=foabic_test:late_item
```

The server continues running and exits normally. `FoabicRegistryFrozenException` is caught by the test mod; the adapter does not crash.

## Foabic Loom evidence

`examples/plugin-demo` uses one source tree and one `foabic.mod.json`. `gradlew.bat :examples:plugin-demo:build` produced:

```text
examples/plugin-demo/dist/mod-fabric.jar
examples/plugin-demo/dist/mod-neoforge.jar
```

Both jars were copied into the normal `foabic-mods/` directory (one at a time)
and loaded by the final adapters. The NeoForge jar was also placed in the
platform `mods/` directory to verify that FML accepts the generated
`neoforge.mods.toml`; it started and was discovered by the Foabic loader. The
Fabric jar was tested the same way in Fabric `mods/`.

```text
信息: initializing foabic_plugin_demo 1.2.0
信息: FOABIC_PLUGIN_READY platform=fabric
信息: FOABIC_PLUGIN_DEMO platform=fabric mod=foabic_plugin_demo
信息: FOABIC_STOP_CLEAN platform=fabric tick_listeners=false

信息: initializing foabic_plugin_demo 1.2.0
信息: FOABIC_PLUGIN_READY platform=neoforge
信息: FOABIC_PLUGIN_DEMO platform=neoforge mod=foabic_plugin_demo
信息: FOABIC_STOP_CLEAN platform=neoforge tick_listeners=false
```

The generated platform metadata was inspected directly in both jars. The Fabric jar contains `fabric.mod.json`; the NeoForge jar contains `META-INF/neoforge.mods.toml`; both contain the canonical `foabic.mod.json` and the embedded custom metadata.

## Launcher icon evidence

The feather pixel PNG is 978 bytes (a 16×16 sprite scaled 16× with nearest-neighbour sampling) and is packaged at `icon.png` in both adapter jars and in the minimal example.

Fabric adapter metadata:

```json
"icon": "icon.png"
```

NeoForge adapter metadata:

```toml
[[mods]]
modId="foabic"
logoFile="icon.png"
```

The Foabic Loom demo uses an explicit `icon` path:

```json
"icon": "assets/foabic_plugin_demo/icon.png"
```

The plugin generated:

```text
fabric.mod.json:  "icon": "assets/foabic_plugin_demo/icon.png"
neoforge.mods.toml: logoFile="assets/foabic_plugin_demo/icon.png"
foabic.mod.json:  "icon": "assets/foabic_plugin_demo/icon.png"
```

Both plugin jars contain the icon at that path and load from `foabic-mods/`
and the standard platform `mods/` directory. The plugin also supports automatic
detection of `src/main/resources/icon.png` when `icon` is omitted; that fallback
was exercised during development. The icon itself was not visually inspected in
HMCL/PCL2 or a Minecraft client.

## Generated resource pack

Both servers generated the same five files:

```text
pack.mcmeta
assets/foabic_test/blockstates/test_block.json
assets/foabic_test/models/block/test_block.json
assets/foabic_test/models/item/test_block.json
assets/foabic_test/textures/block/test_block.png
```

The PNG is copied from the example jar; the blockstate points at `foabic_test:block/test_block`. The generated pack path is `.foabic/generated-resources`.

## What this proves

- The final Gradle-built platform jars load the same JSON-metadata example and register all four content types.
- The registry-freeze guard catches a late submission on both platforms and logs the required line.
- The adapter's server-stopped hook clears session listeners on both platforms.
- The config file is created, read and logged on both platforms.
- Packet registration succeeds on both adapters without client-only classloading errors on a dedicated server.
- The Foabic Loom demo jars are loadable by the normal Foabic loader on both platforms.
- Icon metadata is present in both adapter jars and in plugin-generated platform metadata, and the feather icon is packaged as `icon.png`.
- The core API is 14,420 bytes and contains no platform imports.
- Two final runs per platform exit 0 with the same Foabic observations.

## What this does not prove

- **A live client network round-trip.** Packet encode/decode/dispatch is covered by a fake-transport unit test, and both adapters register their payload handlers successfully. No graphical client or second connected client was launched, so an actual socket send/receive was not exercised.
- **A rendered client.** No graphical client was launched; the generated resource pack was not visually inspected.
- **A launcher icon visual.** HMCL/PCL2 and a Minecraft client were not launched, so the feather icon's rendering was not visually captured; only metadata and packaged bytes were verified.
- **Creative-tab UI.** The server has no creative inventory.
- **Audio playback.** No `.ogg`/`sounds.json` assets are shipped by the example.
- **Mixin, GUI, universal packaging.** Explicitly out of scope.
- **Any Minecraft version other than 26.3, or any Fabric/NeoForge version other than the table above.**
- **Binary compatibility across Foabic 1.x.**

The NeoForge runtime emits Netty epoll/kqueue Windows warnings. They are environment noise, not Foabic load failures; both runs reached `Done` and exited 0.
