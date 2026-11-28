# NeoForge adapter

## Version and metadata

Foabic 1.3 targets NeoForge **26.3.0.115** and Minecraft **26.3**. The adapter is
an ordinary NeoForge mod:

```text
META-INF/neoforge.mods.toml
modLoader="javafml"
loaderVersion="[4,)"
```

The dependency ranges are deliberately narrow:

```toml
[[dependencies.foabic]]
modId="neoforge"
type="required"
versionRange="[26.3.0.115,)"
ordering="NONE"
side="BOTH"

[[dependencies.foabic]]
modId="minecraft"
type="required"
versionRange="[26.3,26.4)"
ordering="NONE"
side="BOTH"
```

The NeoForge jar bundles the core API, content API and loader. It is the only jar
a NeoForge user installs. Its `[[mods]]` entry sets `logoFile="icon.png"`; the
feather pixel icon is packaged at the jar root.

## Gradle project

`:neoforge` uses Gradle's `java` plugin and compiles against the official
universal artifact plus the installed NeoForge libraries:

```kotlin
compileOnly(files(neoForgeJar))
compileOnly(neoForgeLibraries)
```

This avoids a NeoForm/NFRT download. In an environment with access to
`maven.neoforged.net`, the same source set can be switched to
`net.neoforged.moddev` without changing adapter code or jar contents. Build with
`gradlew.bat build`.

## The timing problem

NeoForge constructs mods after the vanilla registries have been frozen. A vanilla
`Block` constructor is not side-effect free. `createIntrusiveHolder` throws if
called during the Foabic mod constructor, so Foabic accepts factories rather than
already-built blocks.

`NeoForgeRegistry<T>` stores `(id, factory)` pairs during mod construction.
`FoabicNeoForge` listens on the mod event bus:

```java
modBus.addListener(RegisterEvent.class, this::onRegister);
```

For each event it flushes the block, item, block-entity-type, entity-type and
sound-event queues that match the event's registry key.
`RegisterEvent.register(...)` evaluates the factory in a writable registry
window. The `Registration<T>` returned to the mod receives the value, so a later
factory can call `block.get()`.

Registration order in 26.3 is sound, block, item, block-entity type; entity types
are flushed in the same listener. The verified run confirms all five queues
flush and the block-entity factory can bind the already-created block.

## Registry freeze guard

After `RegisterEvent` has run, Foabic closes its registration window at
`FMLCommonSetupEvent`:

```java
modBus.addListener(FMLCommonSetupEvent.class,
        event -> FoabicRegistries.freezeRegistration());
```

A late `submit` now fails with:

```text
[Foabic] 模组 X 试图在注册表冻结后注册物品 Y，已被拦截。
```

and `FoabicRegistryFrozenException` instead of an obscure platform registry
error.

## Signals and cleanup

| Foabic signal | NeoForge hook |
| --- | --- |
| `ServerStartedSignal` | `ServerStartedEvent` |
| `ServerTickSignal` | `ServerTickEvent.Post` |
| `PlayerInteractBlockSignal` | `PlayerInteractEvent.RightClickBlock` |
| `PlayerBreakBlockSignal` | `BreakBlockEvent` |
| `ChatMessageSignal` | `ServerChatEvent` |

`PlayerInteractEvent.RightClickBlock` is filtered to the server side and
`ServerPlayer`:

- `PASS` leaves the event untouched.
- `SUCCESS`, `CONSUME`, `FAIL` cancel the NeoForge event and set the matching
  cancellation result.

`BreakBlockEvent` maps to `PlayerBreakBlockSignal`; cancellation is propagated.
`ServerChatEvent` maps to `ChatMessageSignal`; `replyAs` broadcasts a server
system line.

`ServerStartedSignal.server()` is populated from `ServerStartedEvent`, so a mod
can query the live `MinecraftServer` after data packs and recipes load.

On `ServerStoppedEvent`, the adapter calls `FoabicInternal.onServerStopped()` and
logs:

```text
FOABIC_STOP_CLEAN platform=neoforge tick_listeners=false
```

This clears every session subscription and releases listener callbacks.

## Networking

Foabic uses one play payload id, `foabic:packet`, and multiplexes packet class
name plus bytes. NeoForge 26.3 removed the old `SimpleChannel` API; the supported
path is:

```java
modBus.addListener(RegisterPayloadHandlersEvent.class, event -> {
    event.registrar("1").playBidirectional(TYPE, CODEC, (payload, context) ->
            context.enqueueWork(() -> NetworkManager.receive(...)));
});
```

Sending uses `PacketDistributor.sendToPlayer(serverPlayer, payload)` on the
server. Client-to-server sending is kept in a separate `NeoForgeClientNetwork`
class so a dedicated server never loads the client packet class. `sendToServer`
uses the client-only `ClientPacketDistributor`.

## Creative tabs

`NeoForgeCreativeTabs` adds one `BuildCreativeModeTabContentsEvent` listener. It
matches the event's tab key against `CreativeTab.id()` and accepts resolved items
from `FoabicCreativeTabs`. This runs after `RegisterEvent`, so deferred item
registrations are present.

## Generated resources and data

After Foabic mods initialize, the loader writes:

```text
.foabic/generated-resources   assets/ + resource pack (format 97.1)
.foabic/generated-data        data/ + data pack (format 121)
```

`NeoForgeGeneratedResources` listens for `AddPackFindersEvent` on the mod bus and
adds a fixed repository source for each pack type:

- `CLIENT_RESOURCES` → `.foabic/generated-resources`;
- `SERVER_DATA` → `.foabic/generated-data`;
- a `PackLocationInfo` with `PackSource.BUILT_IN`;
- `PathPackResources` over the generated directory;
- required, top, fixed selection.

It logs `FOABIC_RESOURCE_PACK` and `FOABIC_DATA_PACK`. The generated
`pack.mcmeta` uses 26.3's mandatory `min_format`/`max_format` fields; a legacy
`pack_format` value alone is rejected for data format 121.

No Mixin or access transformer is involved.

## Environment and install

`FMLEnvironment.getDist()` selects `Environment.CLIENT` or `Environment.SERVER`.
`FMLPaths.GAMEDIR` is the root for `foabic-mods/`, `.foabic/` and
`config/foabic/`.

```text
mods/
  foabic-neoforge-1.3.0.jar
foabic-mods/
  my-foabic-mod.jar
```

Foabic mods contain `foabic.mod.json` and are discovered from `foabic-mods/`;
`mods/` is also scanned as a fallback. Content mods are initialized on both
sides, preserving registry parity for a client connection. The acceptance run is
a dedicated server.

## What the adapter does not do

- No Mixin configuration.
- No access transformers.
- No custom registry event for modded registries.
- No client renderer for custom entities.
- No client graphical/audio/network round-trip verification in the
  dedicated-server acceptance run.
- No NeoForge version other than 26.3.0.115 is tested.
