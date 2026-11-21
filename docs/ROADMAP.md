# Roadmap

## 1.1 — current

Shipped and verified on Minecraft 26.3:

- defensive discovery: duplicate ids, broken metadata and unreadable jars are logged and skipped;
- dependency preflight with actionable errors for missing/incompatible `fabric-api` and `foabic-api`, including a specific Fabric-only diagnostic on NeoForge;
- platform-neutral `BLOCKS`, `ITEMS`, `BLOCK_ENTITY_TYPES` and `SOUND_EVENTS`;
- `BlockItem` linkage through `submitBlock`, including creative-tab insertion;
- block-entity binding through `submitBlockEntity`;
- `SoundEvent` registration;
- `PlayerInteractBlockSignal` (Fabric `UseBlockCallback`, NeoForge `RightClickBlock`);
- `PlayerBreakBlockSignal` (Fabric `PlayerBlockBreakEvents.BEFORE`, NeoForge `BreakBlockEvent`);
- `ServerTickSignal` (Fabric `END_SERVER_TICK`, NeoForge `ServerTickEvent.Post`);
- generated blockstate/block model/item model plus texture copy from `texture=` metadata or `context.resources()`;
- always-on generated resource packs on both adapters;
- Fabric and NeoForge dedicated-server verification, twice per platform, plus fake-player signal tests.

The v1.0 `UseBlockSignal` remains as a deprecated compatibility path.

## 1.2 — not committed

Candidates, in no promised order:

- a larger signal library (player join/leave, block place, entity events, server stopping);
- a small command and scheduling API that does not require live `MinecraftServer` objects in signals;
- broader block-entity helpers (ticker binding, menu providers);
- a compatibility matrix for another 26.3 patch release;
- data-pack resource helpers alongside the client resource pack.

## Later

- universal/cross-platform packaging;
- networking;
- GUI/screen API;
- a Gradle plugin for Foabic mod authors (explicitly not before 1.3);
- 26.4+ compatibility;
- binary compatibility commitments across 1.x.

## Non-goals for 1.1

- **Mixin.** No Mixin dependency, annotation processor or configuration.
- **A build plugin.** The adapters are configured manually in two modules.
- **Rendering-layer integration.** No Sodium/OpenGL code; the resource work is data generation only.
- replacing Fabric Loader, Fabric API or NeoForge;
- sandboxing mods;
- bundling Minecraft, Fabric API or NeoForge.

## Scope rule

Do not add a deferred subsystem to a release line before the lower-level acceptance path is verified on every supported platform. v1.1 exists to make the next layer debuggable, not to look complete.
