# Foabic 1.0

**A small, platform-neutral mod API for Minecraft 26.3, with real Fabric and NeoForge adapters.**

Foabic 1.0 is deliberately judged by one narrow contract:

> On Minecraft 26.3, one minimal Foabic mod starts on Fabric and on NeoForge, registers one object, receives one signal, and produces the same observable output when the run is repeated.

That contract is verified on both dedicated servers. It is the foundation for v1.1, not a claim that every subsystem is finished.

## Supported matrix

| Component | Version |
| --- | --- |
| Minecraft Java Edition | **26.3** |
| Java | **25** |
| Fabric Loader | **0.20.1** |
| Fabric API | **0.166.2+26.3** |
| NeoForge | **26.3.0.115** |
| Foabic | **1.0.0** |

Only Minecraft 26.3 is supported by 1.0. There is no 26.1 compatibility mode and no cross-version abstraction.

## Artifacts

```text
dist/foabic-fabric-1.0.0.jar             68 KiB   Fabric adapter + API + loader
dist/foabic-neoforge-1.0.0.jar           69 KiB   NeoForge adapter + API + loader
dist/foabic-api-1.0.0.jar                34 KiB   compile-time API only
dist/foabic-example-minimal-1.0.0.jar     4 KiB   platform-neutral acceptance mod
```

The platform jars are separate. The API and loader are bundled into each adapter jar, so a user installs one platform jar, not three. There is no universal/merged jar.

`gradlew build` also writes `dist/SHA256SUMS` for release verification.

## Install

### Fabric

Place in the Fabric `mods/` directory:

```text
mods/
  fabric-api-0.166.2+26.3.jar
  foabic-fabric-1.0.0.jar
```

Place Foabic mods in `foabic-mods/`:

```text
foabic-mods/
  my-foabic-mod.jar
```

### NeoForge

Place in the NeoForge `mods/` directory:

```text
mods/
  foabic-neoforge-1.0.0.jar
```

Place Foabic mods in `foabic-mods/`:

```text
foabic-mods/
  my-foabic-mod.jar
```

Foobic mods are **not** Fabric or NeoForge mods. They contain `foabic.mod` and use the common API; they must not be placed in `mods/`. A Foabic mod that needs loader-visible resources is outside the 1.0 surface.

The Foabic adapter must be present on both client and server for a client connection, because content registration runs on both logical sides. The v1.0 acceptance test uses dedicated servers.

## Minimal mod

```java
public final class MyMod implements ModInitializer {
    private static final ResourceLocation BLOCK_ID =
            ResourceLocation.of("mymod", "test_block");
    private static final Identifier MC_ID =
            Identifier.fromNamespaceAndPath("mymod", "test_block");
    private static final ResourceKey<Item> ITEM_KEY =
            ResourceKey.create(Registries.ITEM, MC_ID);

    @Override
    public void onInitialize(ModContext context) {
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, MC_ID);
        Registration<Block> block = context.submit(
                FoabicRegistries.BLOCKS, BLOCK_ID,
                () -> new Block(BlockBehaviour.Properties.of()
                        .setId(blockKey)
                        .strength(1.0F)));

        context.submit(FoabicRegistries.ITEMS, BLOCK_ID,
                () -> new BlockItem(block.get(), new Item.Properties().setId(ITEM_KEY)));

        context.connect(ServerStartedSignal.class, signal -> {
            context.logger().log(System.Logger.Level.INFO,
                    "FOABIC_SIGNAL platform={0} mc={1} mods={2}",
                    signal.platform(), signal.minecraftVersion(), signal.modCount());
        });
    }
}
```

`foabic.mod`:

```properties
id=mymod
name=My Mod
version=1.0.0
entrypoint=mymod.MyMod
environment=*
api=^1.0.0
```

## Why registrations are factories

Fabric lets a mod construct a `Block` during `onInitialize`. NeoForge 26.3 freezes the vanilla registries before mod constructors run; the `Block` constructor creates an intrusive holder, so constructing a block in the Foabic constructor throws:

```text
java.lang.IllegalStateException: Registry is already frozen
```

Foabic therefore accepts a **factory**:

- Fabric evaluates it immediately during `onInitialize` and registers the value.
- NeoForge stores it and evaluates it inside `RegisterEvent`, where the target registry is writable.

A `Registration<T>` handle lets a later factory depend on an earlier object:

```java
Registration<Block> block = context.submit(FoabicRegistries.BLOCKS, id, blockFactory);
context.submit(FoabicRegistries.ITEMS, id,
        () -> new BlockItem(block.get(), itemProperties));
```

`Registration.get()` throws a clear error if called before the object has been created. Calling it from a signal listener, or from a later registration factory, is safe.

## Signals in 1.0

v1.0 ships a signal kernel and two concrete signals, not a signal standard library:

- `ServerStartedSignal` — deterministic startup observation, used by the v1.0 acceptance test.
- `UseBlockSignal` — server-side right-click observation, bridged from Fabric `UseBlockCallback` and NeoForge `PlayerInteractEvent.RightClickBlock`.

`connect` is a thin alias over the shared synchronous `EventBus`. The bus does exact-type dispatch with priorities and isolated listener failures.

`UseBlockSignal` is a plain data signal; it carries `playerName`, `dimension`, `block`, `pos`, `hand` and a mutable `Result`. It does not hold live Minecraft objects.

## Verification

The acceptance mod is [examples/minimal](examples/minimal). The verification script starts each server twice, removes the test world between runs, waits for `FOABIC_SIGNAL`, and compares the exact Foabic observations.

Expected evidence for each platform:

```text
FOABIC_REGISTER platform=fabric block=foabic_test:test_block item=foabic_test:test_block
FOABIC_SIGNAL name=server_started platform=fabric mc=26.3 mods=1
FOABIC_STATUS platform=fabric block_registered=true item_registered=true
```

and the same three lines with `platform=neoforge`.

```bat
gradlew.bat build
```

The `:coreTest` task prints the test output. Full evidence and environment: [docs/VERIFICATION.md](docs/VERIFICATION.md).

## Build

```bat
set JAVA_HOME=C:\path\to\jdk-25
gradlew.bat build
```

The repository ships a Gradle **9.5.1** wrapper. The build uses Java 25 toolchains and produces the four jars in `dist/`.

- `:fabric` uses the non-remapping `net.fabricmc.fabric-loom` plugin for Minecraft 26.3.
- `:neoforge` uses Gradle's `java` plugin against the official NeoForge 26.3.0.115 universal artifact; this avoids a NeoForm/NFRT download that is unavailable in the current network.
- `:coreTest` runs the platform-neutral event/version/signal tests.

The default Minecraft and NeoForge inputs are read from `.cache/`; see [docs/BUILD.md](docs/BUILD.md) for the exact files and paths.

## Not in 1.0

These are intentionally deferred so v1.0 stays debuggable:

- Mixin support — v1.1
- a Gradle plugin for Foabic mod authors — v1.1
- universal/cross-platform packaging — v1.1
- a full signal standard library — later
- networking, GUI, models, creative tabs, data generation
- NeoForge/Fabric adapters beyond 26.3
- binary compatibility across 1.x

## Documentation

- [API](docs/API.md)
- [Fabric adapter](docs/FABRIC.md)
- [NeoForge adapter](docs/NEOFORGE.md)
- [Build inputs](docs/BUILD.md)
- [Verification](docs/VERIFICATION.md)
- [Roadmap](docs/ROADMAP.md)

## License

MIT. See [LICENSE](LICENSE).
