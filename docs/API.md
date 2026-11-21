# Foabic Beta 0.2 API

The public surface is intentionally small. Internal loader and Fabric bridge classes are not a compatibility promise.

## Entrypoint

```java
public final class MyMod implements ModInitializer {
    @Override
    public void onInitialize(ModContext context) {
        // submit content, connect signals, register commands
    }
}
```

`ModContext`:

```java
ModContainer mod();
EventBus events();
Scheduler scheduler();
CommandRegistry commands();
Environment environment();
System.Logger logger();
Config config();
ModResources resources();

<T> T submit(Registry<T> registry, ResourceLocation id, T value);
<S> EventSubscription connect(Class<S> signalType, Consumer<S> listener);
```

## submit

```java
context.submit(FabricRegistries.BLOCKS, id, block);
context.submit(FabricRegistries.ITEMS, id, item);
```

`submit` delegates to a generic `dev.foabic.api.registry.Registry<T>`:

```java
ResourceLocation key();
<V extends T> V register(ResourceLocation id, V value);
Optional<T> get(ResourceLocation id);
T getOrThrow(ResourceLocation id);
boolean contains(ResourceLocation id);
List<ResourceLocation> ids();
Stream<T> stream();
int size();
```

Registration timing matters. On Fabric, perform registration during `onInitialize`; the adapter initializes Foabic content mods on both client and server before registries freeze.

`FabricRegistries` fields:

- `BLOCKS`
- `ITEMS`
- `ENTITY_TYPES`
- `SOUND_EVENTS`
- `PARTICLE_TYPES`
- `BLOCK_ENTITY_TYPES`

## connect and UseBlockSignal

```java
context.connect(UseBlockSignal.class, signal -> {
    if (signal.block().equals(ResourceLocation.parse("mymod:button"))) {
        signal.player().sendActionBar("clicked");
        signal.setResult(UseBlockSignal.Result.SUCCESS);
    }
});
```

`UseBlockSignal` fields:

```java
Player player();
Level level();
ResourceLocation block();
BlockPos pos();          // Foabic's MC-free x/y/z record
Hand hand();             // MAIN_HAND / OFF_HAND
Result result();         // PASS / SUCCESS / CONSUME / FAIL
void setResult(Result result);
```

The Fabric adapter filters to the server side, translates the result back to Fabric's `UseBlockCallback` result, and posts exactly one signal per server-side block use. `connect` is a one-line alias for `events().subscribe`, not a general signal framework.

## Events

- `ServerStartingEvent`
- `ServerStartedEvent`
- `ServerTickEvent` / `ServerTickEndEvent`
- `LevelTickEvent` / `LevelTickEndEvent`
- `PlayerJoinEvent` / `PlayerLeaveEvent`
- `ServerStoppingEvent` / `ServerStoppedEvent`

Event bus semantics:

- synchronous, exact-type dispatch;
- priorities `HIGHEST` … `LOWEST`;
- listener exceptions are isolated and logged;
- `EventSubscription.close()` removes a listener.

These events are server-side. `UseBlockSignal` is the only client-relevant signal in 0.2, and it is delivered on the server.

## Commands

```java
context.commands().literal("home")
        .requires(source -> source.hasPermission(2))
        .executes(command -> {
            command.source().sendMessage("home");
            return 1;
        });
```

Supported argument types: `STRING`, `WORD`, `GREEDY_STRING`, `INTEGER`, `LONG`, `FLOAT`, `DOUBLE`, `BOOLEAN`. Commands are translated to Brigadier through Fabric's command registration callback.

## Scheduler

```java
context.scheduler().schedule(20, () -> { /* one second */ });
Task task = context.scheduler().scheduleRepeating(0, 20, self -> { /* ... */ });
task.cancel();
```

Tick-based, server-thread-only, fixed period, no catch-up.

## Config and resources

```java
Config config = context.config();          // .foabic/config/<id>.properties
config.set("enabled", true);
config.save();

byte[] icon = context.resources().read("assets/example/icon.png");
```

`ModResources` is scoped to the current mod jar and rejects path traversal.

## Stability

`0.2.x` is a Beta line for Minecraft 26.1 + Fabric. Internal classes, loader behavior, and the example may change in 0.3. The public API is not yet frozen.
