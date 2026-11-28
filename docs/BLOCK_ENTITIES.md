# Block entities and data synchronisation

Foabic 1.3 adds a block-entity base with a small synchronised-field system. The
goal is that a survival block can store a counter, a mode or a short string,
survive a world save, and show the same value on every client without the mod
writing packet code.

## Registration

The block entity type is still registered through `ModContext`:

```java
AtomicReference<BlockEntityRegistration<CollectorBlockEntity>> type =
        new AtomicReference<>();

BlockItemRegistration block = context.submitBlock(id,
        () -> new CollectorBlock(properties, () -> type.get().get()));

type.set(context.submitBlockEntity(id, block.block(),
        (pos, state) -> new CollectorBlockEntity(type.get().get(), pos, state)));
```

`BlockEntityRegistration.get()` is only valid after the platform registration
window. On Fabric that is immediate; on NeoForge it is the `RegisterEvent`.
Store the registration in an `AtomicReference` and call `get()` from the block's
`newBlockEntity`, never from the `submitBlock` factory.

## The base class

```java
public final class CollectorBlockEntity extends FoabicBlockEntity {

    private final SyncedInt feathers = syncedInt("feathers", 0);

    public CollectorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void tick() {
        if (level == null || level.isClientSide()) return;
        if (level.getGameTime() % 200L == 0L) {
            feathers.increment();
        }
    }

    public int feathers() {
        return feathers.get();
    }
}
```

`FoabicBlockEntity` adds:

| Field helper | Stored as | Sync |
| --- | --- | --- |
| `syncedInt(key, default)` | `ValueOutput.putInt` | yes |
| `syncedLong` | `putLong` | yes |
| `syncedFloat` | `putFloat` | yes |
| `syncedBoolean` | `putBoolean` | yes |
| `syncedString` | `putString` | yes |

Each `set` marks the block entity dirty and calls `sync()`. `sync()` sends
`ServerLevel.sendBlockUpdated(..., Block.UPDATE_CLIENTS)`. The vanilla
`ClientboundBlockEntityDataPacket` then carries `getUpdateTag()`, which is the
saved data without metadata. On the client, the normal `loadAdditional` path
reads the same fields.

The save path is the vanilla `ValueInput`/`ValueOutput` API; no `CompoundTag`
code is required for the built-in fields. Subclasses can override
`saveAdditional` / `loadAdditional` and call `super` for extra data.

## Ticking

Use `FoabicBlock` instead of `BaseEntityBlock`:

```java
public final class CollectorBlock extends FoabicBlock {
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CollectorBlockEntity(type.get(), pos, state);
    }
}
```

`FoabicBlock.getTicker` dispatches to `FoabicBlockEntity.tick()` on both sides.
The example checks `level.isClientSide()` before mutating server state. If a
custom ticker is needed, override `getTicker`; `FoabicBlockEntity.ticker()` is
also available as a static helper.

## Synchronisation rules

- **Server to client.** `Synced*` fields are authoritative on the server. A
  client that receives a block update reads the same values.
- **Client to server.** The base does not send client changes. Use a packet or a
  menu for client requests; do not assume a client-side `set` reaches the
  server.
- **Chunk load.** `getUpdateTag` includes all declared fields, so a client that
  loads the chunk gets the current state even if it missed earlier updates.
- **Persistence.** `saveAdditional` writes the fields to the chunk; a server
  restart restores them.
- **Removal.** No custom cleanup is needed for the field list; it is owned by
  the block entity.

## Capacity and limits

The field system is intentionally small: integers, longs, floats, booleans and
strings. For inventories, item stacks or large structures, use vanilla
`Container`/`ItemStack` code and serialize those explicitly. The base still
works as a normal `BlockEntity`; the sync helpers are additive.

`ValueOutput` strings are not length-limited by Foabic. Keep them short; every
client update carries the full update tag.
