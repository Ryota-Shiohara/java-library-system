[CmdletBinding()]
param(
    [switch]$NoBuild
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$mainOutput = [IO.Path]::Combine($projectRoot, "out", "main")
$testOutput = [IO.Path]::Combine($projectRoot, "out", "test")
$reportDirectory = [IO.Path]::Combine($projectRoot, "out", "test-reports")
$junitJar = [IO.Path]::Combine($projectRoot, "lib", "junit-platform-console-standalone-6.1.1.jar")
$compileScript = Join-Path $PSScriptRoot "compile.ps1"

try {
    if (-not $NoBuild) {
        & $compileScript -Target all
        if ($LASTEXITCODE -ne 0) {
            exit $LASTEXITCODE
        }
    }

    foreach ($path in @($junitJar, $mainOutput, $testOutput)) {
        if (-not (Test-Path -LiteralPath $path)) {
            throw "Required test input was not found: $path"
        }
    }

    if (Test-Path -LiteralPath $reportDirectory) {
        Remove-Item -LiteralPath $reportDirectory -Recurse -Force
    }
    New-Item -ItemType Directory -Path $reportDirectory -Force | Out-Null

    $classPath = $mainOutput + [IO.Path]::PathSeparator + $testOutput
    $launcherArguments = @(
        "execute",
        "--class-path", $classPath,
        "--scan-class-path",
        "--fail-if-no-tests",
        "--reports-dir", $reportDirectory
    )

    Write-Host "Running JUnit tests. Reports will be written to $reportDirectory"
    & java -jar $junitJar @launcherArguments
    exit $LASTEXITCODE
}
catch {
    Write-Error $_
    exit 1
}
