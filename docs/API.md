# Foabic 1.2 API

The public API is split into two artifacts:

| Artifact | Contents | Rule |
| --- | --- | --- |
| `foabic-api` | core lifecycle, events, metadata, networking and config contracts | under 15 KiB; no `net.fabricmc.*` / `net.neoforged.*` import |
| `foabic-content` | Minecraft-facing signals, registry views and content helpers | bundled into both platform jars |

Both are compile-time artifacts. A normal user installs one platform jar, which already contains both APIs and the loader.

## Entrypoint

```java
public final class MyMod implements ModInitializer {
    @Override
    public void onInitialize(ModContext context) {
        // register content, resources, config, packets and signals
    }
}
```

`ModContext` extends `FoabicContext`:

```java
// core FoabicContext
ModContainer mod();
EventBus events();
String platform();                 // "fabric" or "neoforge"
System.Logger logger();
ConfigManager config();

<S> FoabicSubscription connect(Class<S> signalType, Consumer<S> listener);

// content ModContext
Environment environment();

<T> Registration<T> submit(Registry<T> registry, ResourceLocation id,
                           Supplier<? extends T> factory);

BlockItemRegistration submitBlock(...);
<T extends BlockEntity> BlockEntityRegistration<T> submitBlockEntity(...);
void addToCreativeTab(CreativeTab tab, Registration<? extends Item> item);
ModResources resources();
```

`Foabic` itself exposes:

```java
String VERSION;          // "1.2.0"
String MINECRAFT;        // "26.3"
int API_VERSION;         // 3
boolean isReady();
String platform();
EventBus events();
Path gameDirectory();
List<ModContainer> mods();
Optional<ModContainer> mod(String id);
```

## Subscriptions

```java
FoabicSubscription subscription =
        context.connect(ServerTickSignal.class, signal -> { ... });

subscription.unsubscribe();
```

`FoabicSubscription` is a functional `AutoCloseable`:

```java
@FunctionalInterface
public interface FoabicSubscription extends AutoCloseable {
    void unsubscribe();
    @Override default void close() { unsubscribe(); }
}
```

`EventBus.subscribe(Class<E>, Consumer<? super E>)` returns the same handle. The bus is synchronous and exact-type, with insertion-order dispatch. `clear()` removes all listeners and nulls their callbacks. Both adapters call `FoabicInternal.onServerStopped()` from their native server-stopped event, so a session does not retain its listeners after shutdown.

## Registry

Four platform-neutral views are bound before any Foabic mod is loaded:

```java
FoabicRegistries.BLOCKS
FoabicRegistries.ITEMS
FoabicRegistries.BLOCK_ENTITY_TYPES
FoabicRegistries.SOUND_EVENTS
```

`Registry<T>` exposes `key`, `register`, `get`, `getOrThrow`, `contains`, `ids`, `stream` and `size`. A registration is a factory:

```java
Registration<Block> block = context.submit(
        FoabicRegistries.BLOCKS, id, blockFactory);
```

Fabric evaluates the factory immediately. NeoForge evaluates it in `RegisterEvent`. Do not call `get()` before the platform registration window.

### Freeze guard

The loader opens the window before mod initialization. Fabric closes it after `FoabicLoader.load()`; NeoForge closes it at `FMLCommonSetupEvent`. A late `submit` logs:

```text
[Foabic] 模组 X 试图在注册表冻结后注册物品 Y，已被拦截。
```

and throws `FoabicRegistryFrozenException` (with `modId()` and `objectId()`). Direct calls through the `FoabicRegistries` proxy are also guarded.

### Blocks, items, block entities and sounds

`submitBlock(id, blockFactory)` registers a block and a `BlockItem` under the same id and queues the item for the Building Blocks tab. Overloads accept an item factory and a different `CreativeTab`.

`submitBlockEntity(id, block, factory)` binds the block to the block-entity type. The type factory runs after the block registration on both platforms; a block's `newBlockEntity` may call `BlockEntityRegistration.get()` later.

`SOUND_EVENTS` registers the sound event only. A mod still ships `sounds.json` and `.ogg` assets for audible custom audio.

## Signals

All signals are plain data. The adapter translates a native event, posts the signal on `Foabic.events()`, and translates a mutable result back where applicable.

| Signal | Data / behavior |
| --- | --- |
| `ServerStartedSignal` | platform, Minecraft version, mod count |
| `ServerTickSignal` | platform, tick, player count |
| `PlayerInteractBlockSignal` | player, dimension, block, pos, hand, mutable `PASS`/`SUCCESS`/`CONSUME`/`FAIL` |
| `PlayerBreakBlockSignal` | player, dimension, block, pos, cancellable |
| `ChatMessageSignal` | player, message, `replyAs(name, text)` |
| `UseBlockSignal` | deprecated v1.0 compatibility signal |

The adapters emit interact and break signals on the logical server only. `PlayerBreakBlockSignal.cancel()` aborts the Fabric break and cancels the NeoForge `BreakBlockEvent`.

## Networking

```java
public interface FoabicPacket {
    byte[] encode();
    void decode(byte[] data);
    default void handle() { }
}
```

Register and send:

```java
NetworkManager.register(PingPacket.class, PingPacket::new);
NetworkManager.sendToServer(new PingPacket("hello"));
NetworkManager.sendToClient(serverPlayer, new PingPacket("hello"));
```

`NetworkManager.receive(type, data)` is called by the adapters on the game thread. Unknown or malformed packets are logged and dropped. The maximum payload is 1 MiB.

- Fabric: `PayloadTypeRegistry.serverboundPlay/clientboundPlay`, `ServerPlayNetworking`, `ClientPlayNetworking`.
- NeoForge 26.3: `RegisterPayloadHandlersEvent` and `PayloadRegistrar.playBidirectional`. This is the supported replacement for the removed `SimpleChannel` API.

`sendToClient(Object player, ...)` requires the active adapter's `ServerPlayer`.

## Config

```java
public interface ConfigManager {
    <T> T register(Class<T> type);
    <T> void save(T config);
}
```

```java
MyConfig config = context.config().register(MyConfig.class);
config.greeting = "hi";
context.config().save(config);
```

Files live at `config/foabic/<mod-id>/<Type>.json` and use pretty-printed Gson JSON. Records and no-arg POJOs are supported. A malformed file is renamed to `.broken` and regenerated. `register` caches instances by class.

## Metadata

`foabic.mod.json` is the canonical metadata entry:

```json
{
  "schemaVersion": 1,
  "id": "mymod",
  "version": "1.2.0",
  "name": "My Mod",
  "description": "A small Foabic mod.",
  "authors": ["Me"],
  "license": "MIT",
  "environment": "*",
  "entrypoint": "mymod.MyMod",
  "api": "^1.2.0",
  "icon": "icon.png",
  "texture": "mymod:block/my_block",
  "depends": {
    "foabic": ">=1.2.0"
  },
  "optionalDepends": {
    "other_mod": "^1.0.0"
  }
}
```

- `id`: lowercase `[a-z][a-z0-9_-]{0,63}`; `foabic` and `minecraft` are reserved.
- `entrypoint` or `entrypoints`.
- `environment`: `*`, `client` or `server`.
- `icon`: optional PNG resource path inside the jar, such as `icon.png` or
  `assets/mymod/icon.png`. Foabic Loom translates it to Fabric's `icon` and
  NeoForge's `logoFile`; when omitted it auto-detects `icon.png` or
  `assets/<id>/icon.png` under `src/main/resources`.
- `texture`: optional namespaced texture id.
- `depends` / `optionalDepends`: JSON object of id to version range.
- `api`: version range for the core API.

The loader still accepts a legacy properties `foabic.mod` for migration; new mods should use JSON.

## Defensive behavior

- Duplicate ids log `发现重复的 Mod ID: ...，已跳过加载` at `ERROR` and continue with the first jar.
- Malformed metadata and unreadable jars are logged and skipped.
- Missing required dependencies fail before entrypoint loading with `DependencyException`.
- A missing class during entrypoint initialization is wrapped in a message that points at the dependency declaration.
- A late content registration is intercepted with the freeze guard above.

## Versioning

- `Foabic.VERSION` is `1.2.0`.
- `Foabic.API_VERSION` is `3`.
- `Foabic.MINECRAFT` is `26.3`.
- Public versions/ranges are strings; the loader validates and evaluates them internally.
- `foabic-api` is a separate core jar; `foabic-content` is the Minecraft-facing artifact. Both are packaged in platform jars.

## Stability

The supported public API is `dev.foabic.api` excluding `dev.foabic.api.internal`. Loader and adapter packages may change. Foabic 1.2 does not promise binary compatibility across 1.x releases.

## Not in 1.2

- Mixin
- rendering-layer integration
- universal packaging
- GUI/screen API
- a live client networking/rendering harness
