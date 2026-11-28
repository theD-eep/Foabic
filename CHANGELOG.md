# Changelog

## 1.3.0 — 2026-11-28

Foabic 1.3 makes a Foabic mod playable as a survival mod: block entities and
entities can store state and synchronise it, recipes/tags/loot/models/language
are generated from code, and the generated data is loaded as an always-on data
pack on both platforms. Mixin and rendering-layer work remain out of scope.

### Added

- **Block entities with data sync**
  - `FoabicBlockEntity` with `syncedInt`, `syncedLong`, `syncedFloat`,
    `syncedBoolean` and `syncedString`.
  - Fields save through the vanilla `ValueInput`/`ValueOutput` APIs, mark the
    block entity dirty, and send a `ClientboundBlockEntityDataPacket` update.
  - `getUpdateTag`/`getUpdatePacket` are implemented; chunk-load sync works.
  - `FoabicBlock` supplies a ticker that calls `tick()` on both sides.
- **Entities with vanilla sync**
  - `FoabicRegistries.ENTITY_TYPES` and `ModContext.submitEntity`.
  - `FoabicEntity` base implementing `Entity`'s abstract methods.
  - `define`, `setSynced` and `synced` helpers over `SynchedEntityData`.
  - `ServerStartedSignal.server()` gives access to the live `MinecraftServer`.
- **Data generation**
  - `FoabicDataProvider` / `FoabicDataContext`.
  - `blockStates(...)` helpers: `simple`, `cubeColumn`, `cross`, `orientable`,
    `variant`, `model`, `itemModel`.
  - `recipe(...)`: shaped, shapeless, smelting, blasting, smoking, campfire,
    stonecutting, with validation.
  - `tag(...)`: item, block, entity type, fluid, game event, biome, structure,
    enchantment, recipe, function, damage type; merging, optional entries and
    `replace()`.
  - `lootTable(...)`: `dropsSelf`, fixed/uniform drops and `noDrop`.
  - `lang(...)` and `raw(path, json)`.
  - Build-time `foabicData` Gradle task; output is included in both platform
    jars by `processResources`.
  - Runtime `.foabic/generated-data` data pack, including `data/` entries from
    Foabic mod jars.
  - Fabric `ResourceLoader.registerBuiltinPack` and NeoForge
    `AddPackFindersEvent` now register both client resources and server data.
  - `pack.mcmeta` uses 26.3 `min_format`/`max_format` (resource 97.1, data 121)
    instead of the legacy `pack_format` field.
- **Survival example**
  - `examples/survival`: feather collector block entity, feather sword item,
    custom feather wisp entity type, recipes, tags, loot table, block/item
    models and `en_us`/`zh_cn` language.
  - One source tree builds `dist/mod-fabric.jar` and `dist/mod-neoforge.jar`.
- **Tests**
  - Data generation file/path/JSON tests, tag merging and optional entries.
  - Dedicated-server checks for recipe loading, sword tag membership and all
    content registrations.

### Changed from 1.2

- `Foabic.VERSION` is `1.3.0`; `Foabic.API_VERSION` is `4`.
- `ModContext.data()` is a new required method; the loader implements it.
- `ModMetadata` adds `dataProviders`; `foabic.mod.json` accepts `data` (one
  class name or an array).
- Fabric generated packs now use an empty subpath instead of the implicit
  `resourcepacks/<id>` path, so the same root is visible for resources and data.
- NeoForge `AddPackFindersEvent` handles `SERVER_DATA` as well as
  `CLIENT_RESOURCES`.
- The API size grew by one byte (14,421) because of the version constant and
  signal constructor; it remains under 15 KiB.
- Platform jars are larger because they bundle the data, block and entity API.

### Not included

- Mixin.
- A universal/merged jar.
- GUI/screen API.
- Client renderers for custom entities.
- Worldgen/structure/advancement builders.
- A live client networking/rendering harness.

### Verified

- Fabric 26.3 dedicated server: `recipe_loaded=true sword_tag=true`, block
  entity/item/entity present, data pack registered, clean stop.
- NeoForge 26.3.0.115 dedicated server: same checks; deferred registration is
  visible after `RegisterEvent`.
- `foabic-api` 14,421 bytes; no platform imports.
- `foabicData` generates 11 files for the survival example (5 data, 6 assets).
- Evidence: [docs/VERIFICATION.md](docs/VERIFICATION.md).

## 1.2.0 — 2026-11-21

Foabic 1.2 hardens the runtime and adds the build, network and config rails a real mod needs. Mixin and rendering-layer work remain out of scope.

### Added

- **Signal lifecycle**
  - `FoabicSubscription` with idempotent `unsubscribe()`.
  - `EventBus.subscribe` and `ModContext.connect` return the handle.
  - Fabric `ServerLifecycleEvents.SERVER_STOPPED` and NeoForge `ServerStoppedEvent` call `FoabicInternal.onServerStopped()`.
  - `EventBus.clear()` releases listener callbacks; adapters log `FOABIC_STOP_CLEAN ... tick_listeners=false`.
- **Registry freeze protection**
  - `FoabicRegistryFrozenException` with `modId()` and `objectId()`.
  - Exact ERROR line: `[Foabic] 模组 X 试图在注册表冻结后注册物品 Y，已被拦截。`
  - Guards in both `ModContext.submit` and the `FoabicRegistries` proxy.
  - Fabric freezes after mod initialization; NeoForge freezes at `FMLCommonSetupEvent`.
- **Core API split**
  - `foabic-api` is now a 14,420-byte core jar with no Fabric/NeoForge imports.
  - `foabic-content` holds Minecraft-facing signals, registry views and content helpers.
  - Platform jars bundle both; runtime install remains one jar.
  - `:api:check` enforces < 15 KiB and the import rule.
- **`foabic.mod.json`**
  - Canonical JSON metadata, read by the loader and generated by the build plugin.
  - Legacy `foabic.mod` properties remain accepted for migration.
  - JSON `depends` / `optionalDepends` objects.
- **Foabic Loom Gradle plugin**
  - Included build at `foabic-gradle-plugin/`, id `dev.foabic.loom`.
  - `foabic { minecraftVersion = "26.3" }`.
  - One main source set produces `dist/mod-fabric.jar` and `dist/mod-neoforge.jar`.
  - Generates `fabric.mod.json` and `META-INF/neoforge.mods.toml`, embeds the canonical metadata.
  - Demo at `examples/plugin-demo`.
- **Networking**
  - `FoabicPacket` with `encode`, `decode` and optional `handle`.
  - `NetworkManager.register`, `sendToServer`, `sendToClient`, `receive`.
  - Fabric: play payload registry + `ServerPlayNetworking` / `ClientPlayNetworking`.
  - NeoForge 26.3: `RegisterPayloadHandlersEvent` + `PayloadRegistrar` (the supported replacement for the removed `SimpleChannel` API).
  - 1 MiB packet limit and unknown-packet logging.
- **Config**
  - `ConfigManager.register(Class<T>)` and `save(T)`.
  - Gson-backed JSON at `config/foabic/<mod-id>/<Type>.json`.
  - Records and no-arg POJOs; malformed files are backed up and regenerated.
- **Launcher icons**
  - New optional `icon` field in `foabic.mod.json`.
  - Foabic Loom auto-detects `src/main/resources/icon.png` or `assets/<id>/icon.png`, validates the file, packages it, and injects Fabric `icon` / NeoForge `logoFile`.
  - The Foabic adapter ships the Minecraft-style feather pixel logo; the plugin demo uses an explicit `assets/<id>/icon.png` path.
  - Documentation explains that launchers such as HMCL/PCL2 only read standard platform metadata from `mods/`; `foabic-mods/` alone is not scanned by them.
  - Launcher support is documented accurately: HMCL 3.9.2+ reads the metadata icon, while PCL2 only shows the online Modrinth/CurseForge project logo and never a local mod-jar icon. A per-instance icon installer for both launchers is provided in `tools/`.
- **Tests and evidence**
  - 9 test groups, including JSON metadata, config POJO/record reload, packet round-trip and freeze interception.
  - Two final dedicated-server runs per platform with config, packet registration, freeze guard, unsubscribe and `FOABIC_STOP_CLEAN`.
  - Plugin-generated jars loaded on both platforms.

### Changed from 1.1

- `Foabic.VERSION` is `1.2.0`; `Foabic.API_VERSION` is `3`.
- The core API no longer contains `dev.foabic.api.version`, `Environment`, `ModMetadata`, `Dependency`, registry or signal classes; those are in `foabic-content`. This is a source/binary break for code that imported them from the core jar.
- `ModContainer` exposes id/name/version/description/authors/jar/dataDirectory/logger/classLoader; environment and parsed metadata remain available through the loader implementation and `ModContext`.
- `EventBus` has no priority API; dispatch is insertion-order.
- `ModContext.connect` now returns `FoabicSubscription`, not `EventSubscription`.
- `foabic.mod` examples were replaced by `foabic.mod.json`.
- Platform jars increased in size because they now bundle the content API, config and network bridges.

### Not included

- Mixin.
- A universal/merged jar.
- GUI/screen API.
- Rendering-layer integration.
- A live client network/rendering/audio harness.

### Verified

- Fabric 26.3 dedicated server: two runs, all content, config, packet registration, freeze guard, unsubscribe and stop cleanup.
- NeoForge 26.3.0.115 dedicated server: same.
- `foabic-api` 14,420 bytes; no platform imports.
- Foabic Loom demo jars loaded on both adapters.
- Evidence: [docs/VERIFICATION.md](docs/VERIFICATION.md).

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
- **Content registration**
  - `ModContext.submitBlock(id, blockFactory)` registers a block and its `BlockItem` under the same id.
  - Custom item factories and explicit `CreativeTab` selection.
  - Automatic insertion into vanilla creative tabs on Fabric and NeoForge.
  - `ModContext.submitBlockEntity(id, block, factory)` binds a `BlockEntityType` to one or more blocks.
  - `FoabicRegistries.BLOCK_ENTITY_TYPES` and `SOUND_EVENTS`.
- **Signals**
  - `PlayerInteractBlockSignal`, `PlayerBreakBlockSignal`, `ServerTickSignal`, emitted by both adapters.
- **Generated resources**
  - `texture=` metadata generates blockstate, cube-all block model and item model.
  - `context.resources().block(...)` override.
  - Fallback texture and always-on built-in pack on both adapters.
- **Verification**
  - Final 1.1.0 jars started twice per platform.
  - Fake-player harness verified block-entity creation and both player signals.
  - Duplicate-id and missing-dependency runtime tests.

### Changed from 1.0

- `Foabic.VERSION` is `1.1.0`; `Foabic.API_VERSION` is `2`.
- `UseBlockSignal` is deprecated in favor of `PlayerInteractBlockSignal`.
- `FoabicRegistries` exposes four views.
- Examples live under `examples/minimal` and `examples/chat`.

### Not included

- Mixin support.
- A Gradle/Maven plugin for Foabic mod authors.
- Universal packaging.
- Networking, GUI, data generation and a full resource/model parser.
- Rendering-layer integration.

## 1.0.0 — 2026-11-21

First cross-platform Foabic release, scoped to a verified minimal contract on Minecraft 26.3.

### Added

- Minecraft 26.3 support; Java 25.
- Fabric Loader 0.20.1 / Fabric API 0.166.2+26.3 adapter.
- NeoForge 26.3.0.115 adapter.
- Platform-neutral `FoabicRegistries.BLOCKS` and `ITEMS`.
- Factory-based registration with `Registration<T>`.
- `ServerStartedSignal`, `UseBlockSignal`, `ChatMessageSignal`.
- Minimal example mod and chat example.
- Foabic mod discovery from `foabic-mods/`, with `mods/` fallback.
- Gradle 9.5.1 wrapper build and two runs per platform.

## 0.2.0 — 2026-11-21

Fabric-only beta for Minecraft 26.1: Fabric Loader 0.20.1 / Fabric API 0.145.1+26.1, `submit`, `connect`, `UseBlockSignal`, typed Fabric registries, example block/assets, real client/server verification.

## 0.1.0 — 2026-11-20

Initial standalone prototype. Not part of the 1.0 lineage.
