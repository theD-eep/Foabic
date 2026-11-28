# Foabic survival example

A playable Foabic 1.3 mod built from one source tree by Foabic Loom. It
demonstrates the v1.3 content path rather than only registration:

- `feather_collector` — a `FoabicBlock` with a ticking `FoabicBlockEntity`;
  the entity stores a feather counter and synchronises it to clients every ten
  seconds;
- `feather_blade` — a real sword item using `Item.Properties.sword`, in the
  `#minecraft:swords` item tag;
- `feather_wisp` — a custom entity type with a synced integer;
- shaped recipes for the collector and the blade;
- a block loot table and `minecraft:mineable/axe` block tag;
- generated blockstate, block/item models, `en_us` and `zh_cn` language;
- 16×16 block/item textures and the Foabic feather icon.

## Build

From the repository root:

```bat
gradlew.bat :examples:survival:build
```

Outputs:

```text
examples/survival/dist/mod-fabric.jar
examples/survival/dist/mod-neoforge.jar
```

`foabicData` runs before `processResources` and prints:

```text
FOABIC_DATAGEN mod=foabic_survival providers=1 files=11 data=5 assets=6
```

The generated JSON is in `build/foabic-data`; the textures and icon are normal
resources under `src/main/resources`.

## Install

Fabric:

```text
mods/
  fabric-api-0.166.2+26.3.jar
  foabic-fabric-1.3.0.jar
foabic-mods/
  mod-fabric.jar
```

NeoForge:

```text
mods/
  foabic-neoforge-1.3.0.jar
foabic-mods/
  mod-neoforge.jar
```

## Expected server evidence

```text
FOABIC_SURVIVAL_READY platform=... block=foabic_survival:feather_collector ...
FOABIC_DATA files=11 provider=5 jar=0 recipes=2 tags=2 loot=1
FOABIC_DATA_PACK platform=... id=...
FOABIC_SURVIVAL_SERVER platform=... recipe_loaded=true block_entity=true item=true entity=true sword_tag=true
FOABIC_STOP_CLEAN platform=... tick_listeners=false
```

The wisp is registered but not spawned and has no client renderer, so the
dedicated-server test does not try to render it. Add a platform renderer if you
want it visible in a client.

The full API is in [docs/DATA.md](../../docs/DATA.md),
[docs/BLOCK_ENTITIES.md](../../docs/BLOCK_ENTITIES.md) and
[docs/ENTITIES.md](../../docs/ENTITIES.md).
