# Client verification harness (not a user mod)

This Fabric client mod auto-connects to `127.0.0.1:25598`, waits for the example block to appear at `playerPos + (2, 1, 0)`, invokes `MultiPlayerGameMode.useItemOn`, and takes a screenshot.

It exists only to reproduce [docs/VERIFICATION.md](../../docs/VERIFICATION.md). Do not ship it.

Build requirements: Fabric Loader 0.20.1, Fabric API 0.145.1+26.1, Minecraft 26.1 client jar and client libraries. The prebuilt test jar is not included in `dist/`.
