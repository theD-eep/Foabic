[CmdletBinding()]
param(
    [string]$JavaHome = $env:JAVA_HOME,
    [string]$McJar = $env:FOABIC_MC_JAR,
    [string]$FabricLoaderJar = $env:FOABIC_FABRIC_LOADER_JAR,
    [string]$FabricApiJar = $env:FOABIC_FABRIC_API_JAR,
    [switch]$SkipTests
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$Version = "0.2.0"
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
if (-not (Test-Path $Javac) -or -not (Test-Path $Jar)) {
    Fail "No full JDK at $JavaHome."
}
$javacVersion = (& $Javac -version 2>&1 | Out-String).Trim()
if ($javacVersion -notmatch "25\.") {
    Fail "Foabic 0.2 targets Java 25; found '$javacVersion'."
}

function Invoke-Javac {
    param([string]$SourceRoot, [string]$Output, [string[]]$Classpath = @())
    New-Item -ItemType Directory -Force -Path $Output | Out-Null
    $files = @(Get-ChildItem -LiteralPath $SourceRoot -Recurse -Filter *.java |
            Select-Object -ExpandProperty FullName)
    if ($files.Count -eq 0) { throw "No Java sources under $SourceRoot" }
    $javacArgs = @("--release", "25", "-g:none", "-Xlint:all,-classfile", "-d", $Output)
    if ($Classpath.Count -gt 0) { $javacArgs += @("-cp", ($Classpath -join ";")) }
    $javacArgs += $files
    & $Javac @javacArgs
    if ($LASTEXITCODE -ne 0) { throw "javac failed for $SourceRoot" }
}

function Extract-IfChanged {
    param([string]$JarPath, [string]$Destination)
    $source = [System.IO.Path]::GetFullPath($JarPath)
    if (-not (Test-Path $source)) { Fail "jar not found: $source" }
    $marker = Join-Path $Destination ".source"
    $id = "$source|$((Get-Item $source).Length)|$((Get-Item $source).LastWriteTimeUtc.Ticks)"
    $current = if (Test-Path $marker) { (Get-Content $marker -Raw).Trim() } else { "" }
    if ($current -ne $id) {
        Remove-Item -LiteralPath $Destination -Recurse -Force -ErrorAction SilentlyContinue
        New-Item -ItemType Directory -Force -Path $Destination | Out-Null
        Push-Location $Destination
        try {
            & $Jar xf $source
            if ($LASTEXITCODE -ne 0) { throw "jar extraction failed: $source" }
        } finally { Pop-Location }
        Set-Content -LiteralPath $marker -Value $id -Encoding ascii
    }
}

if (-not $FabricLoaderJar) { $FabricLoaderJar = Join-Path $Build "fabric-loader.jar" }
if (-not $FabricApiJar) { $FabricApiJar = Join-Path $Build "fabric-api.jar" }
if (-not (Test-Path $FabricLoaderJar) -or -not (Test-Path $FabricApiJar) -or -not $McJar) {
    Fail @"
Foabic 0.2 is Fabric-targeted. Set:
  -McJar <official Minecraft 26.1 server.jar>
  -FabricLoaderJar <fabric-loader-0.20.1.jar>
  -FabricApiJar <fabric-api-0.145.1+26.1.jar>
(or the matching FOABIC_* environment variables)
"@
}
$McJar = [System.IO.Path]::GetFullPath($McJar)
$FabricLoaderJar = [System.IO.Path]::GetFullPath($FabricLoaderJar)
$FabricApiJar = [System.IO.Path]::GetFullPath($FabricApiJar)

Write-Host "Foabic $Version (Fabric) build"
Write-Host "  JDK: $JavaHome"
Remove-Item -LiteralPath $Classes -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $Classes, $Dist | Out-Null

Write-Host "[1/6] Minecraft/Fabric classpath"
$mcBundle = Join-Path $Build "mc-bundle"
Extract-IfChanged -JarPath $McJar -Destination $mcBundle
$nested = Get-ChildItem -LiteralPath (Join-Path $mcBundle "META-INF\versions") `
        -Recurse -Filter "server-*.jar" -ErrorAction SilentlyContinue | Select-Object -First 1
if (-not $nested) { Fail "No nested server jar in $McJar; use the official server.jar bundle." }
$mcLibraries = @(Get-ChildItem -LiteralPath (Join-Path $mcBundle "META-INF\libraries") `
        -Recurse -Filter *.jar | Select-Object -ExpandProperty FullName)

$fabricBundle = Join-Path $Build "fabric-api"
Extract-IfChanged -JarPath $FabricApiJar -Destination $fabricBundle
$fabricModules = @(Get-ChildItem -LiteralPath (Join-Path $fabricBundle "META-INF\jars") `
        -Filter *.jar | Select-Object -ExpandProperty FullName)
if ($fabricModules.Count -eq 0) { Fail "No nested modules in $FabricApiJar." }

Write-Host "[2/6] API"
$api = Join-Path $Classes "api"
$loader = Join-Path $Classes "loader"
$fabric = Join-Path $Classes "fabric"
Invoke-Javac -SourceRoot (Join-Path $Root "src\api\java") -Output $api
Write-Host "[3/6] loader"
Invoke-Javac -SourceRoot (Join-Path $Root "src\loader\java") -Output $loader -Classpath @($api)
Write-Host "[4/6] Fabric adapter"
$fabricClasspath = @($nested.FullName) + $mcLibraries + $fabricModules +
        @($FabricLoaderJar, $api, $loader)
Invoke-Javac -SourceRoot (Join-Path $Root "src\fabric\java") -Output $fabric `
        -Classpath $fabricClasspath

Write-Host "[5/6] jars"
$apiJar = Join-Path $Dist "foabic-api-$Version.jar"
$fabricJar = Join-Path $Dist "foabic-fabric-$Version.jar"
Remove-Item -LiteralPath $apiJar, $fabricJar -Force -ErrorAction SilentlyContinue
& $Jar --create --file $apiJar -C $api .
$all = Join-Path $Classes "fabric-all"
New-Item -ItemType Directory -Force -Path $all | Out-Null
Copy-Item -Path (Join-Path $api "*") -Destination $all -Recurse -Force
Copy-Item -Path (Join-Path $loader "*") -Destination $all -Recurse -Force
Copy-Item -Path (Join-Path $fabric "*") -Destination $all -Recurse -Force
Copy-Item -LiteralPath (Join-Path $Root "src\fabric\resources\fabric.mod.json") -Destination $all
& $Jar --create --file $fabricJar -C $all .
if ($LASTEXITCODE -ne 0) { throw "fabric jar failed" }

Write-Host "[6/6] example mod"
$exampleClasses = Join-Path $Classes "example"
$exampleClasspath = @($apiJar, $fabricJar, $FabricLoaderJar, $nested.FullName) +
        $mcLibraries + $fabricModules
Invoke-Javac -SourceRoot (Join-Path $Root "examples\smoketest\src") -Output $exampleClasses `
        -Classpath $exampleClasspath
$exampleStage = Join-Path $Classes "example-stage"
New-Item -ItemType Directory -Force -Path $exampleStage | Out-Null
Copy-Item -Path (Join-Path $exampleClasses "*") -Destination $exampleStage -Recurse -Force
Copy-Item -Path (Join-Path $Root "examples\smoketest\resources\*") -Destination $exampleStage -Recurse -Force
$exampleJar = Join-Path $Dist "foabic-example-smoketest-$Version.jar"
Remove-Item -LiteralPath $exampleJar -Force -ErrorAction SilentlyContinue
& $Jar --create --file $exampleJar -C $exampleStage .
if ($LASTEXITCODE -ne 0) { throw "example jar failed" }

if (-not $SkipTests) {
    if (Test-Path (Join-Path $Root "src\test\java")) {
        Write-Host "tests"
        $test = Join-Path $Classes "test"
        Invoke-Javac -SourceRoot (Join-Path $Root "src\test\java") -Output $test -Classpath @($api, $loader)
        $previous = $ErrorActionPreference
        try {
            $ErrorActionPreference = "Continue"
            & $Java -cp (@($test, $api, $loader) -join ";") dev.foabic.test.AllTests
            $testExit = $LASTEXITCODE
        } finally { $ErrorActionPreference = $previous }
        if ($testExit -ne 0) { throw "tests failed" }
    }
}

Write-Host ""
Write-Host "Artifacts"
Get-ChildItem -LiteralPath $Dist -Filter "*.jar" | ForEach-Object {
    Write-Host ("  {0,-40} {1,8:N1} KiB" -f $_.Name, ($_.Length / 1KB))
}
