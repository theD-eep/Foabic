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
and the icon at `assets/foabic_plugin_demo/icon.png`. This demo uses the
explicit metadata field:

```json
"icon": "assets/foabic_plugin_demo/icon.png"
```

Foabic Loom also auto-detects `src/main/resources/icon.png` and
`src/main/resources/assets/<id>/icon.png` when the field is omitted. It
injects Fabric `icon` and NeoForge `logoFile` and packages the PNG. Both jars
can be placed in `foabic-mods/` or the standard platform `mods/` directory.

Launchers such as HMCL/PCL2 only scan standard platform metadata under `mods/`.
Put the generated platform jar there if you want launcher icon display; the
Foabic loader also scans `mods/`.

The project uses the repository's core/content projects for its compile classpath;
the plugin itself owns packaging and metadata translation. A real mod would also
add its Minecraft compile classpath in the normal way.
