# Foabic minimal acceptance mod

This jar is intentionally tiny and platform-neutral. It:

1. registers `foabic_test:test_block` and its block item through
   `FoabicRegistries.BLOCKS` / `ITEMS`;
2. connects to `ServerStartedSignal` and logs `FOABIC_SIGNAL` plus
   `FOABIC_STATUS block_registered=true item_registered=true`;
3. connects to `UseBlockSignal` and returns `SUCCESS` for a real right-click.

It contains no `fabric.mod.json` and no `neoforge.mods.toml`: it is a Foabic
mod, not a loader mod. Install it in `foabic-mods/`.

Build with the repository's Gradle wrapper: `gradlew.bat build` (or `./gradlew build`).
