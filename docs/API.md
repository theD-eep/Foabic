# Foabic 1.1 API

The public surface is intentionally small. `dev.foabic.loader` and the adapter internals are not a compatibility promise.

## Entrypoint

```java
public final class MyMod implements ModInitializer {
    @Override
    public void onInitialize(ModContext context) {
        // register content, resources and signals
    }
}
```

`ModContext` in 1.1:

```java
ModContainer mod();
EventBus events();
Environment environment();
String platform();                 // "fabric" or "neoforge"
System.Logger logger();

<T> Registration<T> submit(Registry<T> registry, ResourceLocation id,
                           Supplier<? extends T> factory);
<S> EventSubscription connect(Class<S> signalType, Consumer<S> listener);

BlockItemRegistration submitBlock(ResourceLocation id,
                                  Supplier<? extends Block> blockFactory);
BlockItemRegistration submitBlock(ResourceLocation id,
                                  Supplier<? extends Block> blockFactory,
                                  Function<Block, ? extends Item> itemFactory);
BlockItemRegistration submitBlock(ResourceLocation id,
                                  Supplier<? extends Block> blockFactory,
                                  Function<Block, ? extends Item> itemFactory,
                                  CreativeTab tab);

<T extends BlockEntity> BlockEntityRegistration<T> submitBlockEntity(
        ResourceLocation id, Registration<? extends Block> block,
        BlockEntityType.BlockEntitySupplier<T> factory);
<T extends BlockEntity> BlockEntityRegistration<T> submitBlockEntity(
        ResourceLocation id, List<? extends Registration<? extends Block>> blocks,
        BlockEntityType.BlockEntitySupplier<T> factory);

void addToCreativeTab(CreativeTab tab, Registration<? extends Item> item);
ModResources resources();
```

## Registry

```java
public interface Registry<T> {
    ResourceLocation key();
    Registration<T> register(ResourceLocation id, Supplier<? extends T> factory);
    Optional<T> get(ResourceLocation id);
    T getOrThrow(ResourceLocation id);
    boolean contains(ResourceLocation id);
    List<ResourceLocation> ids();
    Stream<T> stream();
    int size();
}
```

Four stable views are bound before any Foabic mod is loaded:

```java
FoabicRegistries.BLOCKS
FoabicRegistries.ITEMS
FoabicRegistries.BLOCK_ENTITY_TYPES
FoabicRegistries.SOUND_EVENTS
```

A mod does not import `FabricRegistries` or `NeoForgeRegistries`.

### Registration handle

```java
Registration<T> registration = context.submit(FoabicRegistries.BLOCKS, id, factory);
registration.id();
registration.isPresent();
registration.get();   // throws if the platform has not evaluated it yet
```

NeoForge 26.3 freezes the vanilla registries before mod constructors. A `Block`
constructor creates an intrusive holder and throws if the registry is frozen.
Foabic stores the factory and evaluates it in `RegisterEvent`. Fabric evaluates
the factory immediately during `onInitialize`.

A later factory can depend on an earlier registration:

```java
Registration<Block> block = context.submit(FoabicRegistries.BLOCKS, id, blockFactory);
context.submit(FoabicRegistries.ITEMS, id,
        () -> new BlockItem(block.get(), itemProperties));
```

Do not call `get()` in the same initializer before the platform has run the
factory. On Fabric it will work; on NeoForge it will throw. `submitBlock` and
`submitBlockEntity` handle this ordering for the normal cases.

## Blocks and BlockItems

`submitBlock` registers both objects under the same id and adds the item to the
Building Blocks tab:

```java
BlockItemRegistration block = context.submitBlock(BLOCK_ID,
        () -> new Block(BlockBehaviour.Properties.of()
                .setId(blockKey)
                .strength(1.0F)));
```

Custom item:

```java
context.submitBlock(BLOCK_ID, blockFactory,
        block -> new BlockItem(block, itemProperties));
```

Custom tab:

```java
context.submitBlock(BLOCK_ID, blockFactory,
        block -> new BlockItem(block, itemProperties),
        CreativeTab.FUNCTIONAL_BLOCKS);
```

The result exposes `Registration<Block> block()` and `Registration<Item> item()`.
The item is queued in `FoabicCreativeTabs` and inserted when the platform builds
creative tab contents.

## Block entities

```java
AtomicReference<BlockEntityRegistration<MyEntity>> entityRef = new AtomicReference<>();

BlockItemRegistration block = context.submitBlock(BLOCK_ID,
        () -> new MyBlock(properties, () -> entityRef.get().get()));

entityRef.set(context.submitBlockEntity(ENTITY_ID, block.block(),
        (pos, state) -> new MyEntity(entityRef.get().get(), pos, state)));
```

`BlockEntityRegistration<T>`:

```java
Registration<BlockEntityType<?>> type();
List<Registration<? extends Block>> blocks();
BlockEntityType<T> get();
boolean isPresent();
```

The block's `newBlockEntity` runs later (when the world creates the block
entity), so it is safe to call `get()` there. Do not call it in the constructor
of the block itself.

A block-entity type can bind more than one block with the `List` overload.

## Sound events

```java
Registration<SoundEvent> sound = context.submit(
        FoabicRegistries.SOUND_EVENTS, SOUND_ID,
        () -> SoundEvent.createVariableRangeEvent(soundIdentifier));
```

Foabic registers the event only. A mod that wants an audible custom sound still
ships `assets/<namespace>/sounds.json` and the `.ogg` files.

## Creative tabs

```java
public enum CreativeTab {
    BUILDING_BLOCKS, NATURAL_BLOCKS, FUNCTIONAL_BLOCKS, REDSTONE_BLOCKS,
    TOOLS_AND_UTILITIES, COMBAT, FOOD_AND_DRINKS, INGREDIENTS,
    SPAWN_EGGS, OP_BLOCKS
}
```

`context.addToCreativeTab(tab, itemRegistration)` adds an already registered
item. The queue is drained by `CreativeModeTabEvents.modifyOutputEvent` on
Fabric and `BuildCreativeModeTabContentsEvent` on NeoForge.

## Generated resources

The normal path is metadata-only:

```properties
texture=mymod:block/my_block
```

For every block registered through `context.submitBlock`, Foabic generates:

```text
assets/mymod/blockstates/my_block.json
assets/mymod/models/block/my_block.json
assets/mymod/models/item/my_block.json
```

and copies `assets/mymod/textures/block/my_block.png` from the mod jar.

The code path is one line:

```java
context.resources().block(BLOCK_ID, "mymod:block/my_block");
```

Custom model parent (it must accept the generated `all` texture key):

```java
context.resources().block(BLOCK_ID, "mymod:block/my_block", "minecraft:block/cube_mirrored_all");
```

Item override:

```java
context.resources().item(ITEM_ID, ResourceLocation.of("mymod", "item/my_item"));
```

Rules:

- A declared texture is copied from any Foabic mod jar.
- If the PNG is missing, a visible generated fallback texture is used instead of
  the purple-black missing texture.
- If a block has no `texture=` and no explicit request, Foabic checks whether the
  mod jar already has a blockstate. If it does, Foabic leaves it alone.
  Otherwise it generates a fallback cube-all model.
- The generated files go to `.foabic/generated-resources` and are exposed as an
  always-on built-in resource pack:
  - Fabric: `ResourceLoader.registerBuiltinPack` with a synthetic mod root;
  - NeoForge: `AddPackFindersEvent` with a fixed `PathPackResources`.
- No model JSON is parsed or rendered by Foabic. This is data generation only.

## Signals

v1.1 has five current concrete signals, plus the deprecated
`UseBlockSignal` compatibility signal. All are plain data and are dispatched by
the exact-type synchronous `EventBus`.

### ServerStartedSignal

```java
String platform();
String minecraftVersion();
int modCount();
```

Posted once per dedicated/integrated server after startup.

### ServerTickSignal

```java
String platform();
long tick();
int playerCount();
```

Posted at the end of every server tick. The 1.1 acceptance example logs at
`tick == 20`.

### PlayerInteractBlockSignal

```java
String playerName();
String dimension();
ResourceLocation block();
BlockPos pos();
Hand hand();          // MAIN_HAND / OFF_HAND
Result result();      // PASS / SUCCESS / CONSUME / FAIL
void setResult(Result result);
```

Fabric bridges `UseBlockCallback`; NeoForge bridges
`PlayerInteractEvent.RightClickBlock`. The signal is filtered to the logical
server and to `ServerPlayer`.

`UseBlockSignal` is the deprecated v1.0 name. It still compiles and is delivered
when a listener is registered, but new code should use
`PlayerInteractBlockSignal`.

### PlayerBreakBlockSignal

```java
String playerName();
String dimension();
ResourceLocation block();
BlockPos pos();
void cancel();
boolean isCancelled();
```

Fabric bridges `PlayerBlockBreakEvents.BEFORE`; NeoForge bridges
`BreakBlockEvent`. Cancelation aborts the break.

### ChatMessageSignal

```java
String playerName();
String message();
void replyAs(String name, String text);
```

Fabric bridges `ServerMessageEvents.CHAT_MESSAGE`; NeoForge bridges
`ServerChatEvent`. `replyAs` sends a server system line formatted as
`<name> text`; it does not fabricate a signed player chat packet.

### EventBus

```java
EventBus bus = Foabic.events();
EventSubscription sub = bus.subscribe(MySignal.class, signal -> { ... });
sub.close();
```

Semantics:

- synchronous, exact-type dispatch;
- priorities `HIGHEST` through `LOWEST`;
- listener exceptions are isolated and reported to the bus error handler;
- `EventSubscription.close()` removes a listener;
- a `Cancellable` signal stops later listeners once cancelled.

## Metadata

`foabic.mod` is UTF-8 properties. A UTF-8 BOM is tolerated.

```properties
id=mymod
name=My Mod
version=1.1.0
entrypoint=mymod.MyMod
environment=*
api=^1.1.0
texture=mymod:block/my_block
depends=fabric-api@>=0.166.2
optional=other_mod@^1.0.0
```

- `id`: lowercase, `[a-z][a-z0-9_-]{0,63}`; `foabic` and `minecraft` are reserved.
- `texture`: optional; a namespaced texture id. If no namespace is given, the
  mod id is used. `.png` and `textures/` prefixes are accepted by the generator.
- `depends`: comma-separated `id@range`; checked before entrypoint loading.
- `optional`: same syntax; missing optional dependencies are ignored.
- `api`: defaults to `^1.0.0`; Foabic 1.1.0 satisfies old `^1.0.0` mods.

## Defensive behavior

- A duplicate id logs `发现重复的 Mod ID: ...，已跳过加载` at `ERROR` level and
  continues with the first jar.
- A malformed metadata file or unreadable jar is logged and skipped.
- A missing required dependency raises one `DependencyException` naming the mod,
  the dependency and the version that is actually installed.
- A missing class during entrypoint initialization is wrapped in a clear
  `ModLoadException` that points at the dependency declaration.

## Versioning

- `Foabic.VERSION` is `1.1.0`.
- `Foabic.API_VERSION` is `2`.
- `Foabic.MINECRAFT` is `26.3`.
- `foabic.mod` has `api=^1.1.0` in new mods; `^1.0.0` remains valid.
- `depends` uses `id@range`, with ranges such as `*`, `^1.0.0`,
  `>=1.0.0 <2.0.0`.

## Stability

The supported public API is the package `dev.foabic.api` excluding
`dev.foabic.api.internal`. Loader and adapter packages may change.

Foabic 1.1 promises behavior only on the supported matrix. It does not promise
binary compatibility across future 1.x releases, or compatibility with other
Minecraft, Fabric or NeoForge versions.

## Not in 1.1

- Mixin
- a Gradle/Maven build plugin for Foabic mod authors
- a universal/merged jar
- networking, GUI, data generation and a full signal library
- rendering-layer integration
- client rendering tests
