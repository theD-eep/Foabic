# Foabic 1.1

**A small, platform-neutral mod API for Minecraft 26.3, with real Fabric and NeoForge adapters.**

Foabic 1.1 keeps the v1.0 rule that a minimal mod starts on both loaders, registers content and receives a signal, then adds the pieces a small content mod actually needs:

- defensive mod discovery: duplicate ids and broken metadata are logged and skipped, not turned into a game crash;
- dependency preflight: missing `fabric-api` / `foabic-api` versions produce one actionable error, not a late `NoClassDefFoundError`;
- blocks with automatic `BlockItem` registration and creative-tab insertion;
- `BlockEntity` binding and `SoundEvent` registration;
- `PlayerInteractBlockSignal`, `PlayerBreakBlockSignal` and `ServerTickSignal` on both adapters;
- simple resource injection: one `texture=` line in `foabic.mod` generates the blockstate, block model and item model, and installs a built-in resource pack. No purple-black missing model, no manual JSON layout and no Mixin.

## Supported matrix

| Component | Version |
| --- | --- |
| Minecraft Java Edition | **26.3** |
| Java | **25** |
| Fabric Loader | **0.20.1** |
| Fabric API | **0.166.2+26.3** |
| NeoForge | **26.3.0.115** |
| Foabic | **1.1.0** |

Only Minecraft 26.3 is supported. There is no cross-version abstraction.

## Artifacts

```text
dist/foabic-fabric-1.1.0.jar            106 KiB   Fabric adapter + API + loader
dist/foabic-neoforge-1.1.0.jar          106 KiB   NeoForge adapter + API + loader
dist/foabic-api-1.1.0.jar                49 KiB   compile-time API only
dist/foabic-example-minimal-1.1.0.jar     7 KiB   v1.1 acceptance mod
dist/foabic-example-chat-1.1.0.jar        3 KiB   chat reply example
```

The platform jars are separate. The API and loader are bundled into each adapter jar, so a user installs one platform jar, not three. There is no universal/merged jar.

`gradlew build` also writes `dist/SHA256SUMS` for release verification.

## Install

### Fabric

```text
mods/
  fabric-api-0.166.2+26.3.jar
  foabic-fabric-1.1.0.jar

foabic-mods/
  my-foabic-mod.jar
```

### NeoForge

```text
mods/
  foabic-neoforge-1.1.0.jar

foabic-mods/
  my-foabic-mod.jar
```

Foabic mods are **not** Fabric or NeoForge mods. They contain `foabic.mod`, not `fabric.mod.json` or `neoforge.mods.toml`. Prefer `foabic-mods/`; `mods/` is also scanned as a convenience fallback.

The adapter must be present on both client and server for a client connection, because content registration runs on both logical sides.

## A 1.1 mod

```java
public final class MyMod implements ModInitializer {
    private static final ResourceLocation BLOCK_ID =
            ResourceLocation.of("mymod", "my_block");
    private static final ResourceLocation ENTITY_ID =
            ResourceLocation.of("mymod", "my_block_entity");
    private static final ResourceLocation SOUND_ID =
            ResourceLocation.of("mymod", "my_sound");

    @Override
    public void onInitialize(ModContext context) {
        Identifier mcId = Identifier.fromNamespaceAndPath("mymod", "my_block");
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, mcId);
        AtomicReference<BlockEntityRegistration<MyEntity>> entityRef = new AtomicReference<>();

        BlockItemRegistration block = context.submitBlock(BLOCK_ID,
                () -> new MyBlock(BlockBehaviour.Properties.of()
                        .setId(blockKey)
                        .strength(1.0F),
                        () -> entityRef.get().get()));

        entityRef.set(context.submitBlockEntity(ENTITY_ID, block.block(),
                (pos, state) -> new MyEntity(entityRef.get().get(), pos, state)));

        context.submit(FoabicRegistries.SOUND_EVENTS, SOUND_ID,
                () -> SoundEvent.createVariableRangeEvent(
                        Identifier.fromNamespaceAndPath("mymod", "my_sound")));

        // Optional per-block override; the metadata texture already covers
        // the normal case.
        context.resources().block(BLOCK_ID, "mymod:block/my_block");

        context.connect(ServerTickSignal.class, signal -> {
            if (signal.tick() == 20L) {
                context.logger().log(System.Logger.Level.INFO, "tick 20");
            }
        });
    }
}
```

`foabic.mod`:

```properties
id=mymod
name=My Mod
version=1.1.0
entrypoint=mymod.MyMod
environment=*
api=^1.1.0
texture=mymod:block/my_block
```

Place the PNG at `assets/mymod/textures/block/my_block.png` inside the Foabic mod jar. Foabic copies it into the generated pack and writes:

- `assets/mymod/blockstates/my_block.json`
- `assets/mymod/models/block/my_block.json`
- `assets/mymod/models/item/my_block.json`

If the texture is missing, a visible generated fallback is used instead of the purple-black missing texture.

## Registration

`FoabicRegistries` exposes four stable views:

```java
FoabicRegistries.BLOCKS
FoabicRegistries.ITEMS
FoabicRegistries.BLOCK_ENTITY_TYPES
FoabicRegistries.SOUND_EVENTS
```

### Block + BlockItem

`context.submitBlock(id, blockFactory)` registers the block and a `BlockItem` with the same id, then queues the item for the Building Blocks creative tab. A custom item factory can be supplied:

```java
context.submitBlock(id, blockFactory, block -> new BlockItem(block, itemProperties));
```

### BlockEntity

`context.submitBlockEntity(id, blockRegistration, factory)` creates the `BlockEntityType` with the block bound into its valid-block set. The factory is evaluated after the block on both platforms. A block's `newBlockEntity` should call the `BlockEntityRegistration.get()` later, not during `onInitialize`.

### SoundEvent

Sound events are ordinary factories in `FoabicRegistries.SOUND_EVENTS`. The actual `sounds.json` and `.ogg` assets remain the mod author's responsibility; Foabic only guarantees registration.

## Signals in 1.1

| Signal | Fabric bridge | NeoForge bridge |
| --- | --- | --- |
| `ServerStartedSignal` | `ServerLifecycleEvents.SERVER_STARTED` | `ServerStartedEvent` |
| `ServerTickSignal` | `ServerTickEvents.END_SERVER_TICK` | `ServerTickEvent.Post` |
| `PlayerInteractBlockSignal` | `UseBlockCallback` | `PlayerInteractEvent.RightClickBlock` |
| `PlayerBreakBlockSignal` | `PlayerBlockBreakEvents.BEFORE` | `BreakBlockEvent` |
| `ChatMessageSignal` | `ServerMessageEvents.CHAT_MESSAGE` | `ServerChatEvent` |

`PlayerInteractBlockSignal` has a mutable `Result` (`PASS`, `SUCCESS`, `CONSUME`, `FAIL`) mapped back to each platform's interaction result. `PlayerBreakBlockSignal` is cancellable; canceling it aborts the Fabric break and cancels the NeoForge `BreakBlockEvent`.

The old `UseBlockSignal` still compiles and is delivered when a listener is registered, but it is deprecated in favor of `PlayerInteractBlockSignal`.

All signals are plain data. They do not expose live `ServerPlayer`, `Level` or `MinecraftServer` objects.

## Defensive loading

Duplicate ids are handled in `ModDiscoverer`:

```text
发现重复的 Mod ID: mymod，已跳过加载 (...); 已加载自 ...
```

The line is logged at `ERROR` level and loading continues with the first jar. Broken metadata or an unreadable jar is also logged and skipped. Byte-identical duplicate copies are no longer a special case; they are skipped by the same rule.

Declared dependencies are checked before any mod entrypoint class is loaded:

- `fabric-api` and `foabic-api` ranges are checked against the actual installed versions;
- a NeoForge server that sees a Fabric-only dependency gets a message saying so;
- unknown required mods name the mod and the missing dependency;
- an undeclared missing class is caught during initialization and reported as a dependency problem rather than a bare `NoClassDefFoundError`.

A missing dependency is a hard failure for the affected load. Foabic reports it clearly instead of continuing into a random crash.

## Verification

The same platform-neutral example jar is installed on both dedicated servers. The final 1.1.0 jars were started twice per platform. Both runs produced the same Foabic lines, including block, item, block-entity and sound registration, the server-start signal, and the tick-20 signal.

```text
FOABIC_REGISTER platform=fabric block=foabic_test:test_block item=foabic_test:test_block
FOABIC_STATUS platform=fabric block_registered=true item_registered=true block_entity_registered=true sound_registered=true
FOABIC_TICK platform=fabric tick=20 players=0
```

A temporary fake-player harness also exercised the real Fabric and NeoForge interaction paths. It confirmed that the block entity is created and that both player signals reach the mod:

```text
SELFTEST_BLOCK_ENTITY=example.MinimalMod$TestBlockEntity
FOABIC_INTERACT player=[Minecraft] block=foabic_test:test_block pos=0,100,0 hand=MAIN_HAND
FOABIC_BREAK player=[Minecraft] block=foabic_test:test_block pos=0,100,0
```

Full evidence:

- [Fabric 1.1 evidence](docs/verification/v11-fabric.txt)
- [NeoForge 1.1 evidence](docs/verification/v11-neoforge.txt)
- [Verification notes](docs/VERIFICATION.md)

The generated pack contents were inspected on both servers. No graphical client was launched, so the rendered texture was not visually captured. The pack is a normal built-in resource pack; Fabric uses `ResourceLoader.registerBuiltinPack`, NeoForge uses `AddPackFindersEvent` with a fixed `PathPackResources`.

## Build

```bat
set JAVA_HOME=C:\path\to\jdk-25
gradlew.bat build
```

The repository ships a Gradle **9.5.1** wrapper. The build uses Java 25 toolchains and produces the five jars in `dist/`.

- `:fabric` uses the non-remapping `net.fabricmc.fabric-loom` plugin for Minecraft 26.3.
- `:neoforge` uses Gradle's `java` plugin against the official NeoForge 26.3.0.115 universal artifact. ModDevGradle is not active because `net.neoforged:neoform-runtime:2.0.31` is unavailable from this network; the adapter code is independent of that choice.
- `:coreTest` runs event, version, new-signal, defensive-loader and resource-generation tests.
- `:dist` writes the five jars and `SHA256SUMS`.

The default Minecraft and NeoForge inputs are read from `.cache/`; see [docs/BUILD.md](docs/BUILD.md).

## Deliberately not in 1.1

- **Mixin.** No Mixin dependency or annotation processor is added.
- **A Gradle build plugin for Foabic mod authors.** The two adapters are still configured manually.
- **Rendering-layer work.** No Sodium/OpenGL integration; resource generation is data only.
- A universal/merged jar.
- Networking, GUI, data generation and a full UI signal library.
- Binary compatibility across 1.x.

## Documentation

- [API](docs/API.md)
- [Fabric adapter](docs/FABRIC.md)
- [NeoForge adapter](docs/NEOFORGE.md)
- [Build inputs](docs/BUILD.md)
- [Verification](docs/VERIFICATION.md)
- [Roadmap](docs/ROADMAP.md)

## License

MIT. See [LICENSE](LICENSE).
