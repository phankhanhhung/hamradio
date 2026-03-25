# ============================================================
#  HamRadio Client Launcher for Windows (PowerShell)
#  Connects to server running in WSL
# ============================================================

param(
    [string]$ServerHost = "",
    [int]$ServerPort = 7100
)

# --- Auto-detect WSL IP ---
if (-not $ServerHost) {
    Write-Host ""
    Write-Host "Detecting WSL IP..." -ForegroundColor Yellow
    $wslIp = wsl hostname -I 2>$null
    if ($wslIp) {
        $ServerHost = $wslIp.Trim().Split(" ")[0]
        Write-Host "Found WSL IP: $ServerHost" -ForegroundColor Green
    } else {
        Write-Host "Could not auto-detect WSL IP." -ForegroundColor Red
        Write-Host "Usage: .\run-client.ps1 -ServerHost 172.x.x.x"
        exit 1
    }
}

# --- Find Java ---
$javaHome = $null
$candidates = @(
    "C:\Program Files\Eclipse Adoptium\jdk-21*",
    "C:\Program Files\Java\jdk-21*",
    "C:\Program Files\Microsoft\jdk-21*"
)
foreach ($pattern in $candidates) {
    $found = Get-ChildItem $pattern -Directory -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($found) { $javaHome = $found.FullName; break }
}
if (-not $javaHome) {
    # Try JAVA_HOME env
    if ($env:JAVA_HOME) { $javaHome = $env:JAVA_HOME }
}
if (-not $javaHome) {
    Write-Host "ERROR: JDK 21 not found. Download from https://adoptium.net/" -ForegroundColor Red
    exit 1
}
$java = Join-Path $javaHome "bin\java.exe"
Write-Host "Java: $java"

# --- Find JavaFX ---
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$javafxLib = Join-Path $scriptDir "javafx-sdk-21.0.2\lib"
if (-not (Test-Path $javafxLib)) {
    Write-Host "ERROR: JavaFX SDK not found at $javafxLib" -ForegroundColor Red
    Write-Host "Download from: https://gluonhq.com/products/javafx/"
    Write-Host "Extract javafx-sdk-21.0.2 folder next to this script."
    exit 1
}

$jarPath = Join-Path $scriptDir "hamradio-client.jar"

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  HamRadio Client"
Write-Host "  Server: ${ServerHost}:${ServerPort}"
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

& $java `
    --module-path $javafxLib `
    --add-modules javafx.controls,javafx.fxml `
    -cp $jarPath `
    com.hamradio.client.HamRadioClient
