[CmdletBinding()]
param(
    [ValidateSet("main", "test", "all")]
    [string]$Target = "all"
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$mainSourceRoot = [IO.Path]::Combine($projectRoot, "src", "main", "java")
$testSourceRoot = [IO.Path]::Combine($projectRoot, "src", "test", "java")
$mainOutput = [IO.Path]::Combine($projectRoot, "out", "main")
$testOutput = [IO.Path]::Combine($projectRoot, "out", "test")
$junitJar = [IO.Path]::Combine($projectRoot, "lib", "junit-platform-console-standalone-6.1.1.jar")

function Get-JavaSources {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Root
    )

    if (-not (Test-Path -LiteralPath $Root -PathType Container)) {
        return @()
    }

    return @(
        Get-ChildItem -LiteralPath $Root -Recurse -File -Filter "*.java" |
            Select-Object -ExpandProperty FullName
    )
}

function Invoke-Javac {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments
    )

    & javac @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "javac failed with exit code $LASTEXITCODE."
    }
}

try {
    $mainSources = @(Get-JavaSources -Root $mainSourceRoot)
    $testSources = @(Get-JavaSources -Root $testSourceRoot)

    if ($Target -in @("main", "all")) {
        if ($mainSources.Count -eq 0) {
            throw "No production Java sources were found under src/main/java."
        }

        if (Test-Path -LiteralPath $mainOutput) {
            Remove-Item -LiteralPath $mainOutput -Recurse -Force
        }
        New-Item -ItemType Directory -Path $mainOutput -Force | Out-Null

        $mainArguments = @(
            "--release", "17",
            "-encoding", "UTF-8",
            "-d", $mainOutput
        ) + $mainSources

        Write-Host "Compiling production sources to $mainOutput"
        Invoke-Javac -Arguments $mainArguments
    }

    if ($Target -in @("test", "all")) {
        if (-not (Test-Path -LiteralPath $junitJar -PathType Leaf)) {
            throw "JUnit launcher was not found at $junitJar. Download it before compiling tests."
        }
        if (-not (Test-Path -LiteralPath $mainOutput -PathType Container)) {
            throw "Production classes were not found at $mainOutput. Compile production sources first."
        }
        if ($testSources.Count -eq 0) {
            throw "No test Java sources were found under src/test/java."
        }

        if (Test-Path -LiteralPath $testOutput) {
            Remove-Item -LiteralPath $testOutput -Recurse -Force
        }
        New-Item -ItemType Directory -Path $testOutput -Force | Out-Null

        $testClassPath = $mainOutput + [IO.Path]::PathSeparator + $junitJar
        $testArguments = @(
            "--release", "17",
            "-encoding", "UTF-8",
            "-classpath", $testClassPath,
            "-d", $testOutput
        ) + $testSources

        Write-Host "Compiling test sources to $testOutput"
        Invoke-Javac -Arguments $testArguments
    }

    Write-Host "Compilation completed."
}
catch {
    Write-Error $_
    exit 1
}
