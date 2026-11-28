# Foabic 1.3

<img src="docs/assets/foabic-icon.png" width="96" alt="Foabic feather pixel icon">

**A small, platform-neutral mod API for Minecraft 26.3, with real Fabric and NeoForge adapters and a dual-jar Gradle plugin.**

Foabic 1.3 is the release where a Foabic mod becomes a playable survival mod
instead of a set of registered objects:

- **Block entities that keep state.** `FoabicBlockEntity` has typed saved fields
  that also synchronise to clients through vanilla block-entity packets.
- **Entities with vanilla data sync.** `FoabicEntity` exposes
  `SynchedEntityData` with a small `define` / `setSynced` / `synced` surface.
- **Data generation.** One `FoabicDataProvider` writes blockstates, models,
  recipes, tags, loot tables and language files. The same provider runs in the
  build and at server start.
- **Tags and recipes.** A one-line tag API adds an item to
  `#minecraft:swords`; shaped, shapeless, cooking and stonecutting recipes are
  built and validated in Java.
- **An always-on data pack.** Fabric and NeoForge both load
  `.foabic/generated-data`, including `data/` files shipped inside Foabic mod
  jars.
- **Lifecycle-safe signals.** `connect` returns a `FoabicSubscription`;
  `unsubscribe()` removes a listener immediately, and both adapters clear the
  session bus when the server stops.
- **A genuinely tiny core.** `foabic-api-1.3.0.jar` is **14,421 bytes**
  (< 15 KiB) and contains **no** `net.fabricmc.*` or `net.neoforged.*` import.
- **Foabic Loom.** One source tree, one `foabic.mod.json`, one
  `gradlew build`, and `dist/mod-fabric.jar` + `dist/mod-neoforge.jar`.
- **Networking and config.** The 1.2 packet and JSON-config rails are unchanged.

## Supported matrix

| Component | Version |
| --- | --- |
| Minecraft Java Edition | **26.3** |
| Java | **25** |
| Fabric Loader | **0.20.1** |
| Fabric API | **0.166.2+26.3** |
| NeoForge | **26.3.0.115** |
| Foabic | **1.3.0** |

Only Minecraft 26.3 is supported. There is no cross-version abstraction.

## Artifacts

```text
dist/foabic-api-1.3.0.jar                     14,421 B  core API, zero platform imports
dist/foabic-content-1.3.0.jar                 68,428 B  content API (Minecraft-facing)
dist/foabic-fabric-1.3.0.jar                 169,137 B  Fabric adapter + core + content + loader
dist/foabic-neoforge-1.3.0.jar               167,772 B  NeoForge adapter + core + content + loader
dist/foabic-example-minimal-1.3.0.jar         10,345 B  v1.3 acceptance mod
dist/foabic-example-chat-1.3.0.jar             2,786 B  chat example
dist/foabic-example-survival-fabric-1.3.0.jar 17,251 B  survival demo for Fabric
dist/foabic-example-survival-neoforge-1.3.0.jar 17,341 B survival demo for NeoForge
```

Exact byte sizes and SHA-256 hashes are in `dist/SHA256SUMS`. The core API,
content API and loader are bundled into each adapter jar, so a user installs one
platform jar, not four. There is no universal/merged jar.

`foabic-content` is a separate compile-time artifact only because the 15 KiB
core limit forbids putting Minecraft-facing types in the core jar. It is not a
runtime install.

## Install

### Fabric

```text
mods/
  fabric-api-0.166.2+26.3.jar
  foabic-fabric-1.3.0.jar

foabic-mods/
  my-foabic-mod.jar
```

### NeoForge

```text
mods/
  foabic-neoforge-1.3.0.jar

foabic-mods/
  my-foabic-mod.jar
```

Foabic mods are loaded by the Foabic loader, not by Fabric or NeoForge's own
entrypoint system. They contain `foabic.mod.json`. `mods/` is also scanned as a
fallback, but `foabic-mods/` is preferred.

The adapter must be present on both client and server for a client connection,
because content registration runs on both logical sides.

For the survival demo, install the matching example jar in `foabic-mods/`:

```text
foabic-mods/
  foabic-example-survival-fabric-1.3.0.jar
```

## A 1.3 mod

```java
public final class MyMod implements ModInitializer {
    @Override
    public void onInitialize(ModContext context) {
        MyConfig config = context.config().register(MyConfig.class);

        NetworkManager.register(PingPacket.class, PingPacket::new);

        FoabicSubscription tick = context.connect(ServerTickSignal.class, signal -> {
            if (signal.tick() == 20L) {
                context.logger().log(System.Logger.Level.INFO, "tick 20");
                tick.unsubscribe();
            }
        });
    }
}
```

`src/main/resources/foabic.mod.json`:

```json
{
  "schemaVersion": 1,
  "id": "mymod",
  "version": "1.3.0",
  "name": "My Mod",
  "description": "A small Foabic mod.",
  "authors": ["Me"],
  "license": "MIT",
  "environment": "*",
  "entrypoint": "mymod.MyMod",
  "data": "mymod.MyData",
  "api": "^1.3.0",
  "icon": "icon.png",
  "texture": "mymod:block/my_block",
  "depends": {
    "foabic": ">=1.3.0"
  }
}
```

`texture` is optional but useful for the automatic cube-all path. `data` is
optional; when present it names one `FoabicDataProvider` class or an array of
them.

## Block entities and data sync

```java
public final class CollectorBlockEntity extends FoabicBlockEntity {
    private final SyncedInt feathers = syncedInt("feathers", 0);

    public CollectorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void tick() {
        if (level == null || level.isClientSide()) return;
        if (level.getGameTime() % 200L == 0L) feathers.increment();
    }

    public int feathers() {
        return feathers.get();
    }
}
```

`SyncedInt`, `SyncedLong`, `SyncedFloat`, `SyncedBoolean` and `SyncedString` are
saved to the chunk and sent to tracking clients through the vanilla
`ClientboundBlockEntityDataPacket` path. `FoabicBlock` supplies the ticker. See
[BLOCK_ENTITIES.md](docs/BLOCK_ENTITIES.md).

## Entities

```java
public final class FeatherWisp extends FoabicEntity {
    private static final EntityDataAccessor<Integer> CHARGE =
            define(FeatherWisp.class, EntityDataSerializers.INT);

    public FeatherWisp(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineData(SynchedEntityData.Builder builder) {
        builder.define(CHARGE, 0);
    }

    public void setCharge(int value) {
        setSynced(CHARGE, value);
    }
}
```

Register the type with `context.submitEntity(id, builder)`. Data sync is vanilla
`SynchedEntityData`; a client renderer is still the mod's responsibility. See
[ENTITIES.md](docs/ENTITIES.md).

## Data generation, tags and recipes

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

Build time:

```bat
gradlew.bat foabicData
```

writes `build/foabic-data`, which `processResources` includes in both platform
jars. At runtime the loader writes `.foabic/generated-data` and registers it as
an always-on data pack on Fabric and NeoForge. Generated paths and all builders
are documented in [DATA.md](docs/DATA.md).

The tag call above is the complete API for the common case. Tags merge across
provider calls; `optional`, `addTag` and `replace` cover the rest.

## Survival example

`examples/survival` is a complete one-source-tree mod:

- `feather_collector`: a `FoabicBlock` with a ticking `FoabicBlockEntity` that
  stores and synchronises a feather counter;
- `feather_blade`: a real sword item (`Item.Properties.sword`), added to
  `#minecraft:swords`;
- `feather_wisp`: a custom entity type with a synced integer;
- shaped recipes for the collector and blade;
- block loot table and `mineable/axe` tag;
- blockstate, block/item models, `en_us` and `zh_cn` language;
- `dist/mod-fabric.jar` and `dist/mod-neoforge.jar`.

A dedicated server log from the acceptance run:

```text
FOABIC_DATA files=11 provider=5 jar=0 recipes=2 tags=2 loot=1
FOABIC_DATA_PACK platform=fabric id=foabic:generated_data
FOABIC_SURVIVAL_SERVER platform=fabric recipe_loaded=true block_entity=true item=true entity=true sword_tag=true
```

## Lifecycle and cleanup

```java
FoabicSubscription subscription =
        context.connect(ServerTickSignal.class, signal -> { ... });

subscription.unsubscribe();
```

- `connect` returns `FoabicSubscription`, not a one-way registration.
- `unsubscribe()` is idempotent.
- The Fabric adapter listens to `ServerLifecycleEvents.SERVER_STOPPED`.
- The NeoForge adapter listens to `ServerStoppedEvent`.
- Both call `FoabicInternal.onServerStopped()`, which clears the session event
  bus. Adapters log `FOABIC_STOP_CLEAN ... tick_listeners=false` after cleanup.
- `EventBus.clear()` also drops the listener callback, so a retained old handle
  does not keep the mod's listener object reachable.

## Registry freeze guard

The loader opens the registration window before mod initialization. Fabric
closes it after `FoabicLoader.load()`; NeoForge closes it at
`FMLCommonSetupEvent`, after `RegisterEvent` has flushed the deferred queues.

A late submission produces the required line:

```text
[Foabic] 模组 foabic_test 试图在注册表冻结后注册物品 foabic_test:late_item，已被拦截。
```

and throws `dev.foabic.api.registry.FoabicRegistryFrozenException`. The guard is
in both `ModContext.submit` and the `FoabicRegistries` proxy, so a direct late
`register` is intercepted too.

## Networking

The 1.2 packet API is unchanged: `FoabicPacket` + `NetworkManager.sendToServer`
/ `sendToClient`, backed by Fabric play payloads and NeoForge 26.3's
`RegisterPayloadHandlersEvent`. Maximum payload is 1 MiB.

## Config

`context.config().register(MyConfig.class)` reads or creates a pretty-printed
JSON file at `config/foabic/<mod-id>/<Type>.json`; records and ordinary POJOs
are supported.

## Foabic Loom

The plugin lives in `foabic-gradle-plugin/` and is an included build. In a
consuming project:

```kotlin
// settings.gradle.kts
pluginManagement {
    includeBuild("foabic-gradle-plugin")
}

// build.gradle.kts
plugins {
    java
    id("dev.foabic.loom")
}

foabic {
    minecraftVersion = "26.3"
}
```

Then:

```bat
gradlew.bat build
```

Outputs:

```text
dist/mod-fabric.jar
dist/mod-neoforge.jar
```

The plugin compiles the single main source set once; generates the canonical
`foabic.mod.json`; injects `fabric.mod.json` and
`META-INF/neoforge.mods.toml`; runs `foabicData`; and includes the generated
`assets/`/`data/` tree in both jars.

## Icons and launchers

Foabic uses the Minecraft-style feather pixel logo at
[docs/assets/foabic-icon.png](docs/assets/foabic-icon.png). A mod can use
`src/main/resources/icon.png` or an explicit `icon` path; Foabic Loom writes
Fabric `icon` and NeoForge `logoFile`.

Metadata icons are not displayed uniformly:

| Launcher | Mod-list icon from the jar? | Notes |
| --- | --- | --- |
| HMCL 3.9.2+ | Yes | Reads Fabric `icon`, NeoForge `logoFile`, then fallbacks. |
| PCL2 | **No** | Only displays the online Modrinth/CurseForge project logo, matched by hash. |
| Minecraft Mod Menu (Fabric) | Yes | Uses the Fabric `icon` field. |
| HMCL / PCL2 instance card | Yes, as a separate setting | HMCL uses `icon.png` in the instance root; PCL2 uses `PCL\Logo.png`. |

A jar in `foabic-mods/` is invisible to launcher mod lists; use the platform jar
in `mods/`. PCL2 needs an online project or a per-instance icon. The full
explanation, source references and a per-instance installer are in
[docs/LAUNCHER_ICONS.md](docs/LAUNCHER_ICONS.md).

## Build and verify

```bat
set JAVA_HOME=C:\path\to\jdk-25
gradlew.bat clean build
```

The build:

- compiles the core API, content API, loader and both adapters;
- runs the core tests, including the new data-generation tests;
- verifies the core API jar is under 15 KiB and scans `src/api` + `src/content`
  for platform imports;
- builds both example jars, the survival example and the Foabic Loom demo;
- copies the release jars to `dist/` and writes `SHA256SUMS`.

`gradlew.bat :api:check` prints:

```text
FOABIC_API_SIZE bytes=14421 limit=15360
```

## Runtime evidence

The survival example was started on a Fabric 26.3 dedicated server and a
NeoForge 26.3.0.115 dedicated server. Both runs:

- registered the block, block-entity type, sword item and entity type;
- loaded the generated data pack;
- reported `recipe_loaded=true` and `sword_tag=true`;
- exited cleanly after a console `stop` with `FOABIC_STOP_CLEAN`.

Full logs: [docs/VERIFICATION.md](docs/VERIFICATION.md).

## Deliberately not in 1.3

- **Mixin.** No Mixin dependency or annotation processor.
- **Rendering-layer work.** No Sodium/OpenGL integration, and no client renderer
  for custom entities.
- A universal/merged jar.
- GUI/screen API.
- Worldgen, structure, advancement or dimension builders.
- A live client network/rendering harness.
- Binary compatibility across 1.x.

## Documentation

- [API](docs/API.md)
- [Data generation and tags](docs/DATA.md)
- [Block entities and sync](docs/BLOCK_ENTITIES.md)
- [Entities and sync](docs/ENTITIES.md)
- [Build](docs/BUILD.md)
- [Fabric adapter](docs/FABRIC.md)
- [NeoForge adapter](docs/NEOFORGE.md)
- [Verification](docs/VERIFICATION.md)
- [Launcher icons](docs/LAUNCHER_ICONS.md)
- [Roadmap](docs/ROADMAP.md)

## License

MIT. See [LICENSE](LICENSE).
