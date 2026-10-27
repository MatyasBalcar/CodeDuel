param(
  [ValidateSet('setup', 'start', 'stop', 'status', 'test', 'update')]
  [string]$Action = 'setup',
  [string]$Distro = 'Ubuntu-24.04'
)

$ErrorActionPreference = 'Stop'
$scriptPath = Join-Path $PSScriptRoot 'windows-vm.py'
$linuxScript = (& wsl.exe -d $Distro -u root --exec wslpath -u $scriptPath).Trim()
if ($LASTEXITCODE -ne 0) { throw "Cannot open $Distro. Set up the Ubuntu Docker Engine first." }
& wsl.exe -d $Distro -u root --exec python3 -u $linuxScript $Action
if ($LASTEXITCODE -ne 0) { throw "Executor $Action failed. Check the output above." }
