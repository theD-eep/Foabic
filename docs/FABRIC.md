# Fabric adapter

## Version

Foabic 1.1 targets Fabric Loader **0.20.1** and Fabric API **0.166.2+26.3** on Minecraft **26.3**.

The adapter is an ordinary Fabric mod with `environment: "*"` and one common entrypoint:

```json
{
  "entrypoints": { "main": ["dev.foabic.fabric.FoabicFabric"] },
  "depends": {
    "fabricloader": ">=0.20.1",
    "minecraft": "~26.3",
    "fabric-api": ">=0.166.2"
  }
}
```

## Gradle project

`:fabric` is a normal Gradle subproject using the **non-remapping** Loom plugin:

```kotlin
plugins {
    id("net.fabricmc.fabric-loom") version "1.14.10"
}

dependencies {
    minecraft("com.mojang:minecraft:26.3")
    implementation("net.fabricmc:fabric-loader:0.20.1")
    implementation("net.fabricmc.fabric-api:fabric-api:0.166.2+26.3")
}
```

Minecraft 26.3 is unobfuscated, so there is no `mappings` dependency and no `modImplementation` configuration. The Fabric API POM contributes its module jars to the compile classpath. Build with `gradlew.bat build`.

## Initialization

`FoabicFabric` runs on both client and server:

1. Read `FabricLoader.getGameDir()`.
2. Map `EnvType.CLIENT` / `EnvType.SERVER` to `Environment`.
3. Collect Fabric-provided module versions (including `fabric-api` and `fabricloader`) for dependency preflight.
4. Bind `FoabicRegistries.BLOCKS`, `ITEMS`, `BLOCK_ENTITY_TYPES` and `SOUND_EVENTS` to `BuiltInRegistries`.
5. Load Foabic mods from `foabic-mods/` (and `mods/` as a fallback).
6. Register signal bridges, creative-tab callbacks and, on a client, the generated resource pack.

Content runs on both sides so Fabric registry sync sees identical ids.

## Registry bridge

`FabricRegistry<T>` wraps a `net.minecraft.core.Registry<T>`. A registration factory is evaluated immediately:

```java
T value = factory.get();
Registry.register(registry, id, value);
return new FabricRegistration<>(id, value);
```

A duplicate id or a frozen registry becomes a descriptive `RegistryException`. Register from `onInitialize`, before Minecraft freezes its registries.

`submitBlock` creates the block immediately, but its `BlockItem` factory can still call `block.get()` because the block value already exists. `submitBlockEntity` runs after the block registration, so its factory can bind the block into the block-entity type's valid-block set.

## Signals

| Foabic signal | Fabric hook |
| --- | --- |
| `ServerStartedSignal` | `ServerLifecycleEvents.SERVER_STARTED` |
| `ServerTickSignal` | `ServerTickEvents.END_SERVER_TICK` |
| `PlayerInteractBlockSignal` | `UseBlockCallback.EVENT` |
| `PlayerBreakBlockSignal` | `PlayerBlockBreakEvents.BEFORE` |
| `ChatMessageSignal` | `ServerMessageEvents.CHAT_MESSAGE` |

`UseBlockCallback` is filtered to `!level.isClientSide()` and `ServerPlayer`, so a signal is posted once on the logical server. The `PlayerInteractBlockSignal.Result` is mapped back to `InteractionResult`:

```text
PASS -> PASS
SUCCESS -> SUCCESS
CONSUME -> CONSUME
FAIL -> FAIL
```

`PlayerBreakBlockEvents.BEFORE` is filtered the same way; returning `false` from the callback when the signal is cancelled aborts the break.

The deprecated `UseBlockSignal` is still delivered when a listener exists; new code should use `PlayerInteractBlockSignal`.

## Creative tabs

`FabricCreativeTabs` registers one `CreativeModeTabEvents.modifyOutputEvent` per `CreativeTab` enum value. It resolves `FoabicCreativeTabs.items(tab)` at output time, after all deferred registrations have run, and calls `output.accept(item)`.

`submitBlock` queues the generated block item for `BUILDING_BLOCKS` automatically.

## Generated resources

After Foabic mods initialize, the loader writes `.foabic/generated-resources` with blockstates, block/item models, copied textures and `pack.mcmeta`. On a client, `FabricGeneratedResources`:

1. wraps the generated directory in a synthetic `ModContainer` whose `getRootPaths()` is that directory;
2. calls `ResourceLoader.registerBuiltinPack("foabic:generated", container, ..., ALWAYS_ENABLED)`;
3. logs `FOABIC_RESOURCE_PACK`.

The pack is always enabled and sits at the top of the resource stack. No Mixin, resource interception or jar rewriting is involved.

## Mod layout

```text
mods/
  fabric-api-0.166.2+26.3.jar
  foabic-fabric-1.1.0.jar
foabic-mods/
  my-foabic-mod.jar
```

Foabic mods are not Fabric mods. They contain `foabic.mod`, not `fabric.mod.json`, and belong in `foabic-mods/`.

`mods/` is also scanned as a fallback, so a Foabic jar dropped next to Fabric mods still loads. Fabric logs it as a non-Fabric mod; prefer `foabic-mods/`.

## What the adapter does not do

- No Mixin configuration.
- No custom rendering layer.
- No commands, scheduler, config or GUI API.
- No client verification harness in the dedicated-server acceptance run.
- No Fabric version other than the table above is tested.
