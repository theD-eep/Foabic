# Foabic Loom demo

This project is one source tree and one `foabic.mod.json`. It applies the
included `dev.foabic.loom` plugin and builds two platform jars.

From the repository root:

```bat
gradlew.bat :examples:plugin-demo:build
```

Outputs:

```text
examples/plugin-demo/dist/mod-fabric.jar
examples/plugin-demo/dist/mod-neoforge.jar
```

The Fabric jar contains `fabric.mod.json`; the NeoForge jar contains
`META-INF/neoforge.mods.toml`; both contain the canonical `foabic.mod.json`
and the icon at `assets/foabic_plugin_demo/icon.png`. The plugin also runs
`foabicData`; this demo declares no `data` provider, so the task skips:

```text
FOABIC_DATAGEN skipped: no data provider declared
```

See [`examples/survival`](../survival) for a plugin demo that does generate
recipes, tags and models. This demo uses the explicit metadata field:

```json
"icon": "assets/foabic_plugin_demo/icon.png"
```

Foabic Loom also auto-detects `src/main/resources/icon.png` and
`src/main/resources/assets/<id>/icon.png` when the field is omitted. It
injects Fabric `icon` and NeoForge `logoFile` and packages the PNG. Both jars
can be placed in `foabic-mods/` or the standard platform `mods/` directory.

Launchers only scan standard platform metadata under `mods/`; put the generated
platform jar there if you want the launcher to see it at all. HMCL 3.9.2+ then
shows the metadata icon. PCL2 does **not** read a local mod's icon; it only
shows the online Modrinth/CurseForge project logo. See
[`docs/LAUNCHER_ICONS.md`](../../docs/LAUNCHER_ICONS.md) for the per-instance
icon workaround in both launchers. The Foabic loader also scans `mods/`.

The project uses the repository's core/content projects for its compile classpath;
the plugin itself owns packaging and metadata translation. A real mod would also
add its Minecraft compile classpath in the normal way.
