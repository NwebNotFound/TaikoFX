# AI-assisted developer launcher, 2026-10-13. See docs/AI_USAGE.md.
param([Parameter(ValueFromRemainingArguments = $true)][string[]] $GradleArgs)
$ErrorActionPreference = 'Stop'
if (-not $GradleArgs -or $GradleArgs.Count -eq 0) { $GradleArgs = @('run') }

function Test-Jdk25([string] $Candidate) {
    if (-not $Candidate -or -not (Test-Path -LiteralPath (Join-Path $Candidate 'bin\javac.exe'))) { return $false }
    $releaseFile = Join-Path $Candidate 'release'
    return (Test-Path -LiteralPath $releaseFile) -and
        [bool](Select-String -LiteralPath $releaseFile -Pattern '^JAVA_VERSION="25(?:[."]|$)' -Quiet)
}

$candidates = @($env:JAVA_HOME)
$searchRoots = @((Join-Path $env:USERPROFILE '.jdks'),
    (Join-Path $env:ProgramFiles 'Eclipse Adoptium'), (Join-Path $env:ProgramFiles 'Java'))
foreach ($searchRoot in $searchRoots) {
    if (Test-Path -LiteralPath $searchRoot) {
        $candidates += @(Get-ChildItem -LiteralPath $searchRoot -Directory | Select-Object -ExpandProperty FullName)
    }
}
$jdk = $candidates | Where-Object { Test-Jdk25 $_ } | Select-Object -First 1
if (-not $jdk) { throw 'Install a JDK 25 (for example Eclipse Temurin 25), then set JAVA_HOME to its root folder. See README.md.' }

$previousJavaHome = $env:JAVA_HOME
$previousPath = $env:Path
Push-Location $PSScriptRoot
try {
    $env:JAVA_HOME = $jdk
    $env:Path = (Join-Path $jdk 'bin') + ';' + $env:Path
    Write-Host "Using JDK: $jdk"
    & (Join-Path $PSScriptRoot 'gradlew.bat') @GradleArgs
    $gradleExit = $LASTEXITCODE
} finally {
    $env:JAVA_HOME = $previousJavaHome
    $env:Path = $previousPath
    Pop-Location
}
exit $gradleExit
