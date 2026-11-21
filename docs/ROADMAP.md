# Roadmap

## 1.2 — current

Shipped and verified on Minecraft 26.3:

- lifecycle-safe signals: `FoabicSubscription`, explicit `unsubscribe()`, session-bus cleanup on both adapters;
- registry freeze guard with `FoabicRegistryFrozenException` and the required Chinese `ERROR` line;
- a 14,420-byte core API jar and a no-platform-imports contract check;
- `foabic.mod.json` canonical metadata and legacy `foabic.mod` migration support;
- the Foabic Loom Gradle plugin: one source tree to `dist/mod-fabric.jar` and `dist/mod-neoforge.jar`, with platform metadata injection;
- cross-platform `FoabicPacket` / `NetworkManager` over Fabric play payloads and NeoForge `RegisterPayloadHandlersEvent`;
- JSON config with `context.config().register(Type.class)` / `save(instance)`;
- launcher icon packaging and Fabric/NeoForge metadata translation;
- two final dedicated-server runs per platform, plus plugin-generated jar loads on both.

## 1.3 candidates

Not committed:

- a larger signal library (join/leave, block place, entity events, server stopping);
- a small command and scheduling API;
- broader block-entity helpers (ticker binding, menu providers);
- live client networking verification and a friendly client packet context;
- data-pack resource helpers alongside the client resource pack;
- a compatibility matrix for another 26.3 patch release.

## Later

- universal/cross-platform packaging;
- GUI/screen API;
- 26.4+ compatibility;
- binary compatibility commitments across 1.x.

## Non-goals for 1.2

- **Mixin.** No Mixin dependency, annotation processor or configuration.
- **Rendering-layer integration.** No Sodium/OpenGL code; the resource work is data generation only.
- replacing Fabric Loader, Fabric API or NeoForge;
- sandboxing mods;
- bundling Minecraft, Fabric API or NeoForge.

## Scope rule

Do not add a deferred subsystem to a release line before the lower-level acceptance path is verified on every supported platform. v1.2 exists to make v1.3 debuggable, not to look complete.
