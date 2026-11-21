# Roadmap and scope

## 0.2 — shipping now

- Fabric Loader/API adapter
- typed registry bridge and `submit`
- minimal `connect` and one `UseBlockSignal`
- common client/server content registration so Fabric registry sync works
- runnable example, real client/server verification
- no NeoForge, no signal standard library, no build plugin, no Mixin, no cross-platform packaging

## 0.3 and later (deferred, not in this branch)

- NeoForge adapter (only after the Fabric path stays stable)
- a signal standard library / typed signal descriptors
- Gradle or Maven build plugin
- Mixin support for arbitrary hooks
- cross-platform packaging
- networking, GUI, model/creative-tab helpers, data generation
- 26.2+ compatibility
- binary-compatibility commitments

The explicit rule for 0.3: do not add any of the deferred items to 0.2. A working Fabric path is worth more than a broad, partially working abstraction.

## Non-goals

- replacing Fabric Loader or Fabric API
- sandboxing mods
- a cross-loader mod format
- a large annotation/reflection framework
- generated assets or content pipelines in the core
