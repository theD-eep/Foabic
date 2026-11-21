# Changelog

## 1.0.0 — 2026-11-21

First cross-platform Foabic release, scoped to a verified minimal contract on Minecraft 26.3.

### Added

- Minecraft 26.3 support; Java 25.
- Fabric Loader 0.20.1 / Fabric API 0.166.2+26.3 adapter.
- NeoForge 26.3.0.115 adapter.
- Platform-neutral `FoabicRegistries.BLOCKS` and `ITEMS`.
- Factory-based registration with `Registration<T>`:
  - Fabric evaluates immediately;
  - NeoForge evaluates in `RegisterEvent`, because 26.3 freezes registries before mod construction.
- `ServerStartedSignal`, the deterministic acceptance signal.
- `UseBlockSignal` with Fabric `UseBlockCallback` and NeoForge `PlayerInteractEvent.RightClickBlock` bridges.
- Minimal example mod registering one block and one block item.
- Gradle 9.5.1 wrapper build:
  - `:fabric` uses the non-remapping `net.fabricmc.fabric-loom` 1.14.10 plugin;
  - `:neoforge` compiles against the official NeoForge 26.3.0.115 universal artifact with Gradle's `java` plugin;
  - `:coreTest` runs the platform-neutral tests.
- Repeatability check: two runs per platform of the Gradle-built jars, exact Foabic output comparison.

### Changed from 0.2

- `submit` now takes a `Supplier<? extends T>` and returns `Registration<T>`, not a pre-built value.
- `UseBlockSignal` is plain data (`playerName`, `dimension`) instead of live `Player`/`Level` wrappers.
- Commands, scheduler, config, resources and the old server event records were removed from the 1.0 surface.
- Fabric no longer scans `mods/`; Foabic mods live in `foabic-mods/`.
- Minecraft support moved from 26.1 to 26.3.

### Verified

- Fabric 26.3 dedicated server: `FOABIC_REGISTER`, `FOABIC_SIGNAL`, `FOABIC_STATUS block_registered=true item_registered=true`.
- NeoForge 26.3.0.115 dedicated server: same observations.
- Two runs per platform produce identical Foabic observations.
- Core tests: events, failure isolation, versions, use-block signal.

### Not included

Mixin, a Gradle plugin for Foabic mod authors, universal packaging, a full signal library, networking, GUI, resources and data generation. See [docs/ROADMAP.md](docs/ROADMAP.md).

## 0.2.0 — 2026-11-21

Fabric-only beta for Minecraft 26.1: Fabric Loader 0.20.1 / Fabric API 0.145.1+26.1, `submit`, `connect`, `UseBlockSignal`, typed Fabric registries, example block/assets, real client/server verification.

## 0.1.0 — 2026-11-20

Initial standalone prototype. Not part of the 1.0 lineage.
