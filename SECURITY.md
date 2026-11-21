# Security policy

## Supported versions

| Version | Supported |
| --- | --- |
| 1.2.x | yes |
| 1.1.x | no |
| 1.0.x | no |
| 0.x | no |

Only the 1.2 line on Minecraft 26.3 is supported. Older releases are not maintained.

## Reporting a vulnerability

Do **not** open a public issue, discussion or pull request for a security problem.

Use the repository's **Private vulnerability reporting** (Security → Report a vulnerability). If that entry point is unavailable, contact the maintainers privately through the GitHub profile listed on the repository.

Please include:

- the affected version and component (API, content API, loader, Fabric adapter, NeoForge adapter, Foabic Loom);
- reproduction steps or a minimal project;
- impact (crash, permission bypass, arbitrary file read/write, code execution, ...);
- whether it was reproduced on a real Fabric or NeoForge server.

We aim to acknowledge within 7 days and to credit reporters after a fix ships, unless you prefer to stay anonymous. Please do not disclose details before a release.

## Security model

- Foabic **does not sandbox** mods. A Foabic mod runs as ordinary trusted JVM code inside Fabric Loader or NeoForge, with the same permissions as the server. Only install mods you trust.
- Foabic does not bundle Minecraft, Fabric Loader, Fabric API or NeoForge. Those are supplied by the user and their security is their own project's responsibility.
- The loader discovers mods from `foabic-mods/` and the fallback `mods/` directory by reading `foabic.mod.json` (legacy `foabic.mod` is accepted during migration). Foabic mods are not Fabric/NeoForge mods and do not use loader metadata for their entrypoints.
- The mod jar is loaded into a dedicated `URLClassLoader`; Foabic does not extract native code or execute scripts from the jar.
- Dependency versions are checked before entrypoint classloading. A missing or incompatible required dependency aborts the load with a clear message.
- Generated resources are written under `.foabic/generated-resources`. Paths are normalized and checked against the intended roots; declared texture/model paths containing `..` are rejected. The generated pack is read-only input to the platform resource system.
- Config files are written under `config/foabic/<mod-id>/`; file names are derived from the config class simple name and the mod id is validated. A malformed file is backed up rather than trusted.
- Icon paths are normalized and restricted to `.png` files without `..` or absolute paths by the loader and Foabic Loom. Foabic Loom also checks that the declared icon exists under `src/main/resources` before packaging.
- Networking is a trusted-mod transport, not a security boundary. Foabic frames packets with a class name and opaque bytes, limits each packet to 1 MiB, drops unknown types, and dispatches on the game thread. Mod packet handlers are trusted code; validate all fields before acting on them.
- The adapter registers signal listeners, creative-tab callbacks, a server-stopped cleanup hook and network payload handlers. These run trusted mod listeners on the server or client thread.
- Build inputs are explicit local jars; the build does not download or execute code from the network.

Areas worth testing: manifest/JSON parsing, dependency resolution that could load an unexpected jar, registry-factory ordering, freeze-guard bypasses, signal listener error isolation, generated-resource path handling, config path handling, packet size/type validation, and classloading boundaries between Foabic mods and the platform.

## Dependencies

There is no bundled third-party runtime dependency beyond the JDK. Gson is supplied by Minecraft and used by the loader for config/metadata. Fabric API, Fabric Loader and NeoForge are provided by the user and are not redistributed here.
