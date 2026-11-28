# Roadmap

## 1.3 — current

Shipped and verified on Minecraft 26.3:

- block-entity base with saved, client-synchronised `SyncedInt`, `SyncedLong`,
  `SyncedFloat`, `SyncedBoolean` and `SyncedString` fields;
- entity-type registration and a `FoabicEntity` base over vanilla
  `SynchedEntityData`;
- `ServerStartedSignal.server()` for post-load recipe/tag/content checks;
- a fluent data-generation API for blockstates, models, shaped/shapeless/cooking
  recipes, item/block/entity tags, block loot tables, language files and raw
  JSON;
- build-time `foabicData` in Foabic Loom and a runtime
  `.foabic/generated-data` pack on both platforms;
- generated packs registered for both client resources and server data;
- 26.3 `min_format`/`max_format` pack metadata;
- a survival example with a ticking block entity, sword item, custom entity
  type, recipes, tags, loot and two languages;
- dedicated-server evidence that the recipe and `minecraft:swords` tag actually
  load on Fabric and NeoForge.

## 1.4 candidates

Not committed:

- an inventory/menu helper for block entities (`Container`, `MenuProvider`,
  item-handler bridge);
- entity attributes, spawn placement, spawn eggs and an optional renderer
  registration seam;
- a small command API and a larger signal library;
- recipe-display/advancement helpers on top of `raw()`;
- client datapack/resource reload helpers;
- a second 26.3 patch-release compatibility matrix.

## Later

- universal/cross-platform packaging;
- GUI/screen API;
- custom entity renderer abstraction;
- 26.4+ compatibility;
- binary compatibility commitments across 1.x.

## Non-goals for 1.3

- **Mixin.** No Mixin dependency, annotation processor or configuration.
- **Rendering-layer integration.** No Sodium/OpenGL code.
- **Custom entity rendering.** Foabic registers and syncs the entity; the client
  renderer stays with the mod.
- replacing Fabric Loader, Fabric API or NeoForge;
- sandboxing mods;
- bundling Minecraft, Fabric API or NeoForge.

## Scope rule

Do not add a deferred subsystem to a release line before the lower-level
acceptance path is verified on every supported platform. v1.3 exists to make
survival content debuggable, not to look complete.
