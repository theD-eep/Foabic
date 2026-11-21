# NeoForge adapter

## Version and metadata

Foabic 1.1 targets NeoForge **26.3.0.115** and Minecraft **26.3**. The adapter is an ordinary NeoForge mod:

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

## Gradle project

`:neoforge` uses Gradle's `java` plugin and compiles against the official universal artifact plus the installed NeoForge libraries:

```kotlin
compileOnly(files(neoForgeJar))
compileOnly(neoForgeLibraries)
```

This avoids a NeoForm/NFRT download, which is unavailable in the current network. In an environment with access to `maven.neoforged.net`, the same source set can be switched to `net.neoforged.moddev` without changing adapter code or jar contents. Build with `gradlew.bat build`.

## The timing problem

NeoForge constructs mods after the vanilla registries have been frozen. A vanilla `Block` constructor is not side-effect free:

```java
public Block(BlockBehaviour.Properties properties) {
    // ...
    this.builtInRegistryHolder = BuiltInRegistries.BLOCK.createIntrusiveHolder(this);
    // ...
}
```

`createIntrusiveHolder` throws `IllegalStateException: Registry is already frozen` if called during the Foabic mod constructor. That is why Foabic accepts factories rather than already-built blocks.

## Deferred registration

`NeoForgeRegistry<T>` stores `(id, factory)` pairs during mod construction. `FoabicNeoForge` listens on the mod event bus:

```java
modBus.addListener(RegisterEvent.class, this::onRegister);
```

For each event it flushes the block, item, block-entity-type and sound-event queues that match the event's registry key. `RegisterEvent.register(...)` evaluates the factory immediately in a writable registry window:

```java
event.register(registryKey, id, () -> {
    T value = factory.get();
    registration.value = value;
    return value;
});
```

The `Registration<T>` object returned to the mod is the same object that receives the value, so a later factory can call `block.get()`.

Registration order in 26.3 is sound, block, item, block-entity type. The verified run confirms all four queues flush and the block-entity factory can bind the already-created block.

### Block entities

`submitBlockEntity` queues a factory such as:

```java
new BlockEntityType<>(factory, Set.of(block.get()))
```

Because the block registry event runs before `BLOCK_ENTITY_TYPE`, `block.get()` is available. A fake-player verification placed the block and confirmed `getBlockEntity` returned `TestBlockEntity`.

## Signals

| Foabic signal | NeoForge hook |
| --- | --- |
| `ServerStartedSignal` | `ServerStartedEvent` |
| `ServerTickSignal` | `ServerTickEvent.Post` |
| `PlayerInteractBlockSignal` | `PlayerInteractEvent.RightClickBlock` |
| `PlayerBreakBlockSignal` | `BreakBlockEvent` |
| `ChatMessageSignal` | `ServerChatEvent` |

`PlayerInteractEvent.RightClickBlock` is filtered to the server side and `ServerPlayer`:

- `PASS` leaves the event untouched.
- `SUCCESS`, `CONSUME`, `FAIL` cancel the NeoForge event and set the matching cancellation result.

`BreakBlockEvent` maps to `PlayerBreakBlockSignal`; when the signal is cancelled, the NeoForge event is cancelled too.

`ServerChatEvent` maps to `ChatMessageSignal`; `replyAs` broadcasts a server system chat line formatted as `<name> message`.

The deprecated `UseBlockSignal` is still delivered when a listener exists; new code should use `PlayerInteractBlockSignal`.

## Creative tabs

`NeoForgeCreativeTabs` adds one `BuildCreativeModeTabContentsEvent` listener. It matches the event's tab key against `CreativeTab.id()` and accepts the resolved items from `FoabicCreativeTabs`. This runs after `RegisterEvent`, so deferred item registrations are present.

## Generated resources

After Foabic mods initialize, the loader writes `.foabic/generated-resources`. `NeoForgeGeneratedResources` listens for `AddPackFindersEvent` on the mod bus and, for `CLIENT_RESOURCES`, adds a fixed repository source:

- a `PackLocationInfo` with id `foabic_generated`;
- `PackSource.BUILT_IN`;
- `PathPackResources` over the generated directory;
- required, top, fixed selection.

It logs `FOABIC_RESOURCE_PACK`. No Mixin or access transformer is involved.

## Environment

`FMLEnvironment.getDist()` selects `Environment.CLIENT` or `Environment.SERVER`. `FMLPaths.GAMEDIR` is the root for `foabic-mods/` and `.foabic/`.

Foabic content mods are initialized on both sides, preserving registry parity for a client connection. The acceptance run is a dedicated server.

## Install

```text
mods/
  foabic-neoforge-1.1.0.jar
foabic-mods/
  my-foabic-mod.jar
```

Do not put a Foabic mod in `mods/` unless it is also a real NeoForge mod; Foabic mods contain only `foabic.mod` and are discovered from `foabic-mods/`.

`mods/` is also scanned as a fallback for convenience; `foabic-mods/` remains the preferred location.

## What the adapter does not do

- No Mixin configuration.
- No access transformers.
- No custom registry event for modded registries.
- No datagen, model or networking helpers beyond the generated resource pack.
- No NeoForge version other than 26.3.0.115 is tested.
