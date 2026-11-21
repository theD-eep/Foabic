# Build

Foabic 1.2 uses a **Gradle 9.5.1** wrapper. There is no project-local `.cmd` or `.ps1` build entry point. `gradlew.bat` / `gradlew` are the standard Gradle wrapper.

## Requirements

- **JDK 25** (full JDK; `javac` is required)
- The Gradle wrapper is checked in; no system Gradle installation is needed
- A local `.cache/` with the Minecraft/Fabric/NeoForge inputs described below

## Quick build

Windows:

```bat
set JAVA_HOME=C:\path\to\jdk-25
gradlew.bat build
```

macOS/Linux:

```sh
export JAVA_HOME=/path/to/jdk-25
./gradlew build
```

This compiles all modules, runs the core tests and the API contract check, packages the platform/example jars and copies the release artifacts to `dist/`.

## Tasks

| Task | Purpose |
| --- | --- |
| `build` | Full build, tests, `dist` |
| `dist` | Copy the six release jars into `dist/` and write `dist/SHA256SUMS` |
| `coreTest` | Run lifecycle, freeze, metadata, config, networking and loader tests |
| `:api:check` | Verify the core API jar is under 15 KiB and has no platform imports |
| `:api:jar` | Compile-time core API |
| `:content:jar` | Minecraft-facing API |
| `:fabric:jar` | Fabric adapter, including API + content + loader |
| `:neoforge:jar` | NeoForge adapter, including API + content + loader |
| `:examples:minimal:jar` | v1.2 acceptance mod |
| `:examples:chat:jar` | Chat reply example |
| `:examples:plugin-demo:build` | One-source-tree Foabic Loom demo; writes its own `dist/` |
| `clean` | Delete Gradle outputs and `dist/` |

`gradlew.bat :examples:plugin-demo:build` proves the plugin path:

```text
examples/plugin-demo/dist/mod-fabric.jar
examples/plugin-demo/dist/mod-neoforge.jar
```

## Projects

```text
:api            tiny core API; no Minecraft or platform dependency
:content        Minecraft-facing API: registry, signals, resource helpers
:loader         loader, metadata, config and generated resources
:fabric         Fabric Loom adapter
:neoforge       NeoForge adapter
:examples:minimal     v1.2 acceptance mod
:examples:chat        chat reply example
:examples:plugin-demo Foabic Loom demo
```

The core/content split exists to satisfy the 15 KiB core limit. Both are bundled into the platform jars, so runtime installation is still one jar.

Source layout:

```text
src/api/java
src/content/java
src/loader/java
src/fabric/java
src/fabric/resources
src/neoforge/java
src/neoforge/resources
src/test/java
foabic-gradle-plugin/src/main/java
examples/minimal/src/main/{java,resources}
examples/chat/src/main/{java,resources}
examples/plugin-demo/src/main/{java,resources}
```

## API contract check

`:api:check` runs `verifyApiContract`:

```text
FOABIC_API_SIZE bytes=14420 limit=15360
```

It fails if:

- `foabic-api-1.2.0.jar` is 15 KiB or larger;
- any source under `src/api` or `src/content` contains `net.fabricmc.*` or `net.neoforged.*`.

The core API is compiled with debug information disabled and a reproducible jar.

## Platform plugins

### Fabric

`:fabric` applies the non-remapping Loom plugin for Minecraft 26.3:

```kotlin
plugins {
    id("net.fabricmc.fabric-loom") version "1.14.10"
}
```

`minecraft("com.mojang:minecraft:26.3")` supplies the merged game jar. Because 26.3 is unobfuscated, there is no mappings dependency and no `modImplementation` configuration. Fabric Loader and Fabric API are normal `implementation` dependencies; the Fabric API POM brings its module jars onto the compile classpath.

The adapter uses Fabric's `ResourceLoader.registerBuiltinPack` with a synthetic `ModContainer` rooted at `.foabic/generated-resources`. This is a public API, not a Mixin hook.

### NeoForge

`:neoforge` uses Gradle's `java` plugin against the official **NeoForge 26.3.0.115 universal jar** plus the installed NeoForge libraries:

```kotlin
compileOnly(files(neoForgeJar))
compileOnly(neoForgeLibraries)
```

This avoids NeoForm/NFRT downloads. In an environment with access to `maven.neoforged.net`, the same source set can be switched to `net.neoforged.moddev`; the adapter code and jar contents are independent of the build plugin.

The adapter registers a fixed `PathPackResources` through `AddPackFindersEvent` for generated resources.

## Foabic Loom

`foabic-gradle-plugin/` is an included build. A consuming project can use:

```kotlin
// settings.gradle.kts
pluginManagement {
    includeBuild("foabic-gradle-plugin")
}
```

```kotlin
// build.gradle.kts
plugins {
    java
    id("dev.foabic.loom")
}

foabic {
    minecraftVersion = "26.3"
}
```

The plugin compiles the single `main` source set once and produces `dist/mod-fabric.jar` and `dist/mod-neoforge.jar`. It reads `src/main/resources/foabic.mod.json`, translates it to `fabric.mod.json` / `META-INF/neoforge.mods.toml`, and embeds the canonical metadata as a custom field. If `src/main/resources/icon.png` exists, or `foabic.mod.json` declares an `icon` path, the icon is injected as Fabric's `icon` and NeoForge's `logoFile`.

The plugin does not provision a Minecraft development environment. Add the Foabic content API and the Minecraft compile classpath in the normal way; the plugin owns packaging and metadata translation.

## Local input cache

The build reads these existing inputs:

```text
.cache/
  mc-26.3/server-nested.jar
  mc-bundle-libs/META-INF/libraries/...
  fabric-loader-0.20.1.jar
  fabric-api-0.166.2+26.3.jar
  neoforge-server/libraries/...
  neoforge-server/libraries/net/neoforged/neoforge/26.3.0.115/
    neoforge-26.3.0.115-universal.jar
```

`.cache/` is ignored by git. It is a local build input, not part of the source release.

## Tests

`gradlew.bat coreTest` runs `dev.foabic.test.AllTests` on the platform-neutral test classpath. It covers:

- event dispatch, unsubscribe and clear;
- listener failure isolation;
- version/range parsing, including NeoForge's four-part `26.3.0.115`;
- legacy and v1.2 signal data classes;
- network encode/decode/dispatch with a fake transport;
- duplicate-id discovery and malformed-metadata skipping;
- JSON `foabic.mod.json` parsing;
- missing Fabric API dependency diagnostics and provided-version resolution;
- registry-freeze exception and exact ERROR line;
- config POJO/record save and reload;
- generated blockstate/model/texture copy and missing-texture fallback.

Expected tail:

```text
PASS events
PASS failure isolation
PASS versions
PASS legacy use-block signal
PASS player-interact-block signal
PASS player-break-block signal
PASS server-tick signal
PASS networking round trip
PASS loader defenses
ALL TESTS PASSED (9)
```

The test classpath includes Minecraft and the loader because the content and defensive-loader fixtures reference Minecraft classes. No client is started.

See [VERIFICATION.md](VERIFICATION.md) for the dedicated-server evidence.
