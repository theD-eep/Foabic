# Entities

Foabic 1.3 adds entity-type registration and a small `FoabicEntity` base. The
data-sync mechanism is vanilla `SynchedEntityData`: a field declared with
`EntityDataAccessor` is sent by the entity tracker to every client, without a
Foabic packet.

## Registration

```java
context.submitEntity(
        ResourceLocation.of("mymod", "feather_wisp"),
        EntityType.Builder.of(FeatherWisp::new, MobCategory.MISC)
                .sized(0.5F, 0.5F)
                .clientTrackingRange(8));
```

`FoabicRegistries.ENTITY_TYPES` is a platform-neutral view bound to
`BuiltInRegistries.ENTITY_TYPE` on Fabric and flushed through NeoForge's
`RegisterEvent`. The builder is evaluated in the platform registration window,
so the same deferred-registration rule as blocks and items applies.

## The base class

```java
public final class FeatherWisp extends FoabicEntity {

    private static final EntityDataAccessor<Integer> CHARGE =
            define(FeatherWisp.class, EntityDataSerializers.INT);

    public FeatherWisp(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineData(SynchedEntityData.Builder builder) {
        builder.define(CHARGE, 0);
    }

    public int charge() {
        return synced(CHARGE);
    }

    public void setCharge(int value) {
        setSynced(CHARGE, value);
    }
}
```

`FoabicEntity` implements the four abstract `Entity` methods with no-op
save/load and non-damageable defaults:

- `defineSynchedData` calls `defineData(builder)`;
- `readAdditionalSaveData` / `addAdditionalSaveData` are no-ops unless overridden;
- `hurtServer` returns `false`.

## What is synchronised

`setSynced` delegates to `getEntityData().set(accessor, value)`. The vanilla
entity tracker sends dirty values to clients at the entity type's update
interval. This is the same path used for vanilla health, pose and custom names.

- No `FoabicPacket` is involved.
- A client that starts tracking the entity receives non-default values.
- The entity must be spawned on the server (`EntityType.spawn` or
  `ServerLevel.addFreshEntity`); Foabic does not add a spawn egg or spawn
  placement automatically.

## Persistence

Override `addAdditionalSaveData` / `readAdditionalSaveData` for fields that must
survive a restart. Synced data is not automatically persisted. Example:

```java
@Override
protected void addAdditionalSaveData(ValueOutput output) {
    output.putInt("charge", charge());
}

@Override
protected void readAdditionalSaveData(ValueInput input) {
    setCharge(input.getIntOr("charge", 0));
}
```

On the client, `readAdditionalSaveData` is not the sync path; `SynchedEntityData`
is. Use both when a value must persist and be visible.

## Rendering is still the mod's job

Foabic registers and synchronises the entity; it deliberately does not add a
client renderer. A custom entity that is spawned without a platform renderer
will be invisible (and may log a missing-renderer warning) even though its
server logic and data sync work. Register a renderer in the same way as any
Fabric/NeoForge mod.

The survival example registers `foabic_survival:feather_wisp` to prove the
entity-type path. It does not spawn the wisp in the dedicated-server test and
does not ship a renderer.

## Limits

- No attribute registration, spawn placement, spawn egg or AI builder.
- No client renderer/model layer.
- No automatic persistence of synced data.
- The entity type id is registered on both logical sides; a client must have the
  same Foabic mod and adapter installed.
