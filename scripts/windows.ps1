param(
  [ValidateSet('setup', 'services', 'backend', 'frontend', 'stop')]
  [string]$Action = 'setup'
)

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
$projectPath = Split-Path -Parent $PSScriptRoot
$localPath = Join-Path $projectPath '.local'
$toolsPath = Join-Path $localPath 'tools'
$settingsPath = Join-Path $localPath 'windows.json'
$databasePath = Join-Path $localPath 'postgres'
$mailpitPath = Join-Path $toolsPath 'mailpit\mailpit.exe'

function Invoke-Checked {
  param([string]$File, [string[]]$Arguments)

  & $File @Arguments
  if ($LASTEXITCODE -ne 0) { throw "$File failed with exit code $LASTEXITCODE." }
}

function Install-Archive {
  param([string]$Url, [string]$Checksum, [string]$Name, [string]$Destination)

  $archivePath = Join-Path $toolsPath $Name
  Write-Host "Downloading $Name..."
  Invoke-WebRequest -UseBasicParsing -Uri $Url -OutFile $archivePath
  if ((Get-FileHash -LiteralPath $archivePath -Algorithm SHA256).Hash -ne $Checksum) {
    throw "Checksum verification failed for $Name."
  }
  Expand-Archive -LiteralPath $archivePath -DestinationPath $Destination -Force
  Remove-Item -LiteralPath $archivePath
}

function Get-LocalMailpit {
  Get-Process -Name mailpit -ErrorAction SilentlyContinue |
    Where-Object { $_.Path -eq $mailpitPath }
}

function Start-Services {
  $pgCtl = Join-Path $settings.PostgresBin 'pg_ctl.exe'
  if (-not (Test-Path -LiteralPath (Join-Path $databasePath 'PG_VERSION'))) {
    $passwordPath = Join-Path $localPath 'database-password.tmp'
    [System.IO.File]::WriteAllText($passwordPath, 'codeduel_local')
    try {
      Invoke-Checked (Join-Path $settings.PostgresBin 'initdb.exe') @(
        '-D', $databasePath, '-U', 'codeduel', '-A', 'scram-sha-256',
        '--encoding=UTF8', '--locale=C', "--pwfile=$passwordPath"
      )
    } finally {
      Remove-Item -LiteralPath $passwordPath
    }
  }

  & $pgCtl -D $databasePath status *> $null
  if ($LASTEXITCODE -ne 0) {
    Invoke-Checked $pgCtl @(
      '-D', $databasePath, '-l', (Join-Path $localPath 'postgres.log'),
      '-o', '-p 5444 -h 127.0.0.1', '-w', 'start'
    )
  }

  $existingPassword = $env:PGPASSWORD
  $env:PGPASSWORD = 'codeduel_local'
  try {
    $exists = Invoke-Checked (Join-Path $settings.PostgresBin 'psql.exe') @(
      '-w', '-h', '127.0.0.1', '-p', '5444', '-U', 'codeduel', '-d', 'postgres',
      '-tAc', "SELECT 1 FROM pg_database WHERE datname = 'codeduel'"
    )
    if ($exists -ne '1') {
      Invoke-Checked (Join-Path $settings.PostgresBin 'createdb.exe') @(
        '-w', '-h', '127.0.0.1', '-p', '5444', '-U', 'codeduel', 'codeduel'
      )
    }
  } finally {
    $env:PGPASSWORD = $existingPassword
  }

  if (-not (Get-LocalMailpit)) {
    $process = Start-Process -FilePath $mailpitPath -WindowStyle Hidden -PassThru -ArgumentList @(
      '--listen', '127.0.0.1:8026', '--smtp', '127.0.0.1:1026',
      '--database', ('"' + (Join-Path $localPath 'mailpit.db') + '"'), '--disable-version-check'
    ) -RedirectStandardOutput (Join-Path $localPath 'mailpit.log') -RedirectStandardError (Join-Path $localPath 'mailpit-error.log')
    Start-Sleep -Milliseconds 500
    if ($process.HasExited) { throw 'Mailpit could not start. Check .local\mailpit-error.log.' }
  }
  Write-Host 'PostgreSQL: 127.0.0.1:5444 | Captured email: http://localhost:8026'
}

if ($Action -eq 'setup') {
  if (-not (Get-Command node -ErrorAction SilentlyContinue) -or (node --version) -notmatch '^v24\.') {
    throw 'Install Node.js 24 from https://nodejs.org before running setup.'
  }
  New-Item -ItemType Directory -Path $toolsPath -Force | Out-Null

  $javaHome = $env:JAVA_HOME
  if (-not $javaHome -or -not (Test-Path -LiteralPath (Join-Path $javaHome 'bin\javac.exe')) -or
      -not (Select-String -LiteralPath (Join-Path $javaHome 'release') -Pattern '^JAVA_VERSION="21\.' -Quiet)) {
    $java = Get-ChildItem -LiteralPath $toolsPath -Directory -Filter 'jdk-21*' | Select-Object -First 1
    if (-not $java) {
      $releases = Invoke-RestMethod -Uri 'https://api.adoptium.net/v3/assets/latest/21/hotspot?architecture=x64&image_type=jdk&os=windows&vendor=eclipse'
      $package = @($releases)[0].binary.package
      Install-Archive $package.link $package.checksum $package.name $toolsPath
      $java = Get-ChildItem -LiteralPath $toolsPath -Directory -Filter 'jdk-21*' | Select-Object -First 1
    }
    $javaHome = $java.FullName
  }

  $postgres = Get-ChildItem -Path "$env:ProgramFiles\PostgreSQL\*\bin\initdb.exe" -ErrorAction SilentlyContinue |
    Where-Object { [int]$_.Directory.Parent.Name -ge 17 } |
    Sort-Object { [int]$_.Directory.Parent.Name } -Descending | Select-Object -First 1
  if (-not $postgres) { throw 'Install PostgreSQL 17 or newer, then rerun setup.' }
  $postgresBin = $postgres.Directory.FullName
  if (Test-Path -LiteralPath $settingsPath) {
    $previous = Get-Content -LiteralPath $settingsPath -Raw | ConvertFrom-Json
    $postgresBin = $previous.PostgresBin
  }

  if (-not (Test-Path -LiteralPath $mailpitPath)) {
    Install-Archive 'https://github.com/axllent/mailpit/releases/download/v1.31.6/mailpit-windows-amd64.zip' `
      '5d5ad094bfb3bac9004f9d1105d2335fea9f8952c09951df73ae0a280e34d3b1' 'mailpit.zip' (Split-Path -Parent $mailpitPath)
  }
  @{ JavaHome = $javaHome; PostgresBin = $postgresBin } | ConvertTo-Json |
    Set-Content -LiteralPath $settingsPath -Encoding UTF8

  $env:npm_config_cache = Join-Path $localPath 'npm-cache'
  Push-Location (Join-Path $projectPath 'CodeDuelFrontend')
  try { Invoke-Checked 'npm.cmd' @('ci', '--no-audit', '--no-fund') }
  finally { Pop-Location }
}

if (-not (Test-Path -LiteralPath $settingsPath)) { throw 'Run .\scripts\windows.ps1 setup first.' }
$settings = Get-Content -LiteralPath $settingsPath -Raw | ConvertFrom-Json
$env:JAVA_HOME = $settings.JavaHome
$env:PATH = (Join-Path $env:JAVA_HOME 'bin') + ';' + $env:PATH

switch ($Action) {
  'setup' {
    Start-Services
    Write-Host 'Setup complete. Run the backend and frontend actions in separate terminals.'
    Write-Host "Select this JDK in IntelliJ: $env:JAVA_HOME"
  }
  'services' { Start-Services }
  'backend' {
    Start-Services
    $env:SPRING_PROFILES_ACTIVE = 'demo'
    $env:DATABASE_URL = 'jdbc:postgresql://127.0.0.1:5444/codeduel'
    $env:DATABASE_USER = 'codeduel'
    $env:DATABASE_PASSWORD = 'codeduel_local'
    $env:JUDGE_MODE = 'FIXTURE'
    $env:COOKIE_SECURE = 'false'
    $env:PUBLIC_URL = 'http://localhost:5173'
    $env:SMTP_HOST = '127.0.0.1'
    $env:SMTP_PORT = '1026'
    Push-Location (Join-Path $projectPath 'CodeDuelBackend')
    try { Invoke-Checked '.\mvnw.cmd' @('spring-boot:run') }
    finally { Pop-Location }
  }
  'frontend' {
    Push-Location (Join-Path $projectPath 'CodeDuelFrontend')
    try { Invoke-Checked 'npm.cmd' @('run', 'dev') }
    finally { Pop-Location }
  }
  'stop' {
    & (Join-Path $settings.PostgresBin 'pg_ctl.exe') -D $databasePath status *> $null
    if ($LASTEXITCODE -eq 0) {
      Invoke-Checked (Join-Path $settings.PostgresBin 'pg_ctl.exe') @('-D', $databasePath, '-w', 'stop')
    }
    Get-LocalMailpit | Stop-Process
  }
}
