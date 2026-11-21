# Security policy

## Supported versions

Foabic is a `0.x` beta; only the latest release receives security fixes.

| Version | Supported |
| --- | --- |
| 0.2.x | ✅ |
| < 0.2 | ❌ |

## Reporting a vulnerability

Do **not** open a public issue, discussion or pull request for a security problem.

Use the repository's **Private vulnerability reporting** (Security → Report a vulnerability). If that entry point is unavailable, contact the maintainers privately through the GitHub profile listed on the repository.

Please include:

- the affected version and component (API, loader, Fabric adapter);
- reproduction steps or a minimal project;
- impact (crash, permission bypass, arbitrary file read, code execution, ...);
- whether it was reproduced on a real Fabric server.

We aim to acknowledge within 7 days and to credit reporters after a fix ships, unless you prefer to stay anonymous. Please do not disclose details before a release.

## Security model

- Foabic **does not sandbox** mods. A Foabic mod runs as ordinary trusted JVM code inside Fabric Loader, with the same permissions as the server. Only install mods you trust.
- Foabic 0.2 does not bundle Minecraft, Fabric Loader or Fabric API. Those are supplied by the user and their security is their own project's responsibility.
- The loader discovers mods by reading `foabic.mod` from `mods/` and `foabic-mods/`.
- `ModResources` is scoped to the owning mod jar and rejects `..` paths.
- Commands use 0–4 permission levels mapped onto the Fabric environment.

Areas worth testing: manifest parsing, path traversal in resources, dependency resolution that could load an unexpected jar, registry-bridge ordering, permission mapping, and anything that could execute code from a mod jar before the user expects it.

## Dependencies

There is no bundled third-party runtime dependency beyond the JDK. Fabric API is provided by the user and is not redistributed here. Build-time downloads in CI are pinned to explicit versions and URLs.
