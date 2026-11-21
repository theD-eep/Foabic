# Foabic smoke-test mod

This is the 0.2 acceptance fixture: a Foabic mod that registers a block/item through `submit`, connects to `UseBlockSignal`, and ships blockstate/model/texture assets so a Fabric client can render it.

## Why it has both manifests

- `foabic.mod` is what the Foabic loader reads.
- `fabric.mod.json` has no entrypoint. Fabric Loader loads the jar only to expose its `assets/` as a built-in resource pack. Foabic still initializes the mod's logic from `foabic.mod`.

The jar belongs in Fabric's `mods/` directory. Pure logic-only Foabic mods can go in `foabic-mods/` instead.

## Behaviour

On `PlayerJoinEvent`, the example places `smoketest:test_block` at `playerPos + (2, 1, 0)`. A right-click then produces:

```text
FOABIC_USE_SIGNAL block=smoketest:test_block pos=... hand=MAIN_HAND
```

and the player's action bar shows:

```text
Foabic: Test Block used
```

## Test

1. Install Fabric Loader 0.20.1 for Minecraft 26.1.
2. Put Fabric API `0.145.1+26.1`, Foabic `foabic-fabric-0.2.0.jar`, and this example jar in both server and client `mods/`.
3. Start the server and connect with the Fabric client.
4. Look at the checkerboard block placed to your east; right-click it.
5. The action bar appears. The server log shows `FOABIC_USE_SIGNAL`.
6. Optionally, server-side simulation:
   ```mcfunction
   /foabictest use
   ```
   It prints `FOABIC_USE_RESULT=Success[...]`.

The block item exists (`smoketest:test_block`), but the example does not add it to a creative tab. Use `/setblock` or `/give @s smoketest:test_block`.
