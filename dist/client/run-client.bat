@echo off
REM ============================================================
REM  HamRadio Client Launcher for Windows
REM  Connects to server running in WSL
REM ============================================================

REM --- Configuration ---
SET JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot
SET JAVAFX_PATH=%~dp0javafx-sdk-21.0.2\lib
SET JAR_PATH=%~dp0hamradio-client.jar
SET SERVER_HOST=SET_YOUR_WSL_IP_HERE
SET SERVER_PORT=7100

REM --- Auto-detect WSL IP if not set ---
if "%SERVER_HOST%"=="SET_YOUR_WSL_IP_HERE" (
    echo.
    echo  !! You need to set the WSL IP address !!
    echo.
    echo  Run this in WSL to find the IP:
    echo    hostname -I
    echo.
    echo  Then edit this .bat file and set SERVER_HOST=your_wsl_ip
    echo  Or pass it as argument: run-client.bat 172.x.x.x
    echo.
    if not "%~1"=="" (
        SET SERVER_HOST=%~1
    ) else (
        pause
        exit /b 1
    )
)

echo ============================================================
echo  HamRadio Client
echo  Server: %SERVER_HOST%:%SERVER_PORT%
echo ============================================================
echo.

REM --- Check Java ---
if not exist "%JAVA_HOME%\bin\java.exe" (
    echo ERROR: Java not found at %JAVA_HOME%
    echo.
    echo Download JDK 21 from: https://adoptium.net/
    echo Install to: C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot
    echo Or edit JAVA_HOME in this .bat file.
    pause
    exit /b 1
)

REM --- Check JavaFX ---
if not exist "%JAVAFX_PATH%" (
    echo ERROR: JavaFX SDK not found at %JAVAFX_PATH%
    echo.
    echo Download JavaFX 21 SDK for Windows from:
    echo   https://gluonhq.com/products/javafx/
    echo.
    echo Extract to: %~dp0javafx-sdk-21.0.2\
    pause
    exit /b 1
)

REM --- Launch ---
"%JAVA_HOME%\bin\java.exe" ^
    --module-path "%JAVAFX_PATH%" ^
    --add-modules javafx.controls,javafx.fxml ^
    -cp "%JAR_PATH%" ^
    com.hamradio.client.HamRadioClient

pause
