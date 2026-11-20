[CmdletBinding()]
param(
    [string]$ServerJar = "server.jar",
    [string]$AgentJar = "",
    [string]$JavaHome = $env:JAVA_HOME,
    [string[]]$ServerArgs = @("nogui")
)

$ErrorActionPreference = "Stop"
$Tools = Split-Path -Parent $MyInvocation.MyCommand.Path
$Root = Split-Path -Parent $Tools
$Work = (Get-Location).Path

if (-not $AgentJar) {
    $AgentJar = Join-Path $Root "dist\foabic-0.1.0.jar"
}
if (-not (Test-Path $AgentJar)) {
    throw "Foabic agent not found: $AgentJar (run build.ps1 first)"
}
$AgentJar = [System.IO.Path]::GetFullPath($AgentJar)

if (-not $JavaHome) {
    $javaCommand = Get-Command java.exe -ErrorAction SilentlyContinue
    if (-not $javaCommand) {
        throw "Java 25 not found. Set JAVA_HOME or pass -JavaHome."
    }
    $JavaHome = Split-Path -Parent (Split-Path -Parent $javaCommand.Source)
}
$Java = Join-Path $JavaHome "bin\java.exe"
$Jar = Join-Path $JavaHome "bin\jar.exe"
if (-not (Test-Path $Java) -or -not (Test-Path $Jar)) {
    throw "No full JDK at $JavaHome"
}

$server = (Resolve-Path -LiteralPath $ServerJar).Path
$runtime = Join-Path $Work ".foabic\runtime"
$marker = Join-Path $runtime ".source"
$sourceId = "$($server)|$((Get-Item $server).Length)|$((Get-Item $server).LastWriteTimeUtc.Ticks)"
$currentId = if (Test-Path $marker) { (Get-Content $marker -Raw).Trim() } else { "" }

if ($currentId -ne $sourceId) {
    Remove-Item -LiteralPath $runtime -Recurse -Force -ErrorAction SilentlyContinue
    New-Item -ItemType Directory -Force -Path $runtime | Out-Null
    Push-Location $runtime
    try {
        & $Jar xf $server
        if ($LASTEXITCODE -ne 0) {
            throw "could not extract $server"
        }
    } finally {
        Pop-Location
    }
    Set-Content -LiteralPath $marker -Value $sourceId -Encoding ascii
}

$versions = Join-Path $runtime "META-INF\versions"
$nested = $null
if (Test-Path $versions) {
    $nested = Get-ChildItem -LiteralPath $versions -Recurse -Filter "server-*.jar" |
            Select-Object -First 1
}
if (-not $nested) {
    throw "No nested server jar in $server; use the official server.jar bundle."
}
$libraries = @(Get-ChildItem -LiteralPath (Join-Path $runtime "META-INF\libraries") `
            -Recurse -Filter *.jar -ErrorAction SilentlyContinue |
        Select-Object -ExpandProperty FullName)
$classpath = (@($nested.FullName) + $libraries) -join ";"

$javaArgs = @(
    "-Dfoabic.gameDir=$Work",
    "-javaagent:$AgentJar",
    "-cp", $classpath,
    "net.minecraft.server.Main"
) + $ServerArgs

& $Java @javaArgs
exit $LASTEXITCODE
