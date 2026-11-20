[CmdletBinding()]
param(
    [string]$JavaHome = $env:JAVA_HOME,
    [string]$McJar = $env:FOABIC_MC_JAR,
    [switch]$SkipTests
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$Version = "0.1.0"
$Build = Join-Path $Root "build"
$Classes = Join-Path $Build "classes"
$Dist = Join-Path $Root "dist"

function Fail([string]$Message) {
    Write-Error $Message
    exit 1
}

if (-not $JavaHome) {
    $javacCommand = Get-Command javac.exe -ErrorAction SilentlyContinue
    if (-not $javacCommand) {
        Fail "Java 25 JDK not found. Set JAVA_HOME or pass -JavaHome."
    }
    $JavaHome = Split-Path -Parent (Split-Path -Parent $javacCommand.Source)
}

$Javac = Join-Path $JavaHome "bin\javac.exe"
$Jar = Join-Path $JavaHome "bin\jar.exe"
$Java = Join-Path $JavaHome "bin\java.exe"
if (-not (Test-Path $Javac) -or -not (Test-Path $Jar) -or -not (Test-Path $Java)) {
    Fail "No full JDK at $JavaHome (javac/jar/java are required)."
}

$javacVersion = (& $Javac -version 2>&1 | Out-String).Trim()
if ($javacVersion -notmatch "25\.") {
    Fail "Foabic targets Java 25 and Minecraft 26.1; found '$javacVersion'."
}

function Invoke-Javac {
    param(
        [string]$SourceRoot,
        [string]$Output,
        [string[]]$Classpath = @()
    )
    New-Item -ItemType Directory -Force -Path $Output | Out-Null
    $files = @(Get-ChildItem -LiteralPath $SourceRoot -Recurse -Filter *.java |
            Select-Object -ExpandProperty FullName)
    if ($files.Count -eq 0) {
        throw "No Java sources under $SourceRoot"
    }
    $javacArgs = @("--release", "25", "-g:none", "-Xlint:all,-classfile", "-d", $Output)
    if ($Classpath.Count -gt 0) {
        $javacArgs += @("-cp", ($Classpath -join ";"))
    }
    $javacArgs += $files
    & $Javac @javacArgs
    if ($LASTEXITCODE -ne 0) {
        throw "javac failed for $SourceRoot"
    }
}

Write-Host "Foabic $Version build"
Write-Host "  JDK: $JavaHome"

Remove-Item -LiteralPath $Classes -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $Classes, $Dist | Out-Null

$api = Join-Path $Classes "api"
$loader = Join-Path $Classes "loader"
$mc26 = Join-Path $Classes "mc26"
$test = Join-Path $Classes "test"

Write-Host "[1/5] API"
Invoke-Javac -SourceRoot (Join-Path $Root "src\api\java") -Output $api

Write-Host "[2/5] loader"
Invoke-Javac -SourceRoot (Join-Path $Root "src\loader\java") -Output $loader -Classpath @($api)

$mcBuilt = $false
if (-not $McJar -and (Test-Path (Join-Path $Root "server.jar"))) {
    $McJar = Join-Path $Root "server.jar"
}

if ($McJar) {
    $McJar = [System.IO.Path]::GetFullPath($McJar)
    if (-not (Test-Path $McJar)) {
        Fail "Minecraft server jar not found: $McJar"
    }
    Write-Host "[3/5] Minecraft 26.x adapter"
    $bundle = Join-Path $Build "mc-bundle"
    $marker = Join-Path $bundle ".source"
    $sourceId = "$($McJar)|$((Get-Item $McJar).Length)|$((Get-Item $McJar).LastWriteTimeUtc.Ticks)"
    $currentId = if (Test-Path $marker) { (Get-Content $marker -Raw).Trim() } else { "" }
    if ($currentId -ne $sourceId) {
        Remove-Item -LiteralPath $bundle -Recurse -Force -ErrorAction SilentlyContinue
        New-Item -ItemType Directory -Force -Path $bundle | Out-Null
        Push-Location $bundle
        try {
            & $Jar xf $McJar
            if ($LASTEXITCODE -ne 0) {
                throw "could not extract $McJar"
            }
        } finally {
            Pop-Location
        }
        Set-Content -LiteralPath $marker -Value $sourceId -Encoding ascii
    }

    $versions = Join-Path $bundle "META-INF\versions"
    $nested = $null
    if (Test-Path $versions) {
        $nested = Get-ChildItem -LiteralPath $versions -Recurse -Filter "server-*.jar" |
                Select-Object -First 1
    }
    if (-not $nested) {
        Fail "No nested server jar in $McJar; use the official server.jar bundle."
    }
    $libraries = @(Get-ChildItem -LiteralPath (Join-Path $bundle "META-INF\libraries") `
                -Recurse -Filter *.jar -ErrorAction SilentlyContinue |
            Select-Object -ExpandProperty FullName)
    $mcClasspath = @($nested.FullName) + $libraries + @($api, $loader)
    Invoke-Javac -SourceRoot (Join-Path $Root "src\mc26\java") -Output $mc26 -Classpath $mcClasspath
    $mcBuilt = $true
} else {
    Write-Warning "[3/5] FOABIC_MC_JAR not set: building core only (no 26.x adapter)."
}

Write-Host "[4/5] jars"
$apiManifest = Join-Path $Build "API-MANIFEST.MF"
$agentManifest = Join-Path $Build "AGENT-MANIFEST.MF"
@"
Manifest-Version: 1.0
Implementation-Title: Foabic API
Implementation-Version: $Version
Automatic-Module-Name: dev.foabic.api
"@ | Set-Content -LiteralPath $apiManifest -Encoding ascii
@"
Manifest-Version: 1.0
Premain-Class: dev.foabic.loader.agent.FoabicAgent
Agent-Class: dev.foabic.loader.agent.FoabicAgent
Can-Retransform-Classes: false
Implementation-Title: Foabic
Implementation-Version: $Version
Implementation-Vendor: Foabic
"@ | Set-Content -LiteralPath $agentManifest -Encoding ascii

$apiJar = Join-Path $Dist "foabic-api-$Version.jar"
$agentJar = Join-Path $Dist "foabic-$Version.jar"
Remove-Item -LiteralPath $apiJar, $agentJar -Force -ErrorAction SilentlyContinue
& $Jar --create --file $apiJar --manifest $apiManifest -C $api .
if ($LASTEXITCODE -ne 0) { throw "jar failed for API" }

$all = Join-Path $Classes "all"
New-Item -ItemType Directory -Force -Path $all | Out-Null
foreach ($dir in @($api, $loader, $mc26)) {
    if (Test-Path $dir) {
        Copy-Item -Path (Join-Path $dir "*") -Destination $all -Recurse -Force
    }
}
& $Jar --create --file $agentJar --manifest $agentManifest -C $all .
if ($LASTEXITCODE -ne 0) { throw "jar failed for agent" }

$apiSourcesJar = Join-Path $Dist "foabic-api-$Version-sources.jar"
$agentSourcesJar = Join-Path $Dist "foabic-$Version-sources.jar"
Remove-Item -LiteralPath $apiSourcesJar, $agentSourcesJar -Force -ErrorAction SilentlyContinue

& $Jar --create --file $apiSourcesJar -C (Join-Path $Root "src\api\java") .
if ($LASTEXITCODE -ne 0) { throw "jar failed for API sources" }

$sources = Join-Path $Build "sources"
Remove-Item -LiteralPath $sources -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $sources | Out-Null
foreach ($dir in @("src\api\java", "src\loader\java", "src\mc26\java")) {
    $path = Join-Path $Root $dir
    if (Test-Path $path) {
        Copy-Item -Path (Join-Path $path "*") -Destination $sources -Recurse -Force
    }
}
& $Jar --create --file $agentSourcesJar -C $sources .
if ($LASTEXITCODE -ne 0) { throw "jar failed for agent sources" }
Remove-Item -LiteralPath $sources -Recurse -Force -ErrorAction SilentlyContinue

if (-not $SkipTests) {
    Write-Host "[5/5] tests"
    Invoke-Javac -SourceRoot (Join-Path $Root "src\test\java") -Output $test -Classpath @($api, $loader)
    $testClasspath = @($test, $api, $loader) -join ";"
    $previousErrorAction = $ErrorActionPreference
    try {
        # The test suite deliberately logs one expected task failure to stderr.
        # Native stderr must not terminate the build under ErrorActionPreference=Stop.
        $ErrorActionPreference = "Continue"
        & $Java -cp $testClasspath dev.foabic.test.AllTests
        $testExit = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousErrorAction
    }
    if ($testExit -ne 0) {
        throw "tests failed"
    }
} else {
    Write-Host "[5/5] tests skipped"
}

Write-Host ""
Write-Host "Artifacts"
Get-ChildItem -LiteralPath $Dist -Filter "*.jar" | ForEach-Object {
    $hash = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.Substring(0, 16)
    Write-Host ("  {0,-28} {1,8:N1} KiB  sha256:{2}" -f $_.Name, ($_.Length / 1KB), $hash)
}
if ($mcBuilt) {
    Write-Host "Full 26.x adapter included."
} else {
    Write-Host "Core only. Set FOABIC_MC_JAR to build the 26.x adapter."
}
