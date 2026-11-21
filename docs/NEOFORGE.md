# NeoForge adapter

## Version and metadata

Foabic 1.0 targets NeoForge **26.3.0.115** and Minecraft **26.3**. The adapter is an ordinary NeoForge mod:

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

`createIntrusiveHolder` throws `IllegalStateException: Registry is already frozen` if called during the Foabic mod constructor. This is why v1.0 does not let a mod pass an already-built `Block` to `submit`.

## Deferred registration

`NeoForgeRegistry<T>` stores `(id, factory)` pairs during mod construction. `FoabicNeoForge` listens on the mod event bus:

```java
modBus.addListener(RegisterEvent.class, this::onRegister);
```

For each event, it flushes the block and item queues that match the event's registry key. `RegisterEvent.register(...)` evaluates the factory immediately in a writable registry window:

```java
event.register(registryKey, id, () -> {
    T value = factory.get();
    registration.value = value;
    return value;
});
```

The `Registration<T>` object returned to the mod is the same object that receives the value, so a later item factory can call `block.get()`.

The flush order is blocks then items. NeoForge fires the block registry event before the item registry event in 26.3; the verified acceptance run confirms `block_registered=true item_registered=true`.

## Signals

`ServerStartedEvent` is mapped to `ServerStartedSignal`. `PlayerInteractEvent.RightClickBlock` is filtered to the server side and mapped to `UseBlockSignal`:

- `PASS` leaves the event untouched.
- `SUCCESS`, `CONSUME`, `FAIL` cancel the NeoForge event and set the matching cancellation result.

The v1.0 acceptance test uses `ServerStartedSignal` because it is deterministic and does not need a client.

## Environment

`FMLEnvironment.getDist()` selects `Environment.CLIENT` or `Environment.SERVER`. `FMLPaths.GAMEDIR` is the root for `foabic-mods/` and `.foabic/`.

Foabic content mods are initialized on both sides, preserving registry parity for a future client connection. The 1.0 acceptance run is a dedicated server.

## Install

```text
mods/
  foabic-neoforge-1.0.0.jar
foabic-mods/
  my-foabic-mod.jar
```

Do not put a Foabic mod in `mods/` unless it is also a real NeoForge mod; Foabic mods contain only `foabic.mod` and are discovered from `foabic-mods/`.

## What the adapter does not do

- No Mixin configuration.
- No access transformers.
- No custom registry event for modded registries.
- No datagen, model, creative-tab, or networking helpers.
- No NeoForge version other than 26.3.0.115 is tested.
