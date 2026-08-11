# Pack Fenyuan_liquor_industry frontend + backend into nginx apps/Fenyuan_liquor_industry
$ErrorActionPreference = 'Stop'

$Root = Split-Path -Parent $PSScriptRoot
$NginxApps = 'D:\E\nginx-1.26.3\nginx-1.26.3\apps\Fenyuan_liquor_industry'
$Frontend = Join-Path $Root 'frontend'
$Backend = Join-Path $Root 'backend'

Write-Host '==> Build frontend (base=/Fenyuan_liquor_industry/)' -ForegroundColor Cyan
Push-Location $Frontend
try {
  if (-not (Test-Path 'node_modules')) {
    npm install
  }
  npm run build
} finally {
  Pop-Location
}

Write-Host '==> Build backend jar' -ForegroundColor Cyan
$MvnCandidates = @(
  (Join-Path $Root 'tools\apache-maven-3.9.6\bin\mvn.cmd'),
  (Join-Path $Root 'tools\maven\bin\mvn.cmd'),
  'mvn'
)
$Mvn = $MvnCandidates | Where-Object {
  $_ -eq 'mvn' -or (Test-Path $_)
} | Select-Object -First 1
if (-not $Mvn) { throw 'mvn not found. Install Maven or place it under tools/apache-maven-3.9.6' }

Push-Location $Backend
try {
  & $Mvn -q -DskipTests package
  if ($LASTEXITCODE -ne 0) { throw "Maven package failed with exit $LASTEXITCODE" }
} finally {
  Pop-Location
}

$JarSrc = Join-Path $Backend 'target\admin.jar'
if (-not (Test-Path $JarSrc)) {
  throw "Backend jar not found: $JarSrc"
}

Write-Host "==> Deploy to $NginxApps" -ForegroundColor Cyan
New-Item -ItemType Directory -Force -Path $NginxApps | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $NginxApps 'app') | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $NginxApps 'logs') | Out-Null

# Clear old frontend assets; keep app/, logs/, WinSW service files
$Keep = @(
  'app',
  'logs',
  'start-backend.bat',
  'fenyuan-liquor-service.exe',
  'fenyuan-liquor-service.xml'
)
Get-ChildItem $NginxApps -Force | Where-Object {
  $_.Name -notin $Keep
} | Remove-Item -Recurse -Force

$Dist = Join-Path $Frontend 'dist'
Copy-Item -Path (Join-Path $Dist '*') -Destination $NginxApps -Recurse -Force
Copy-Item -Path $JarSrc -Destination (Join-Path $NginxApps 'app\admin.jar') -Force

# WinSW service definition (same pattern as LeasingEntityService / AtourlyService)
$SvcXmlSrc = Join-Path $PSScriptRoot 'fenyuan-liquor-service.xml'
$SvcXmlDst = Join-Path $NginxApps 'fenyuan-liquor-service.xml'
$SvcExeDst = Join-Path $NginxApps 'fenyuan-liquor-service.exe'
Copy-Item -Path $SvcXmlSrc -Destination $SvcXmlDst -Force
if (-not (Test-Path $SvcExeDst)) {
  $WinSwCandidates = @(
    (Join-Path $NginxApps '..\leasing_entity\leasing-entity-service.exe'),
    (Join-Path $NginxApps '..\atourly\atourly-service.exe'),
    'D:\E\nginx-1.26.3\nginx-1.26.3\fengchi-service.exe'
  )
  $WinSw = $WinSwCandidates | Where-Object { Test-Path $_ } | Select-Object -First 1
  if (-not $WinSw) { throw 'WinSW exe not found (need leasing-entity-service.exe or atourly-service.exe to copy)' }
  Copy-Item -Path $WinSw -Destination $SvcExeDst -Force
  Write-Host "  Copied WinSW -> fenyuan-liquor-service.exe" -ForegroundColor DarkGray
}

$StartBat = @'
@echo off
REM Prefer Windows service. Manual fallback if service not installed.
cd /d "%~dp0"
sc query FenyuanLiquorService >nul 2>&1
if %ERRORLEVEL%==0 (
  echo Starting FenyuanLiquorService ...
  net start FenyuanLiquorService
  goto :eof
)
cd /d "%~dp0app"
echo Service not installed. Starting admin.jar manually on :6001 ...
java -jar admin.jar --spring.profiles.active=prod
pause
'@
Set-Content -Path (Join-Path $NginxApps 'start-backend.bat') -Value $StartBat -Encoding ASCII

Write-Host 'Done.' -ForegroundColor Green
Write-Host '  Frontend: https://www.zszy.cc/Fenyuan_liquor_industry/login'
Write-Host '  API:      https://www.zszy.cc/Fenyuan_liquor_industry/api/* -> 127.0.0.1:6001/api/*'
Write-Host '  Service:  FenyuanLiquorService'
Write-Host '    install: apps\Fenyuan_liquor_industry\fenyuan-liquor-service.exe install'
Write-Host '    start:   net start FenyuanLiquorService'
Write-Host '    stop:    net stop FenyuanLiquorService'
Write-Host '    restart: apps\Fenyuan_liquor_industry\fenyuan-liquor-service.exe restart'
Write-Host '  Nginx:    nginx -s reload'
