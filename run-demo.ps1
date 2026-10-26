$ErrorActionPreference = 'Stop'

Set-Location -LiteralPath $PSScriptRoot

function Require-Command([string]$Name) {
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Required command '$Name' was not found. Install it and reopen PowerShell."
    }
}

Require-Command 'docker'

Write-Host 'Starting PostgreSQL...' -ForegroundColor Cyan
docker compose up -d postgres

Write-Host 'Waiting for PostgreSQL...' -ForegroundColor Cyan
$databaseReady = $false
for ($attempt = 1; $attempt -le 30; $attempt++) {
    try {
        docker compose exec -T postgres pg_isready -U myuser -d fm_school_db | Out-Null
        if ($LASTEXITCODE -eq 0) {
            $databaseReady = $true
            break
        }
    } catch {
        # The container may still be starting.
    }
    Start-Sleep -Seconds 2
}
if (-not $databaseReady) {
    throw 'PostgreSQL did not become ready. Run: docker compose logs postgres'
}

$applicationRunning = $false
try {
    $healthResponse = Invoke-WebRequest -Uri 'http://localhost:8080/login' -UseBasicParsing -TimeoutSec 3
    if ($healthResponse.StatusCode -ge 200 -and $healthResponse.StatusCode -lt 500) {
        $applicationRunning = $true
    }
} catch {
    $applicationRunning = $false
}

if (-not $applicationRunning) {
    $mavenCommand = Join-Path $PSScriptRoot 'mvnw.cmd'
    if (-not (Test-Path -LiteralPath $mavenCommand)) {
        throw 'mvnw.cmd was not found in the project root.'
    }

    $logPath = Join-Path $PSScriptRoot 'run-demo.log'
    $errorLogPath = Join-Path $PSScriptRoot 'run-demo-error.log'
    Remove-Item -LiteralPath $logPath, $errorLogPath -Force -ErrorAction SilentlyContinue
    Write-Host "Starting Spring Boot. Logs: $logPath" -ForegroundColor Cyan
    Start-Process -FilePath $mavenCommand `
        -ArgumentList 'spring-boot:run' `
        -WorkingDirectory $PSScriptRoot `
        -RedirectStandardOutput $logPath `
        -RedirectStandardError $errorLogPath | Out-Null

    Write-Host 'Waiting for Spring Boot...' -ForegroundColor Cyan
    $applicationReady = $false
    for ($attempt = 1; $attempt -le 60; $attempt++) {
        Start-Sleep -Seconds 2
        try {
            $healthResponse = Invoke-WebRequest -Uri 'http://localhost:8080/login' -UseBasicParsing -TimeoutSec 3
            if ($healthResponse.StatusCode -ge 200 -and $healthResponse.StatusCode -lt 500) {
                $applicationReady = $true
                break
            }
        } catch {
            # Spring Boot is still starting or Maven is downloading dependencies.
        }
    }
    if (-not $applicationReady) {
        Write-Host 'Spring Boot did not become ready. Recent log output:' -ForegroundColor Red
        if (Test-Path -LiteralPath $logPath) {
            Get-Content -LiteralPath $logPath -Tail 40
        }
        if (Test-Path -LiteralPath $errorLogPath) {
            Get-Content -LiteralPath $errorLogPath -Tail 40
        }
        throw 'Application startup failed or exceeded the waiting period.'
    }
} else {
    Write-Host 'Spring Boot is already running.' -ForegroundColor Green
}

Write-Host 'Loading Admin, Teachers, Students and enrollments...' -ForegroundColor Cyan
Get-Content -LiteralPath (Join-Path $PSScriptRoot 'demo\seed-demo-data.sql') |
    docker compose exec -T postgres psql -v ON_ERROR_STOP=1 -U myuser -d fm_school_db
if ($LASTEXITCODE -ne 0) {
    throw 'Demo data could not be loaded.'
}

Write-Host 'Demo data loaded successfully.' -ForegroundColor Green
Write-Host 'Admin login: admin / 12345678' -ForegroundColor Yellow
Write-Host 'Teacher login: teacher2 / teacher2' -ForegroundColor Yellow
Write-Host 'Student login: student2 / student2' -ForegroundColor Yellow
Start-Process 'http://localhost:8080/login'
