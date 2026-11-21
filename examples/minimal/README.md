# Foabic minimal example

This example targets Foabic 1.1 on Minecraft 26.3. It demonstrates the smallest
useful end-to-end path:

- one block and its automatic `BlockItem`;
- one block-entity type bound to that block;
- one custom sound event;
- a generated block/item model from `texture=foabic_test:block/test_block`;
- `ServerStartedSignal`, `ServerTickSignal`, `PlayerInteractBlockSignal` and
  `PlayerBreakBlockSignal`.

Build the example jar with:

```bat
gradlew.bat :examples:minimal:jar
```

Copy `examples/minimal/build/libs/foabic-example-minimal-1.1.0.jar` into `foabic-mods/`
on either adapter. The PNG is bundled at
`examples/minimal/src/main/resources/assets/foabic_test/textures/block/test_block.png`.
