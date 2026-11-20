[CmdletBinding()]
param(
    [string]$JavaHome = $env:JAVA_HOME,
    [string]$McJar = $env:FOABIC_MC_JAR,
    [switch]$SkipTests
)

# Builds release artifacts, writes dist/SHA256SUMS and prints the tag steps.
# The GitHub Release workflow does the same on a v* tag; this script is for
# maintainers who need to build the full adapter locally.

$ErrorActionPreference = "Stop"
$Tools = Split-Path -Parent $MyInvocation.MyCommand.Path
$Root = Split-Path -Parent $Tools
$Dist = Join-Path $Root "dist"

$buildArgs = @{}
if ($JavaHome) { $buildArgs["JavaHome"] = $JavaHome }
if ($McJar) { $buildArgs["McJar"] = $McJar }
if ($SkipTests) { $buildArgs["SkipTests"] = $true }

& (Join-Path $Root "build.ps1") @buildArgs

$jars = @(Get-ChildItem -LiteralPath $Dist -Filter "*.jar" | Sort-Object Name)
if ($jars.Count -eq 0) {
    throw "no artifacts in $Dist"
}

$lines = foreach ($jar in $jars) {
    $hash = (Get-FileHash -LiteralPath $jar.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
    "{0}  {1}" -f $hash, $jar.Name
}
$lines | Set-Content -LiteralPath (Join-Path $Dist "SHA256SUMS") -Encoding ascii

Write-Host ""
Write-Host "Artifacts"
$lines | ForEach-Object { Write-Host "  $_" }
Write-Host ""
Write-Host "Next steps"
Write-Host "  1. git tag -a v<version> -m `"v<version>`""
Write-Host "  2. git push origin v<version>"
Write-Host "  3. the Release workflow rebuilds and attaches the jars"
Write-Host "     (set repository variable MC_SERVER_JAR_URL first)"
