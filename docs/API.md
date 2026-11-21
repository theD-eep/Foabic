# Foabic 1.0 API

The public surface is intentionally small. `dev.foabic.loader` and the adapter internals are not a compatibility promise.

## Entrypoint

```java
public final class MyMod implements ModInitializer {
    @Override
    public void onInitialize(ModContext context) {
        // register content and connect signals
    }
}
```

`ModContext`:

```java
ModContainer mod();
EventBus events();
Environment environment();
String platform();                 // "fabric" or "neoforge"
System.Logger logger();

<T> Registration<T> submit(Registry<T> registry, ResourceLocation id,
                           Supplier<? extends T> factory);
<S> EventSubscription connect(Class<S> signalType, Consumer<S> listener);
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

The two stable views are:

```java
FoabicRegistries.BLOCKS
FoabicRegistries.ITEMS
```

They are bound by the platform adapter before any Foabic mod is loaded. A mod does not import `FabricRegistries` or `NeoForgeRegistries`.

### Registration handle

```java
Registration<T> registration = context.submit(FoabicRegistries.BLOCKS, id, factory);
registration.id();          // the id passed in
registration.isPresent();   // whether the factory has run
registration.get();         // throws if it has not run yet
```

### Why factories

NeoForge 26.3 freezes the vanilla registries before mod constructors. A `Block` constructor creates an intrusive holder and throws if the registry is frozen. Foabic stores the factory and evaluates it in `RegisterEvent`.

Fabric evaluates the factory immediately during `onInitialize` and registers the value.

A later factory can depend on an earlier registration:

```java
Registration<Block> block = context.submit(FoabicRegistries.BLOCKS, id, blockFactory);
context.submit(FoabicRegistries.ITEMS, id,
        () -> new BlockItem(block.get(), itemProperties));
```

Do not call `get()` in the same initializer before the platform has run the factory. On Fabric it will work; on NeoForge it will throw. This asymmetry is deliberate and visible rather than hidden.

## Signals

v1.0 is a signal kernel plus two concrete signals. It is not a full signal standard library.

### ServerStartedSignal

```java
public final class ServerStartedSignal {
    String platform();
    String minecraftVersion();
    int modCount();
}
```

Posted once per dedicated/integrated server after startup. It is the deterministic v1.0 acceptance signal.

### UseBlockSignal

```java
public final class UseBlockSignal {
    String playerName();
    String dimension();
    ResourceLocation block();
    BlockPos pos();
    Hand hand();          // MAIN_HAND / OFF_HAND
    Result result();      // PASS / SUCCESS / CONSUME / FAIL
    void setResult(Result result);
}
```

Posted once for a server-side block use. Fabric bridges `UseBlockCallback`; NeoForge bridges `PlayerInteractEvent.RightClickBlock`.

### EventBus

```java
EventBus bus = Foabic.events();
EventSubscription sub = bus.subscribe(MySignal.class, signal -> { ... });
sub.close();
```

Semantics:

- synchronous, exact-type dispatch;
- priorities `HIGHEST` … `LOWEST`;
- listener exceptions are isolated and reported to the bus error handler;
- `EventSubscription.close()` removes a listener.

`connect` is a one-line alias for `events().subscribe`.

## Environment

```java
Environment.CLIENT
Environment.SERVER
Environment.UNIVERSAL   // metadata only
```

A mod with `environment=*` loads on both. A dedicated-server acceptance mod is `SERVER` at runtime even if it is universal in metadata.

## Versioning

- `Foabic.VERSION` is `1.0.0`.
- `Foabic.MINECRAFT` is `26.3`.
- `foabic.mod` has `api=^1.0.0`; the loader rejects a mod whose range does not include the runtime version.
- `depends` uses `id@range`, with ranges such as `*`, `^1.0.0`, `>=1.0.0 <2.0.0`.

## Stability

The v1.0 public API is the package `dev.foabic.api` excluding `dev.foabic.api.internal`. Loader and adapter packages may change.

v1.0 promises behavior only on the supported matrix. It does not promise binary compatibility across future 1.x releases, nor compatibility with other Minecraft, Fabric or NeoForge versions.

## Not in 1.0

- Mixin
- Gradle/Maven build plugin
- universal packaging
- full signal standard library
- networking, GUI, models, creative tabs, data generation
- client-side rendering helpers
