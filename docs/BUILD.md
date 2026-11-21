# Build

Foabic 1.0 uses a **Gradle 9.5.1** wrapper. There is no project-local `.cmd` or `.ps1` build entry point.

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

This compiles all modules, runs the core tests, packages four jars and copies them to `dist/`.

## Tasks

| Task | Purpose |
| --- | --- |
| `build` | Full build, tests and `dist` |
| `dist` | Copy the four release jars into `dist/` and write `dist/SHA256SUMS` |
| `coreTest` | Run the platform-neutral event/version/signal tests |
| `:api:jar` | Compile-time API only |
| `:fabric:jar` | Fabric adapter, including API and loader classes |
| `:neoforge:jar` | NeoForge adapter, including API and loader classes |
| `:example:jar` | Platform-neutral acceptance mod |
| `clean` | Delete Gradle outputs and `dist/` |

## Projects

```text
:api        Java library, Minecraft content types on the compile classpath
:loader     Java library, depends on :api
:fabric     Fabric Loom adapter
:neoforge   NeoForge adapter
:example    acceptance mod
```

Source directories are reused from the original layout:

```text
src/api/java
src/loader/java
src/fabric/java
src/fabric/resources
src/neoforge/java
src/neoforge/resources
examples/minimal/src
examples/minimal/resources
src/test/java
```

## Platform plugins

### Fabric

`:fabric` applies the non-remapping Loom plugin for Minecraft 26.1+:

```kotlin
plugins {
    id("net.fabricmc.fabric-loom") version "1.14.10"
}
```

`minecraft("com.mojang:minecraft:26.3")` supplies the merged game jar. Because 26.3 is unobfuscated, there is no mappings dependency and no `modImplementation` configuration. Fabric Loader and Fabric API are normal `implementation` dependencies; the Fabric API POM brings its module jars onto the compile classpath.

### NeoForge

`:neoforge` uses Gradle's `java` plugin against the official **NeoForge 26.3.0.115 universal jar** plus the installed NeoForge libraries:

```kotlin
compileOnly(files(neoForgeJar))
compileOnly(neoForgeLibraries)
```

This avoids NeoForm/NFRT downloads. In an environment with access to `maven.neoforged.net`, the same source set can be switched to `net.neoforged.moddev`; the adapter code and jar contents are independent of the build plugin.

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

See [VERIFICATION.md](VERIFICATION.md) for the runtime evidence produced from the Gradle-built jars.
