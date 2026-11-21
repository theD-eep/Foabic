# Changelog

## 1.1.0 — 2026-11-21

Foabic 1.1 expands the v1.0 minimal contract into a usable small-content API and hardens loading against the failure modes found during v1.0 testing. Mixin, a mod-author Gradle plugin and rendering-layer work remain explicitly out of scope.

### Added

- **Defensive discovery**
  - Duplicate mod ids are skipped with an `ERROR` line: `发现重复的 Mod ID: <id>，已跳过加载`.
  - Malformed `foabic.mod` files and unreadable jars are logged and skipped instead of aborting the whole load.
  - UTF-8 BOM in `foabic.mod` is accepted.
- **Dependency preflight**
  - Required `fabric-api`, `fabricloader`, `foabic-api`, `foabic`, `minecraft` and `neoforge` versions are checked before entrypoint classloading.
  - Missing platform-specific dependencies produce actionable messages (for example, Fabric API on NeoForge).
  - Undeclared missing classes are caught and reported as dependency problems with a real message instead of a bare `NoClassDefFoundError`.
- **Content registration**
  - `ModContext.submitBlock(id, blockFactory)` registers a block and its `BlockItem` under the same id.
  - Custom item factories and explicit `CreativeTab` selection.
  - Automatic insertion into vanilla creative tabs on Fabric and NeoForge.
  - `ModContext.submitBlockEntity(id, block, factory)` binds a `BlockEntityType` to one or more blocks.
  - `FoabicRegistries.BLOCK_ENTITY_TYPES` and `FoabicRegistries.SOUND_EVENTS`.
  - `BlockItemRegistration` and `BlockEntityRegistration` handles.
- **Signals**
  - `PlayerInteractBlockSignal` with mutable `PASS` / `SUCCESS` / `CONSUME` / `FAIL`.
  - `PlayerBreakBlockSignal`, cancellable.
  - `ServerTickSignal`.
  - Both adapters emit all three through their real platform hooks.
  - Fabric: `UseBlockCallback`, `PlayerBlockBreakEvents.BEFORE`, `ServerTickEvents.END_SERVER_TICK`.
  - NeoForge: `PlayerInteractEvent.RightClickBlock`, `BreakBlockEvent`, `ServerTickEvent.Post`.
- **Generated resources**
  - `texture=` in `foabic.mod` generates blockstate, cube-all block model and item model for every `submitBlock` block.
  - `context.resources().block(...)` is the one-line per-block override.
  - Textures are copied from Foabic mod jars; a visible fallback PNG is generated when a declared texture is missing.
  - Blocks with no declared texture receive a generated fallback model unless the mod already ships a custom blockstate.
  - The generated `.foabic/generated-resources` pack is registered as an always-on built-in pack:
    - Fabric: `ResourceLoader.registerBuiltinPack` with a synthetic mod root;
    - NeoForge: `AddPackFindersEvent` with a fixed `PathPackResources`.
- **Verification**
  - Final 1.1.0 jars started twice per platform with identical Foabic observations.
  - Fake-player harnesses verified real block-entity creation and both player signals on both adapters.
  - Duplicate-id runtime test proved the server continues.
  - Missing-dependency runtime test proved the failure is clear and occurs before classloading.
  - Generated pack files and texture SHA-256 compared across platforms.

### Changed from 1.0

- `Foabic.VERSION` is `1.1.0`; `Foabic.API_VERSION` is `2`.
- `UseBlockSignal` is deprecated in favor of `PlayerInteractBlockSignal`; the adapters still deliver the legacy signal when a listener is registered.
- `FoabicRegistries` now exposes four views instead of two.
- The example mod registers a block entity and a sound, uses generated assets, and observes tick/interact/break signals.
- Example Gradle projects now live under `examples/minimal` and `examples/chat`; the duplicate root `example/` and `example-chat/` project folders were removed.
- The loader module now compiles against Minecraft for its API signatures and defensive tests; it remains bundled into each adapter and is not a separate runtime install.
- Artifact names use `1.1.0`.

### Not included

- Mixin support.
- A Gradle/Maven plugin for Foabic mod authors.
- Universal packaging.
- A full signal standard library.
- Networking, GUI, data generation and a full resource/model parser.
- Rendering-layer integration.
- Client rendering tests.

### Verified

- Core tests: 8 pass groups, including duplicate/malformed discovery, dependency diagnostics and generated resources.
- Fabric 26.3 dedicated server: all four registries present; `FOABIC_TICK` at tick 20.
- NeoForge 26.3.0.115 dedicated server: same.
- Fake-player tests: `TestBlockEntity` created; `FOABIC_INTERACT` and `FOABIC_BREAK` emitted on both.
- Duplicate id: one ERROR, one loaded mod, server continues.
- Missing `fabric-api` range: one clear `依赖检查失败` line, no `NoClassDefFoundError`.
- Evidence: [docs/VERIFICATION.md](docs/VERIFICATION.md).

## 1.0.0 — 2026-11-21

First cross-platform Foabic release, scoped to a verified minimal contract on Minecraft 26.3.

### Added

- Minecraft 26.3 support; Java 25.
- Fabric Loader 0.20.1 / Fabric API 0.166.2+26.3 adapter.
- NeoForge 26.3.0.115 adapter.
- Platform-neutral `FoabicRegistries.BLOCKS` and `ITEMS`.
- Factory-based registration with `Registration<T>`:
  - Fabric evaluates immediately;
  - NeoForge evaluates in `RegisterEvent`, because 26.3 freezes registries before mod construction.
- `ServerStartedSignal`, the deterministic acceptance signal.
- `UseBlockSignal` with Fabric `UseBlockCallback` and NeoForge `PlayerInteractEvent.RightClickBlock` bridges.
- `ChatMessageSignal` with Fabric `ServerMessageEvents.CHAT_MESSAGE` and NeoForge `ServerChatEvent` bridges; `replyAs` adds a `<name> message` chat line.
- Minimal example mod registering one block and one block item.
- Chat example mod: on `aaawww123`, adds `<foabic> wwwaaa`.
- Foabic mods are also discovered from `mods/` as a fallback; `foabic-mods/` remains preferred.
- Byte-identical duplicate copies in both directories are ignored with a warning instead of crashing the game.
- Gradle 9.5.1 wrapper build:
  - `:fabric` uses the non-remapping `net.fabricmc.fabric-loom` 1.14.10 plugin;
  - `:neoforge` compiles against the official NeoForge 26.3.0.115 universal artifact with Gradle's `java` plugin;
  - `:coreTest` runs the platform-neutral tests.
- Repeatability check: two runs per platform of the Gradle-built jars, exact Foabic output comparison.

### Changed from 0.2

- `submit` now takes a `Supplier<? extends T>` and returns `Registration<T>`, not a pre-built value.
- `UseBlockSignal` is plain data (`playerName`, `dimension`) instead of live `Player`/`Level` wrappers.
- Commands, scheduler, config, resources and the old server event records were removed from the 1.0 surface.
- Fabric no longer scans `mods/`; Foabic mods live in `foabic-mods/`.
- Minecraft support moved from 26.1 to 26.3.

### Verified

- Fabric 26.3 dedicated server: `FOABIC_REGISTER`, `FOABIC_SIGNAL`, `FOABIC_STATUS block_registered=true item_registered=true`.
- NeoForge 26.3.0.115 dedicated server: same observations.
- Two runs per platform produce identical Foabic observations.
- Core tests: events, failure isolation, versions, use-block signal.
- Chat example: Fabric and NeoForge chat event bridges invoked via a temporary fake-player self-test; `FOABIC_CHAT_REPLY player=[Minecraft] text=wwwaaa` on both platforms.
- The chat output is a server system line formatted as `<foabic> wwwaaa`; it does not fabricate a signed player chat packet.

### Not included

- Mixin, a Gradle plugin for Foabic mod authors, universal packaging, a full signal library, networking, GUI, resources and data generation. See [docs/ROADMAP.md](docs/ROADMAP.md).

## 0.2.0 — 2026-11-21

Fabric-only beta for Minecraft 26.1: Fabric Loader 0.20.1 / Fabric API 0.145.1+26.1, `submit`, `connect`, `UseBlockSignal`, typed Fabric registries, example block/assets, real client/server verification.

## 0.1.0 — 2026-11-20

Initial standalone prototype. Not part of the 1.0 lineage.
