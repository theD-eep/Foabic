# Launcher icons

Foabic packages a 256×256 feather PNG at `icon.png` and declares it in both
platform metadata files. That is the correct mod metadata, but **not every
launcher displays mod metadata icons**. The behaviour differs enough that it is
worth checking before changing the mod.

The short version:

| Launcher | Mod-list icon from the jar? | What it actually reads |
| --- | --- | --- |
| HMCL 3.9.2 and later | Yes | Fabric `icon`, NeoForge `logoFile`, then common root names such as `icon.png` and `logo.png` |
| PCL2 | **No** | Only the online Modrinth/CurseForge project logo, looked up by file hash |
| Minecraft Mod Menu (Fabric) | Yes | Fabric `fabric.mod.json` `icon` |
| HMCL / PCL2 instance card | Yes, but it is a separate per-instance setting | `icon.png` in the instance root (HMCL) or `PCL\Logo.png` (PCL2) |

## HMCL

HMCL 3.9.2's mod list accepts the icon from the mod metadata. Its
`ModListPageSkin.loadIcon()` starts with `LocalModFile.getLogoPath()` and then
tries root names including `icon.png`, `logo.png` and
`assets/<modid>/icon.png`:

- [HMCL v3.9.2 `ModListPageSkin.java`](https://github.com/HMCL-dev/HMCL/blob/v3.9.2/HMCL/src/main/java/org/jackhuang/hmcl/ui/versions/ModListPageSkin.java)

Use the platform jar in the standard mods directory:

```text
.minecraft/mods/foabic-fabric-1.2.0.jar       (Fabric)
.minecraft/mods/foabic-neoforge-1.2.0.jar     (NeoForge)
```

For an isolated HMCL instance, use that instance's `mods/` directory instead.
Refresh the mod list or restart HMCL. If the loader icon (a Fabric/NeoForge
symbol) is still shown, the jar in that list is not one of the platform jars,
or the HMCL build predates metadata-icon support.

## PCL2

PCL2 does not read icons from a local mod jar at all. Its local mod row calls
`Entry.Project.ApplyLogoToMyImage(...)`, and `Project` is populated only by the
online Modrinth/CurseForge lookup:

- [`MyLocalModItem.xaml.vb`](<https://github.com/Meloong-Git/PCL/blob/main/Plain Craft Launcher 2/Pages/PageInstance/MyLocalModItem.xaml.vb>)
- [`LocalResourceLoaders.vb`](<https://github.com/Meloong-Git/PCL/blob/main/Plain Craft Launcher 2/Modules/Resource/LocalResourceLoaders.vb>)

Adding `icon.png`, changing `fabric.mod.json`, or adding a NeoForge `logoFile`
cannot change that. PCL2 will keep showing `NoIcon.png` for a local-only mod.
There are only two ways to see a project logo there:

1. Publish the mod to Modrinth or CurseForge with the feather as the project
   icon. PCL2 identifies the file by SHA-1/CurseForge fingerprint and then
   displays the online logo.
2. Use a PCL2 **instance** icon, which is a launcher-level setting rather than
   a mod icon. See below.

For a Modrinth project icon, use the 512×512 file:
[foabic-icon-512.png](assets/launcher/foabic-icon-512.png). Upload it as the
project icon, not as a gallery image, and keep the released file byte-for-byte
unchanged: PCL2 matches the local jar's SHA-1 to the version file, then displays
the project's online icon. PCL2 caches local mod metadata, so restart it after
the project is approved and the version is published.

## Instance icons (works in both launchers)

These set the picture on the version/instance card, not the row in the mod
list:

- **HMCL:** put a square `icon.png` in the instance root, for example
  `.minecraft/versions/26.3-Foabic/icon.png`.
- **PCL2:** open the instance → game settings → game icon → custom, and choose
  the PNG. PCL2 copies it to `versions/<name>/PCL/Logo.png` and records
  `Logo:PCL\Logo.png` plus `LogoCustom:True` in `PCL/Setup.ini`.

The helper script does the copy and, for PCL2, updates `Setup.ini`:

```bat
:: HMCL
tools\install-launcher-icon.cmd -Launcher HMCL -InstancePath "C:\Users\me\.minecraft\versions\26.3-Foabic"

:: PCL2
tools\install-launcher-icon.cmd -Launcher PCL -InstancePath "D:\PCL\versions\26.3-Foabic"
```

The `.cmd` wrapper calls PowerShell with `-ExecutionPolicy Bypass`, because a
default Windows execution policy blocks `.ps1` files. Running the `.ps1`
directly works only after relaxing that policy.

Both commands use
[foabic-icon-256.png](assets/launcher/foabic-icon-256.png) and keep a backup of
an existing icon. Restart the launcher if it caches the old card image.

## Built-in icon variants

| File | Use |
| --- | --- |
| [foabic-icon-32.png](assets/launcher/foabic-icon-32.png) | tiny previews / tray |
| [foabic-icon-64.png](assets/launcher/foabic-icon-64.png) | small lists |
| [foabic-icon-128.png](assets/launcher/foabic-icon-128.png) | 2× lists |
| [foabic-icon-256.png](assets/launcher/foabic-icon-256.png) | HMCL/PCL2 instance icon, mod metadata |
| [foabic-icon-512.png](assets/launcher/foabic-icon-512.png) | Modrinth/CurseForge project icon |

All variants are nearest-neighbour scales of the same 16×16 feather sprite, so
the pixel edges stay sharp.
