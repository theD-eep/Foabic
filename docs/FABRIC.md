# Fabric adapter

## Version

Foabic 1.0 targets Fabric Loader **0.20.1** and Fabric API **0.166.2+26.3** on Minecraft **26.3**.

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
3. Bind `FoabicRegistries.BLOCKS` and `ITEMS` to `BuiltInRegistries`.
4. Load Foabic mods from `foabic-mods/`.
5. Register the signal bridges. `UseBlockCallback` filters to the logical server; `ServerLifecycleEvents` also covers an integrated server on a client.

Content runs on both sides so Fabric registry sync sees identical block/item ids. The v1.0 acceptance run is a dedicated server; it still exercises the same adapter that would run on a client.

## Registry bridge

`FabricRegistry<T>` wraps a `net.minecraft.core.Registry<T>`. A registration factory is evaluated immediately:

```java
T value = factory.get();
Registry.register(registry, id, value);
return new FabricRegistration<>(id, value);
```

A duplicate id or a frozen registry becomes a descriptive `RegistryException`. Register from `onInitialize`, before Minecraft freezes its registries.

## Signals

| Foabic signal | Fabric hook |
| --- | --- |
| `ServerStartedSignal` | `ServerLifecycleEvents.SERVER_STARTED` |
| `UseBlockSignal` | `UseBlockCallback.EVENT` |

`UseBlockCallback` is filtered to `!level.isClientSide()` and `ServerPlayer`, so a signal is posted once on the logical server. The `UseBlockSignal.Result` is mapped back to `InteractionResult`:

```text
PASS -> PASS
SUCCESS -> SUCCESS
CONSUME -> CONSUME
FAIL -> FAIL
```

Foabic does not register a client-side use-block bridge, so there is no double dispatch.

## Mod layout

```text
mods/
  fabric-api-0.166.2+26.3.jar
  foabic-fabric-1.0.0.jar
foabic-mods/
  my-foabic-mod.jar
```

Foabic mods are not Fabric mods. They contain `foabic.mod`, not `fabric.mod.json`, and belong in `foabic-mods/`.

The 0.2 dual-manifest asset arrangement is not part of v1.0. Resource-pack integration for Foabic mods is deferred.

## What the adapter does not do

- No Mixin configuration.
- No Fabric API event beyond the two signals above.
- No asset/resource-pack injection for Foabic mods.
- No commands, scheduler, config, or GUI API.
- No client verification harness in the v1.0 acceptance run.
