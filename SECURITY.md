# Security policy

## Supported versions

| Version | Supported |
| --- | --- |
| 1.0.x | ✅ |
| 0.x | ❌ |

Only the 1.0 line on Minecraft 26.3 is supported. Older releases are not maintained.

## Reporting a vulnerability

Do **not** open a public issue, discussion or pull request for a security problem.

Use the repository's **Private vulnerability reporting** (Security → Report a vulnerability). If that entry point is unavailable, contact the maintainers privately through the GitHub profile listed on the repository.

Please include:

- the affected version and component (API, loader, Fabric adapter, NeoForge adapter);
- reproduction steps or a minimal project;
- impact (crash, permission bypass, arbitrary file read, code execution, ...);
- whether it was reproduced on a real Fabric or NeoForge server.

We aim to acknowledge within 7 days and to credit reporters after a fix ships, unless you prefer to stay anonymous. Please do not disclose details before a release.

## Security model

- Foabic **does not sandbox** mods. A Foabic mod runs as ordinary trusted JVM code inside Fabric Loader or NeoForge, with the same permissions as the server. Only install mods you trust.
- Foabic 1.0 does not bundle Minecraft, Fabric Loader, Fabric API or NeoForge. Those are supplied by the user and their security is their own project's responsibility.
- The loader discovers mods only from `foabic-mods/` by reading `foabic.mod`. Foabic mods are not Fabric/NeoForge mods and do not use loader metadata.
- The mod jar is loaded into a dedicated `URLClassLoader`; Foabic does not extract native code or execute scripts from the jar.
- There is no command, permission or resource API in 1.0, so those are not part of the attack surface.
- Build inputs are explicit local jars; the build does not download or execute code from the network.

Areas worth testing: manifest parsing, dependency resolution that could load an unexpected jar, registry-factory ordering, signal listener error isolation, and classloading boundaries between Foabic mods and the platform.

## Dependencies

There is no bundled third-party runtime dependency beyond the JDK. Fabric API, Fabric Loader and NeoForge are provided by the user and are not redistributed here.
