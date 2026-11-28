# Foabic 1.3 verification

## Contract

On Minecraft 26.3, one platform-neutral Foabic mod starts on Fabric and on
NeoForge, registers a block, block item, block-entity type, sword item and custom
entity type, generates and loads recipes/tags/loot/models/language, reports that
the recipe and `minecraft:swords` tag are live, and leaves no signal listeners
after a clean server stop.

The same final 1.3 jars were started on a Fabric 26.3 dedicated server and a
NeoForge 26.3.0.115 dedicated server. Both runs exited 0 after a console `stop`.

## Environment

| Item | Value |
| --- | --- |
| Date | 2026-11-28 |
| OS | Windows 11 |
| Java | Eclipse Temurin 25.0.5.1+2 |
| Gradle | 9.5.1 (wrapper) |
| Minecraft | Java Edition 26.3 |
| Fabric Loader | 0.20.1 |
| Fabric API | 0.166.2+26.3 |
| NeoForge | 26.3.0.115 |
| Foabic | 1.3.0 |

## Build

```bat
gradlew.bat clean build
```

Result:

```text
FOABIC_API_SIZE bytes=14421 limit=15360
...
ALL TESTS PASSED (10)
BUILD SUCCESSFUL
```

Core tests cover:

- event dispatch, unsubscribe and clear;
- listener failure isolation;
- version/range parsing, including NeoForge's four-part `26.3.0.115`;
- legacy and v1.3 signal data classes;
- network encode/decode/dispatch with a fake transport;
- duplicate-id discovery and malformed-metadata skipping;
- JSON `foabic.mod.json` parsing, including `data`/`dataProviders`;
- missing Fabric API dependency diagnostics and provided-version resolution;
- registry-freeze exception and exact ERROR line;
- config POJO/record save and reload;
- generated blockstate/model/texture copy and missing-texture fallback;
- generated blockstates, item models, recipes, tags, optional tag entries, loot
  tables, language files and path validation.

## Data-generation evidence

`examples/survival` uses:

```json
"entrypoint": "survival.SurvivalMod",
"data": "survival.SurvivalData"
```

`gradlew.bat :examples:survival:build` produced:

```text
> Task :examples:survival:foabicData
FOABIC_DATAGEN mod=foabic_survival providers=1 files=11 data=5 assets=6 output=...\build\foabic-data
```

Generated tree (11 JSON files):

```text
assets/foabic_survival/blockstates/feather_collector.json
assets/foabic_survival/models/block/feather_collector.json
assets/foabic_survival/models/item/feather_collector.json
assets/foabic_survival/models/item/feather_blade.json
assets/foabic_survival/lang/en_us.json
assets/foabic_survival/lang/zh_cn.json
data/foabic_survival/recipe/feather_collector.json
data/foabic_survival/recipe/feather_blade.json
data/foabic_survival/loot_table/blocks/feather_collector.json
data/minecraft/tags/block/mineable/axe.json
data/minecraft/tags/item/swords.json
```

The two PNGs are normal resources, not generated:

```text
assets/foabic_survival/textures/block/feather_collector.png
assets/foabic_survival/textures/item/feather_blade.png
```

The recipe and tag JSON were inspected directly:

```json
{"type":"minecraft:crafting_shaped","group":"feather",
 "pattern":["F F","FCF","FFF"],
 "key":{"F":"minecraft:feather","C":"minecraft:chest"},
 "result":{"id":"foabic_survival:feather_collector"}}
```

```json
{"values":["foabic_survival:feather_blade"]}
```

## Runtime data-pack evidence

The loader wrote the same 11 files into `.foabic/generated-data` and registered
the pack on both adapters:

```text
FOABIC_DATA files=11 provider=5 jar=0 recipes=2 tags=2 loot=1
FOABIC_DATA_PACK platform=fabric id=foabic:generated_data
FOABIC_DATA_PACK platform=neoforge id=foabic_generated_data
```

Both dedicated servers loaded it:

```text
[Server thread/INFO]: Found new data pack foabic:generated_data, loading it automatically
[main/INFO] [minecraft/MinecraftServer]: Found new data pack foabic_generated_data, loading it automatically
```

The first attempt used the legacy `pack_format` field and Minecraft 26.3
rejected it:

```text
Pack declares support for version newer than 81, but is missing mandatory
fields min_format and max_format
```

The final `pack.mcmeta` uses the mandatory fields:

```json
{
  "pack": {
    "description": "Foabic generated recipes, tags and loot",
    "min_format": [121, 0],
    "max_format": [121, 0]
  }
}
```

No pack-metadata warning appears in the final runs.

## Survival-mod evidence

The example registers in `onInitialize` and checks live state from
`ServerStartedSignal.server()`:

```text
FOABIC_SURVIVAL_READY platform=fabric block=foabic_survival:feather_collector block_entity=foabic_survival:feather_collector item=foabic_survival:feather_blade entity=foabic_survival:feather_wisp
FOABIC_SURVIVAL_SERVER platform=fabric recipe_loaded=true block_entity=true item=true entity=true sword_tag=true
```

```text
FOABIC_SURVIVAL_READY platform=neoforge block=foabic_survival:feather_collector block_entity=foabic_survival:feather_collector item=foabic_survival:feather_blade entity=foabic_survival:feather_wisp
FOABIC_SURVIVAL_SERVER platform=neoforge recipe_loaded=true block_entity=true item=true entity=true sword_tag=true
```

`recipe_loaded=true` comes from
`MinecraftServer.getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE,
foabic_survival:feather_collector))`. `sword_tag=true` comes from the live item
registry holder's `is(TagKey.create(Registries.ITEM, minecraft:swords))`. This is
proof that the generated files were parsed into the running server, not merely
written to disk.

Both runs stopped cleanly:

```text
FOABIC_STOP_CLEAN platform=fabric tick_listeners=false
FOABIC_STOP_CLEAN platform=neoforge tick_listeners=false
```

Full curated logs:

- [Fabric](verification/v13-fabric.txt)
- [NeoForge](verification/v13-neoforge.txt)

## Artifacts

| File | Bytes |
| --- | ---: |
| `foabic-api-1.3.0.jar` | 14,421 |
| `foabic-content-1.3.0.jar` | 68,428 |
| `foabic-fabric-1.3.0.jar` | 169,137 |
| `foabic-neoforge-1.3.0.jar` | 167,772 |
| `foabic-example-minimal-1.3.0.jar` | 10,345 |
| `foabic-example-chat-1.3.0.jar` | 2,786 |
| `foabic-example-survival-fabric-1.3.0.jar` | 17,251 |
| `foabic-example-survival-neoforge-1.3.0.jar` | 17,341 |

The core jar is under the 15 KiB (15,360 byte) limit. `verifyApiContract` also
scans `src/api` and `src/content`; no `net.fabricmc.*` or `net.neoforged.*`
import is present.

## What this proves

- The final Gradle-built platform jars load the same 1.3 survival mod on Fabric
  and NeoForge.
- The block, block-entity type, sword item and entity type are registered on
  both platforms. On NeoForge the check runs after `RegisterEvent`, so deferred
  registration is proven, not assumed.
- The generated recipes and `minecraft:swords` tag are present in the running
  server's recipe manager and item registry.
- The data pack is registered and loaded on both platforms, including the 26.3
  `min_format`/`max_format` metadata.
- The data-generation task produces the same files in the build and at runtime.
- The server-stopped hook clears listeners on both platforms.
- The core API is 14,421 bytes and contains no platform imports.
- Both dedicated servers exit 0 after a console `stop`.

## What this does not prove

- **A live client.** No graphical client was launched. Block-entity
  `ClientboundBlockEntityDataPacket` sync and entity `SynchedEntityData` sync are
  implemented against the vanilla APIs and compile on both adapters, but a
  two-client visual round-trip was not captured.
- **A custom entity renderer.** The example registers `feather_wisp` but does not
  spawn it and does not ship a renderer. Foabic deliberately does not abstract
  client rendering.
- **Block-entity persistence across a server restart.** The fields use vanilla
  `ValueInput`/`ValueOutput` and `saveAdditional`/`loadAdditional`, but the
  dedicated-server check does not place a block and reload the chunk.
- **Creative-tab UI, JEI/REI recipe display, or loot-table rolls.** These are
  data/registry checks, not graphical UI tests.
- **Any Minecraft version other than 26.3, or any Fabric/NeoForge version other
  than the table above.**
- **Binary compatibility across Foabic 1.x.**

The NeoForge runtime emits Netty epoll/kqueue Windows warnings. They are
environment noise, not Foabic load failures; both runs reached `Done` and exited
0.

## 1.2 regression evidence

The 1.2 acceptance path (block/item/block-entity type/sound registration,
config, packet registration, freeze guard, unsubscribe and stop cleanup) is
covered by the same `MinimalMod` example and by the core tests. The v1.2 logs
remain in `docs/verification/v12-fabric.txt` and
`docs/verification/v12-neoforge.txt`.
