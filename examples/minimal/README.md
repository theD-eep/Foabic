# Foabic minimal example

This example targets Foabic 1.2 on Minecraft 26.3. It demonstrates the end-to-end
path:

- one block and its automatic `BlockItem`;
- one block-entity type bound to that block;
- one custom sound event;
- a generated block/item model from the `texture` field in `foabic.mod.json`;
- `ServerStartedSignal`, `ServerTickSignal`, `PlayerInteractBlockSignal` and
  `PlayerBreakBlockSignal`;
- JSON config loading via `context.config().register(TestConfig.class)`;
- packet registration via `NetworkManager.register(...)`;
- explicit `FoabicSubscription.unsubscribe()`;
- a late `submit` that is intercepted by the registry freeze guard;
- a launcher icon declared as `"icon": "icon.png"` and packaged at the jar root.

Build the example jar with:

```bat
gradlew.bat :examples:minimal:jar
```

Copy `examples/minimal/build/libs/foabic-example-minimal-1.2.0.jar` into
`foabic-mods/` on either adapter. The PNG is bundled at
`examples/minimal/src/main/resources/assets/foabic_test/textures/block/test_block.png`,
and the metadata is `examples/minimal/src/main/resources/foabic.mod.json`.

Expected marker lines include:

```text
FOABIC_CONFIG platform=... greeting=hello-v1.2
FOABIC_NETWORK platform=... packet=example.MinimalMod$StatusPacket
FOABIC_FROZEN_GUARD platform=... caught=true object=foabic_test:late_item
FOABIC_UNSUBSCRIBE platform=... tick=40
FOABIC_STOP_CLEAN platform=... tick_listeners=false
```
