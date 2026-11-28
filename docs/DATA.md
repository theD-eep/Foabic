# Data generation, recipes and tags

Foabic 1.3 turns blockstates, models, recipes, tags, loot tables and language
files into code. The same provider runs in two places:

1. **Build time** — the Foabic Loom `foabicData` task executes the provider and
   writes a normal `assets/` + `data/` tree into the mod jars.
2. **Runtime** — the Foabic loader executes the provider again and writes an
   always-on data pack at `.foabic/generated-data`.

Both paths use the same JSON builders, so a developer can inspect the files and
a server sees the same recipes and tags without a separate datagen launch.

## Declaring a provider

`foabic.mod.json`:

```json
{
  "id": "mymod",
  "entrypoint": "mymod.MyMod",
  "data": "mymod.MyData",
  "api": "^1.3.0"
}
```

`data` may be one class name or an array. Each class must be public, have a
public no-argument constructor, and implement `FoabicDataProvider`:

```java
public final class MyData implements FoabicDataProvider {
    @Override
    public void generate(FoabicDataContext data) {
        ResourceLocation block = ResourceLocation.of("mymod", "feather_collector");
        ResourceLocation blade = ResourceLocation.of("mymod", "feather_blade");

        data.blockStates(block).simple("mymod:block/feather_collector");
        data.itemModel(blade, "minecraft:item/handheld",
                Map.of("layer0", "mymod:item/feather_blade"));

        data.recipe(block)
                .shaped("F F", "FCF", "FFF")
                .define('F', "minecraft:feather")
                .define('C', "minecraft:chest")
                .result(block.toString())
                .save();

        data.tag(TagType.ITEM, ResourceLocation.parse("minecraft:swords"))
                .add(blade);
        data.tag(TagType.BLOCK, ResourceLocation.parse("minecraft:mineable/axe"))
                .add(block);

        data.lootTable(block).dropsSelf();
        data.lang("en_us").put("block.mymod.feather_collector", "Feather Collector");
    }
}
```

The provider may also be invoked from `ModInitializer`:

```java
context.data().provider(new MyData());
```

Do not register content from a provider. It only writes JSON; block/item/entity
registration belongs in `onInitialize`.

## Build-time task

Foabic Loom registers:

```bat
gradlew.bat foabicData
```

It writes to `build/foabic-data`, then `processResources` includes that directory
in both `dist/mod-fabric.jar` and `dist/mod-neoforge.jar`. `build` depends on it.
If no `data` provider is declared, the task skips with:

```text
FOABIC_DATAGEN skipped: no data provider declared
```

A successful run prints:

```text
FOABIC_DATAGEN mod=foabic_survival providers=1 files=11 data=5 assets=6 output=...\build\foabic-data
```

The provider runs in a plain JVM with the project's compile/runtime classpath;
it does not start a Minecraft server. Keep providers free of world access and
server state.

## Runtime data pack

After all Foabic mods initialize, the loader:

1. collects every `context.data()` file and every declared `data` provider;
2. copies `data/` entries shipped inside Foabic mod jars (so build-time output
   works even if a provider cannot run);
3. writes `.foabic/generated-data` with a 26.3 data-pack `pack.mcmeta`
   (`pack_format` 121, expressed as `min_format`/`max_format`);
4. logs `FOABIC_DATA ...` and registers the pack on both adapters.

Fabric uses `ResourceLoader.registerBuiltinPack` with an empty subpath, so the
same directory is visible as both client resources and server data. NeoForge
adds a `PathPackResources` repository source for `CLIENT_RESOURCES` and
`SERVER_DATA` in `AddPackFindersEvent`.

A dedicated server log shows:

```text
FOABIC_DATA files=11 provider=5 jar=0 recipes=2 tags=2 loot=1
FOABIC_DATA_PACK platform=fabric id=foabic:generated_data
[Server thread/INFO]: Found new data pack foabic:generated_data, loading it automatically
```

## Generated paths

| Builder | Standard path |
| --- | --- |
| `blockStates(id).simple(texture)` | `assets/<ns>/blockstates/<id>.json`, `models/block/<id>.json`, `models/item/<id>.json` |
| `blockStates(id).orientable(side, front, top)` | blockstate with `facing=north/east/south/west` and an `orientable` model |
| `blockStates(id).variant(...)` | one blockstate variant; repeated keys become a weighted variant list |
| `itemModel(id, parent)` | `assets/<ns>/models/item/<id>.json` |
| `recipe(id).shaped(...).save()` | `data/<ns>/recipe/<id>.json` |
| `recipe(id).shapeless(...).save()` | `data/<ns>/recipe/<id>.json` |
| `recipe(id).smelting/blasting/smoking/campfire(...)` | cooking recipe JSON |
| `recipe(id).stonecutting(...)` | stonecutting recipe JSON |
| `tag(type, tag).add(...)` | `data/<ns>/tags/<type>/<tag>.json` |
| `lootTable(block).dropsSelf()` | `data/<ns>/loot_table/blocks/<id>.json` |
| `lang(locale).put(...)` | `assets/<ns>/lang/<locale>.json` |
| `raw(path, json)` | any `assets/` or `data/` path |

`TagType` covers `item`, `block`, `entity_type`, `fluid`, `game_event`, `biome`,
`structure`, `enchantment`, `recipe`, `function` and `damage_type`.

## Tags

```java
data.tag(TagType.ITEM, ResourceLocation.parse("minecraft:swords"))
        .add("mymod:feather_blade")
        .optional("othermod:optional_blade");
```

- `add` writes a required entry.
- `addTag` writes `#namespace:path`.
- `optional` writes `{"id": "...", "required": false}`.
- `replace()` sets the tag's `replace` flag.
- Repeated calls, including calls from different providers, merge values by
  path instead of overwriting the file.

Tag files are written into their declared namespace. Adding to
`minecraft:swords` therefore writes `data/minecraft/tags/item/swords.json`; the
data pack merges it with vanilla unless `replace()` is used.

## Recipes

```java
data.recipe(id)
        .shaped(" F ", " F ", " S ")
        .define('F', "minecraft:feather")
        .define('S', "minecraft:stick")
        .result("mymod:feather_blade")
        .group("feather")
        .save();
```

Supported recipe types:

- `crafting_shaped`
- `crafting_shapeless`
- `smelting`, `blasting`, `smoking`, `campfire_cooking`
- `stonecutting`

The builder validates undefined pattern keys, row/column bounds, result count and
missing results before writing. Ingredient strings may be item ids or
`#namespace:tag`.

## Loot tables

```java
data.lootTable(block).dropsSelf();
data.lootTable(block).drop("minecraft:feather", 1, 3);
data.lootTable(block).noDrop();
```

The generated table uses `minecraft:block`, one pool, `survives_explosion`, and
`minecraft:set_count` for ranged drops.

## Models

`blockStates(...).simple(texture)` is the cube-all path used by the survival
example. Additional helpers:

```java
data.blockStates(id).cubeColumn("mymod:block/log_side", "mymod:block/log_top");
data.blockStates(id).cross("mymod:block/flower");
data.blockStates(id).orientable("mymod:block/side", "mymod:block/front", "mymod:block/top");
```

For arbitrary models use `raw()` or ship the JSON in `src/main/resources`. Foabic
also copies every `assets/` entry from Foabic mod jars into its generated
resource pack, so build-time models and language files work even though
`foabic-mods/` is not a Fabric/NeoForge resource pack.

## Limits

- No worldgen, advancement, structure or dimension builders. Use `raw()` or
  normal resource files.
- No automatic texture drawing; provide PNGs under
  `src/main/resources/assets/<id>/textures/`.
- Data packs are generated when the game starts. Editing generated JSON while a
  server is running requires `/reload` or a restart.
- Recipes and tags are data, not content registration. The referenced items and
  blocks must exist in the same game instance.
