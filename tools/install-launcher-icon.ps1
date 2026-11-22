# Installs the Foabic feather as a launcher instance icon.
#
# This is intentionally separate from mod metadata: PCL2 never reads a local
# mod's icon, and HMCL/PCL2 instance icons are a launcher setting, not part of
# the mod jar. See docs/LAUNCHER_ICONS.md.
[CmdletBinding()]
param(
    [Parameter(Mandatory)]
    [ValidateSet('HMCL', 'PCL')]
    [string]$Launcher,

    [Parameter(Mandatory)]
    [string]$InstancePath,

    [string]$IconPath
)

$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
if (-not $IconPath) {
    $IconPath = Join-Path $repoRoot 'docs/assets/launcher/foabic-icon-256.png'
}

if (-not (Test-Path -LiteralPath $IconPath -PathType Leaf)) {
    throw "Icon not found: $IconPath"
}
if (-not (Test-Path -LiteralPath $InstancePath -PathType Container)) {
    throw "Instance directory not found: $InstancePath"
}

$icon = (Resolve-Path -LiteralPath $IconPath).Path
$instance = (Resolve-Path -LiteralPath $InstancePath).Path
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'

function Backup-IfExists {
    param([string]$Path)

    if (Test-Path -LiteralPath $Path -PathType Leaf) {
        $backup = "$Path.backup-$stamp"
        Move-Item -LiteralPath $Path -Destination $backup
        Write-Host "Backed up $Path -> $backup"
    }
}

function Set-PclIniValue {
    param(
        [string]$Path,
        [string]$Key,
        [string]$Value
    )

    $lines = @()
    if (Test-Path -LiteralPath $Path -PathType Leaf) {
        $lines = @([System.IO.File]::ReadAllLines($Path))
    }

    $prefix = $Key + ':'
    $replaced = $false
    $result = foreach ($line in $lines) {
        if ($line.StartsWith($prefix, [System.StringComparison]::Ordinal)) {
            if (-not $replaced) {
                "$Key`:$Value"
                $replaced = $true
            }
        }
        else {
            $line
        }
    }

    if (-not $replaced) {
        $result = @($result) + "$Key`:$Value"
    }

    $text = (@($result) -join "`r`n") + "`r`n"
    [System.IO.File]::WriteAllText($Path, $text, [System.Text.UTF8Encoding]::new($false))
}

switch ($Launcher) {
    'HMCL' {
        # HMCL probes icon.<ext> in the instance root. Remove other supported
        # extensions after backing them up so icon.png is the one selected.
        Get-ChildItem -LiteralPath $instance -File -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -match '^icon\.(png|jpg|jpeg|gif|bmp|webp)$' } |
            ForEach-Object { Backup-IfExists -Path $_.FullName }

        $target = Join-Path $instance 'icon.png'
        Copy-Item -LiteralPath $icon -Destination $target -Force
        Write-Host "HMCL instance icon installed: $target"
    }

    'PCL' {
        $pclDir = Join-Path $instance 'PCL'
        New-Item -ItemType Directory -Path $pclDir -Force | Out-Null

        $target = Join-Path $pclDir 'Logo.png'
        Backup-IfExists -Path $target
        Copy-Item -LiteralPath $icon -Destination $target -Force

        $ini = Join-Path $pclDir 'Setup.ini'
        if (Test-Path -LiteralPath $ini -PathType Leaf) {
            Copy-Item -LiteralPath $ini -Destination "$ini.backup-$stamp"
        }

        Set-PclIniValue -Path $ini -Key 'Logo' -Value 'PCL\Logo.png'
        Set-PclIniValue -Path $ini -Key 'LogoCustom' -Value 'True'

        Write-Host "PCL2 instance icon installed: $target"
        Write-Host "Updated $ini (Logo, LogoCustom)"
    }
}

Write-Host 'Restart the launcher if it still shows the old cached card image.'
