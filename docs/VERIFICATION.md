# Beta 0.2 verification

## Environment

| Item | Value |
| --- | --- |
| Date | 2026-11-21 |
| OS | Windows 11 |
| Java | OpenJDK 25.0.1 (Microsoft build) |
| Minecraft | Java Edition 26.1 |
| Fabric Loader | 0.20.1 |
| Fabric API | 0.145.1+26.1 |
| Foabic | 0.2.0 |

## End-to-end client/server test

1. Started a Fabric 26.1 dedicated server with Fabric API, Foabic and the example mod.
2. Started a Fabric 26.1 client with the same three mods.
3. The client auto-connected to the local server.
4. The example registered `smoketest:test_block` on both sides through `submit`.
5. On player join, the example placed the block at `playerPos + (2, 1, 0)`.
6. The client received the block state and its assets.
7. The client used the block (`MultiPlayerGameMode.useItemOn`).
8. The server adapter posted `UseBlockSignal`; the example set `SUCCESS` and sent an action bar.
9. The client captured a screenshot.

Evidence:

```text
# client
FOABIC_SMOKE init smoketest api=0.2.0
FOABIC_REGISTRY block=true item=true blocks=1169 items=1507
FOABIC_ASSET=true
FOABIC_CLIENT_JOIN
FOABIC_CLIENT_USE=Pass[]
FOABIC_CLIENT_BLOCK=Block{smoketest:test_block}
FOABIC_CLIENT_SCREENSHOT=已将截图保存为2026-11-21_13.19.23.png

# server
FOABIC_PLACED BlockPos{x=9, y=-59, z=-5}
Tester joined the game
FOABIC_USE_SIGNAL block=smoketest:test_block pos=9,-59,-5 hand=MAIN_HAND
```

Screenshot: [foabic-client.png](images/foabic-client.png)

It shows the checkerboard test block, the player's hand, and the action bar:

```text
Foabic: Test Block used
```

This verifies visible client rendering, client/server registry parity, real block interaction, signal dispatch, result mutation, and visible feedback.

## Server-only simulation

The example also exposes:

```mcfunction
/foabictest use
```

It uses Fabric's `FakePlayer` to invoke `UseBlockCallback` on the server and prints:

```text
FOABIC_USE_SIGNAL block=smoketest:test_block pos=0,64,0 hand=MAIN_HAND
FOABIC_USE_RESULT=Success[...]
```

## Core tests

```text
PASS versions
PASS events
PASS scheduler
PASS commands
ALL TESTS PASSED
```

## Artifacts

```text
foabic-api-0.2.0.jar                 46.6 KiB
foabic-fabric-0.2.0.jar              92.9 KiB
foabic-example-smoketest-0.2.0.jar    7.5 KiB
```

## Notes

- `Failed to fetch user properties` and Realms errors in the client log are expected from the offline test account; they do not affect joining or gameplay.
- The client test harness auto-connects to `127.0.0.1:25598`; it lives under `examples/client-test` and is not needed by users.
- No NeoForge, Mixin, signal library or build plugin was involved.
