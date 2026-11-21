# Roadmap

## 1.0 — current

Shipped and verified:

- Minecraft 26.3, Java 25
- Fabric Loader 0.20.1 / Fabric API 0.166.2+26.3 adapter
- NeoForge 26.3.0.115 adapter
- platform-neutral `FoabicRegistries.BLOCKS` / `ITEMS`
- factory-based `submit` with `Registration<T>`
- synchronous signal bus
- `ServerStartedSignal`
- `UseBlockSignal` with Fabric and NeoForge bridges
- minimal example that starts on both platforms, registers a block + item, receives a signal, and repeats identically
- Gradle 9.5.1 wrapper build with repeatability verification

The definition of "stable" for 1.0 is intentionally narrow: one minimal mod, both platforms, one object, one signal, repeatable output. Everything below is deliberately outside that contract.

## 1.1 — next

- Mixin support, including a documented `foabic.mixins.json` / adapter integration path
- a Gradle build plugin for Foabic mod authors
- universal/cross-platform packaging (one artifact that selects an adapter at runtime, or an equivalent documented distribution)
- compatibility test matrix for at least one patch release in the 26.3 line

These are the next foundation layers. They should not be added to 1.0.

## Later

- a real signal standard library (more than the two concrete signals)
- networking
- GUI/screen APIs
- block/item model and creative-tab helpers
- data generation
- 26.4+ compatibility
- binary compatibility commitments across 1.x

## Non-goals

- replacing Fabric Loader, Fabric API or NeoForge
- sandboxing mods
- a cross-loader mod format independent of Forge/Fabric metadata
- a large annotation/reflection framework
- bundling Minecraft, Fabric API or NeoForge

## Scope rule

Do not add a deferred subsystem to a release line before the lower-level acceptance path is verified on every supported platform. v1.0 exists to make v1.1 debuggable, not to look complete.
