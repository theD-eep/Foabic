# Changelog

## 0.2.0 — 2026-11-21

Beta 0.2: Fabric adapter + registry + working block/signal example.

- Added Fabric Loader 0.20.1 / Fabric API 0.145.1+26.1 adapter.
- Added common client/server Foabic initialization; content mods register on both sides.
- Added `ModContext.submit(Registry<T>, id, value)`.
- Added `ModContext.connect(Class<S>, Consumer<S>)`.
- Added `UseBlockSignal` and Fabric `UseBlockCallback` bridge with result mapping.
- Added typed `FabricRegistries` views for blocks, items, entity types, sounds, particles and block entities.
- Added example block/item mod with assets and right-click action bar.
- Added real Fabric 26.1 client/server verification and screenshot.
- Rewrote docs for the intentionally narrow 0.2 scope.

Explicitly not included: NeoForge, signal standard library, build plugin, Mixin, cross-platform packaging.

## 0.1.0 — 2026-11-20

- Initial standalone agent prototype (not part of the 0.2 Fabric deliverable).
