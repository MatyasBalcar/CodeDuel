param(
  [ValidateSet('all', 'backend', 'problems', 'frontend', 'browser')]
  [string]$Only = 'all',
  [switch]$Wsl,
  [switch]$Fix,
  [switch]$Browser,
  [string]$Distro = 'Ubuntu-24.04'
)

$ErrorActionPreference = 'Stop'
$projectPath = Split-Path -Parent $PSScriptRoot
$mavenImage = 'maven:3.9-eclipse-temurin-21@sha256:99e61abcff91a9b1333463bd8451fb18495d6eba9250ac66a338b518f8278320'

function Invoke-Checked {
  param([string]$File, [string[]]$Arguments)
  & $File @Arguments
  if ($LASTEXITCODE -ne 0) { throw "$File failed with exit code $LASTEXITCODE. Fix that check, then rerun it." }
}

function Invoke-Backend {
  param([string[]]$Goals)
  if ($Wsl) {
    Invoke-Checked 'wsl.exe' (@('-d', $Distro, '-u', 'root', '--exec', 'docker', 'run', '--rm', '--network', 'host',
      '--mount', "type=bind,source=$linuxProject,target=/build",
      '--mount', 'type=volume,source=codeduel_maven_cache,target=/root/.m2',
      '--mount', 'type=bind,source=/var/run/docker.sock,target=/var/run/docker.sock',
      '-w', '/build', $mavenImage, 'sh', './mvnw', '-B', '-ntp') + $Goals)
  } else {
    Invoke-Checked (Join-Path $projectPath 'mvnw.cmd') (@('-B', '-ntp') + $Goals)
  }
}

function Invoke-TestCompose {
  param([string[]]$Arguments)
  Invoke-Checked 'docker' (@('compose', '-p', $testProject, '-f', 'docker-compose.yml', '-f', 'docker-compose.test.yml') + $Arguments)
}

Push-Location $projectPath
try {
  if ($Wsl) {
    $linuxProject = (& wsl.exe -d $Distro -u root --exec wslpath -u $projectPath).Trim()
    if ($LASTEXITCODE -ne 0) { throw "Cannot open $Distro. See docs/testing.md." }
  } elseif ($Only -in @('all', 'backend', 'problems')) {
    $javaVersion = (& java -version 2>&1 | Out-String)
    if ($javaVersion -notmatch 'version "21\.') { throw 'Use JDK 21, or add -Wsl for the Linux Docker setup. See docs/testing.md.' }
  }

  if ($Only -in @('all', 'backend')) {
    Write-Host 'Checking backend formatting, unit tests and PostgreSQL integration tests...'
    if ($Fix) { Invoke-Backend @('-f', 'CodeDuelBackend/pom.xml', 'spotless:apply') }
    Invoke-Backend @('verify')
  }

  if ($Only -in @('all', 'problems')) {
    Write-Host 'Checking every trusted reference solution...'
    if ($Wsl) {
      $jdkDirectory = '/opt/codeduel-java21'
      & wsl.exe -d $Distro -u root --exec test -x "$jdkDirectory/bin/javac"
      if ($LASTEXITCODE -ne 0) {
        $container = (& wsl.exe -d $Distro -u root --exec docker create $mavenImage).Trim()
        if ($LASTEXITCODE -ne 0) { throw 'Cannot prepare the Java 21 tools.' }
        try { Invoke-Checked 'wsl.exe' @('-d', $Distro, '-u', 'root', '--exec', 'docker', 'cp', "${container}:/opt/java/openjdk", $jdkDirectory) }
        finally { Invoke-Checked 'wsl.exe' @('-d', $Distro, '-u', 'root', '--exec', 'docker', 'rm', $container) }
      }
      Invoke-Checked 'wsl.exe' @('-d', $Distro, '-u', 'root', '--exec', '/usr/bin/env',
        "JAVA_HOME=$jdkDirectory", "PATH=${jdkDirectory}/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
        'python3', '-u', "$linuxProject/problems/verify_catalog.py")
    } else {
      Invoke-Checked 'python' @('problems/verify_catalog.py')
    }
  }

  if ($Only -in @('all', 'frontend')) {
    Write-Host 'Checking frontend formatting, lint, tests, production build and dependencies...'
    Push-Location (Join-Path $projectPath 'CodeDuelFrontend')
    try {
      Invoke-Checked 'npm.cmd' @('ci')
      if ($Fix) { Invoke-Checked 'npm.cmd' @('run', 'format') }
      foreach ($command in @('format:check', 'lint', 'test', 'build')) {
        Invoke-Checked 'npm.cmd' @('run', $command)
      }
      Invoke-Checked 'npm.cmd' @('audit', '--audit-level=moderate')
    } finally { Pop-Location }
  }

  if ($Browser -or $Only -eq 'browser') {
    Write-Host 'Building the demo and running the Chromium journeys...'
    $testProject = 'codeduel-check-' + [guid]::NewGuid().ToString('N').Substring(0, 8)
    try {
      Invoke-TestCompose @('up', '-d', '--build', 'db', 'mailpit', 'api', 'frontend')
      $ready = $false
      for ($attempt = 0; $attempt -lt 90; $attempt++) {
        try {
          $system = Invoke-RestMethod -Uri 'http://127.0.0.1:5175/api/v1/system' -TimeoutSec 2
          $nova = Invoke-RestMethod -Uri 'http://127.0.0.1:5175/api/v1/users/nova' -TimeoutSec 2
          $atlas = Invoke-RestMethod -Uri 'http://127.0.0.1:5175/api/v1/users/atlas' -TimeoutSec 2
          if ($system.judgeMode -eq 'FIXTURE' -and $nova.username -eq 'nova' -and $atlas.username -eq 'atlas') {
            $ready = $true
            break
          }
        } catch {}
        Start-Sleep -Seconds 1
      }
      if (-not $ready) { throw 'The test demo did not finish seeding in fixture mode.' }
      $previousBaseUrl = $env:E2E_BASE_URL
      $previousMailpitUrl = $env:MAILPIT_URL
      $previousCi = $env:CI
      Push-Location (Join-Path $projectPath 'CodeDuelFrontend')
      try {
        $env:E2E_BASE_URL = 'http://127.0.0.1:5175'
        $env:MAILPIT_URL = 'http://127.0.0.1:8027'
        $env:CI = 'true'
        if (-not (Test-Path -LiteralPath 'node_modules/.bin/playwright.cmd')) { Invoke-Checked 'npm.cmd' @('ci') }
        Invoke-Checked 'npx.cmd' @('playwright', 'install', 'chromium')
        Invoke-Checked 'npm.cmd' @('run', 'test:e2e')
      } finally {
        $env:E2E_BASE_URL = $previousBaseUrl
        $env:MAILPIT_URL = $previousMailpitUrl
        $env:CI = $previousCi
        Pop-Location
      }
    } catch {
      Invoke-TestCompose @('logs', '--tail', '100', 'api', 'frontend')
      throw
    } finally {
      Invoke-TestCompose @('down', '--volumes', '--remove-orphans')
    }
  }
  Write-Host 'The selected checks passed.'
} finally { Pop-Location }
