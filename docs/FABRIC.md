# Fabric adapter

## Why Fabric Loader

Foabic 0.2 does not ship a Java agent, Mixin transformer, launcher or classpath patcher. Fabric Loader already supplies a stable entrypoint/mixin environment; Foabic attaches to it as an ordinary Fabric mod.

This keeps Beta 0.2 small and makes the first working path testable.

## Initialization

`FoabicFabric` implements Fabric's common `ModInitializer` and runs on both sides:

1. Read `FabricLoader.getGameDir()`.
2. Determine `Environment.CLIENT` or `Environment.SERVER` from `FabricLoader.getEnvironmentType()`.
3. Initialize Foabic mods from:
   - `foabic-mods/` (logic-only jars), and
   - `mods/` (jars that also need Fabric resource visibility).
4. On a dedicated server, register Fabric event bridges.

Both sides must see the same block/item IDs. That is why content mods run on the client as well; otherwise Fabric's registry sync disconnects the client with:

```text
Received 2 registry entries that are unknown to this client.
```

## Fabric events used

| Foabic event | Fabric hook |
| --- | --- |
| server starting/started/stopping/stopped | `ServerLifecycleEvents` |
| server tick start/end | `ServerTickEvents.START_SERVER_TICK` / `END_SERVER_TICK` |
| level tick start/end | `ServerTickEvents.START_LEVEL_TICK` / `END_LEVEL_TICK` |
| player join/leave | `ServerPlayConnectionEvents.JOIN` / `DISCONNECT` |
| commands | `CommandRegistrationCallback.EVENT` |
| `UseBlockSignal` | `UseBlockCallback.EVENT` |

`UseBlockCallback` is filtered to `!level.isClientSide()`, so a signal is posted once on the logical server, not once per side.

## Registry bridge

`FabricRegistry<T>` wraps a `net.minecraft.core.Registry<T>`:

```java
return net.minecraft.core.Registry.register(registry, mcId, value);
```

`FabricRegistries` exposes block, item, entity type, sound, particle and block-entity views. Registration must occur before Minecraft's freeze; a late call raises `RegistryException` with that explanation.

The adapter intentionally does **not** provide creative-tab integration, data generation, model generation, or automatic item registration. 0.2 is about proving the core registration/signal path.

## Two mod directories

```text
mods/
  fabric-api-...jar
  foabic-fabric-...jar
  my-content-mod.jar      # optional: has fabric.mod.json + assets/
foabic-mods/
  my-logic-mod.jar        # logic only, no fabric.mod.json
```

A Foabic mod that has no Fabric metadata must not be placed in `mods/`; Fabric Loader will reject it before Foabic runs.

A content mod that ships `assets/` needs a minimal `fabric.mod.json` with no entrypoint. Fabric loads it as a resource-only mod; Foabic still reads its `foabic.mod` and initializes the logic. The example demonstrates this dual-manifest approach.

## Build classpath

The adapter is compiled against:

- official Minecraft 26.1 server jar (unobfuscated)
- Minecraft libraries extracted from the server bundle
- Fabric Loader 0.20.1
- Fabric API 0.145.1+26.1 nested modules

The build script extracts these automatically from the supplied jars. No Loom, Gradle plugin, mappings or remapping step is used.

## Compatibility

- Tested with Minecraft 26.1, Fabric Loader 0.20.1, Fabric API 0.145.1+26.1.
- 26.2+ is out of scope for 0.2.
- Client and server adapters are the same jar; server-only events are registered only in the dedicated-server environment.
- No NeoForge, no cross-platform abstraction, no Mixin, no build plugin (0.3+).
