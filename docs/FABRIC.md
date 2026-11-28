# Fabric adapter

## Version

Foabic 1.3 targets Fabric Loader **0.20.1** and Fabric API **0.166.2+26.3** on
Minecraft **26.3**.

The adapter is an ordinary Fabric mod with `environment: "*"` and one common
entrypoint:

```json
{
  "entrypoints": { "main": ["dev.foabic.fabric.FoabicFabric"] },
  "icon": "icon.png",
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

Minecraft 26.3 is unobfuscated, so there is no `mappings` dependency and no
`modImplementation` configuration. The Fabric API POM contributes its module
jars to the compile classpath. Build with `gradlew.bat build`.

The Fabric jar bundles the core API, content API and loader classes. It is the
only jar a Fabric user installs; a separate `foabic-api`/`foabic-content`
download is not needed at runtime.

## Initialization

`FoabicFabric` runs on both client and server:

1. Read `FabricLoader.getGameDir()`.
2. Map `EnvType.CLIENT` / `EnvType.SERVER` to `Environment`.
3. Collect Fabric-provided module versions for dependency preflight.
4. Bind `FoabicRegistries.BLOCKS`, `ITEMS`, `BLOCK_ENTITY_TYPES`,
   `ENTITY_TYPES` and `SOUND_EVENTS` to `BuiltInRegistries`.
5. Bind the Fabric network transport.
6. Load Foabic mods from `foabic-mods/` (and `mods/` as a fallback), reading
   `foabic.mod.json`.
7. Run content initializers and declared `FoabicDataProvider` classes.
8. Freeze the Foabic registration window after all mod initializers.
9. Register signal/lifecycle bridges, creative-tab callbacks and the generated
   resource/data packs.

Content runs on both sides so Fabric registry sync sees identical ids.

## Registry bridge and freeze guard

`FabricRegistry<T>` wraps a `net.minecraft.core.Registry<T>`. A registration
factory is evaluated immediately:

```java
T value = factory.get();
Registry.register(registry, id, value);
return new FabricRegistration<>(id, value);
```

A duplicate id or a frozen registry becomes a descriptive exception. The
explicit Foabic guard runs before this call. Fabric closes the Foabic
registration window immediately after `FoabicLoader.load()`; a mod that tries to
submit from a later signal gets:

```text
[Foabic] 模组 X 试图在注册表冻结后注册物品 Y，已被拦截。
```

and `FoabicRegistryFrozenException`.

Entity types use the same immediate path through `BuiltInRegistries.ENTITY_TYPE`.

## Signals and cleanup

| Foabic signal | Fabric hook |
| --- | --- |
| `ServerStartedSignal` | `ServerLifecycleEvents.SERVER_STARTED` |
| `ServerTickSignal` | `ServerTickEvents.END_SERVER_TICK` |
| `PlayerInteractBlockSignal` | `UseBlockCallback.EVENT` |
| `PlayerBreakBlockSignal` | `PlayerBlockBreakEvents.BEFORE` |
| `ChatMessageSignal` | `ServerMessageEvents.CHAT_MESSAGE` |

`UseBlockCallback` is filtered to `!level.isClientSide()` and `ServerPlayer`, so
a signal is posted once on the logical server. `PlayerInteractBlockSignal.Result`
is mapped back to `InteractionResult`:

```text
PASS -> PASS
SUCCESS -> SUCCESS
CONSUME -> CONSUME
FAIL -> FAIL
```

`PlayerBlockBreakEvents.BEFORE` is filtered the same way; returning `false` when
the signal is cancelled aborts the break.

`ServerStartedSignal.server()` is populated from the lifecycle callback, so a
mod can query the live `MinecraftServer` after data packs and recipes load.

On `ServerLifecycleEvents.SERVER_STOPPED`, the adapter calls
`FoabicInternal.onServerStopped()` and logs:

```text
FOABIC_STOP_CLEAN platform=fabric tick_listeners=false
```

That clears every session subscription. A retained `FoabicSubscription` handle
is harmless; the callback has been released.

## Networking

Foabic uses one play payload id, `foabic:packet`, and multiplexes packet class
name plus bytes:

- `PayloadTypeRegistry.serverboundPlay()` and `clientboundPlay()` register the
  payload/codec;
- `ServerPlayNetworking.registerGlobalReceiver` dispatches to
  `NetworkManager.receive` on the server thread;
- on a client, `ClientPlayNetworking.registerGlobalReceiver` dispatches on the
  client thread;
- `sendToServer` uses `ClientPlayNetworking.send`;
- `sendToClient(ServerPlayer, ...)` uses `ServerPlayNetworking.send`.

The adapter compiles on a dedicated server without loading the client networking
class until a client branch is actually used.

## Creative tabs

`FabricCreativeTabs` registers one `CreativeModeTabEvents.modifyOutputEvent` per
`CreativeTab` enum value. It resolves `FoabicCreativeTabs.items(tab)` at output
time, after all deferred registrations have run, and calls
`output.accept(item)`.

`submitBlock` queues the generated block item for `BUILDING_BLOCKS`
automatically; `submitItem` accepts a `CreativeTab`.

## Generated resources and data

After Foabic mods initialize, the loader writes:

```text
.foabic/generated-resources   assets/ + resource pack (format 97.1)
.foabic/generated-data        data/ + data pack (format 121)
```

`FabricGeneratedResources` wraps each directory in a synthetic `ModContainer` and
calls:

```java
ResourceLoader.registerBuiltinPack(packId, "", container, description,
        PackActivationType.ALWAYS_ENABLED);
```

The explicit **empty subpath** is required. The convenience overload without it
looks under `resourcepacks/<id>` and rejects a root-level pack. Fabric's built-in
pack API registers both `CLIENT_RESOURCES` and `SERVER_DATA`, so the data root is
visible on a dedicated server and the resource root on a client.

The generated `pack.mcmeta` uses 26.3's mandatory `min_format`/`max_format`
fields. The old `pack_format` value alone is rejected for data format 121:

```json
{
  "pack": {
    "description": "Foabic generated recipes, tags and loot",
    "min_format": [121, 0],
    "max_format": [121, 0]
  }
}
```

The data pack is always enabled and logs:

```text
FOABIC_DATA files=11 provider=5 jar=0 recipes=2 tags=2 loot=1
FOABIC_DATA_PACK platform=fabric id=foabic:generated_data
```

Fabric's built-in pack API needs no Mixin, resource interception or jar rewriting.

## Mod layout

```text
mods/
  fabric-api-0.166.2+26.3.jar
  foabic-fabric-1.3.0.jar
foabic-mods/
  my-foabic-mod.jar
```

Foabic mods contain `foabic.mod.json`, not `fabric.mod.json`, and belong in
`foabic-mods/`. The fallback `mods/` scan still works for convenience.

## What the adapter does not do

- No Mixin configuration or custom rendering layer.
- No client renderer for custom entities.
- No commands, scheduler or GUI API.
- No client graphical/audio verification in the dedicated-server acceptance run.
- No Fabric version other than the matrix above is tested.
