@echo off
setlocal EnableExtensions
cd /d "%~dp0"

set "SERVER_WINDOW_TITLE=Bannerfall Server"
set "BUILT_JAR=%CD%\build\libs\BannerfallCharters-1.0-SNAPSHOT.jar"
for %%I in ("%CD%\..\..\server") do set "SERVER_DIR=%%~fI"
set "PLUGIN_DIR=%SERVER_DIR%\plugins"
set "TARGET_JAR=%PLUGIN_DIR%\BannerfallCharters-1.0-SNAPSHOT.jar"

if not exist "%SERVER_DIR%" (
    echo.
    echo Could not find the server folder:
    echo %SERVER_DIR%
    pause
    exit /b 1
)

if not exist "%PLUGIN_DIR%" mkdir "%PLUGIN_DIR%"

echo ========================================
echo  Stopping Bannerfall Server
echo ========================================
echo.

rem First close the server window and every child process started from it.
taskkill /F /T /FI "WINDOWTITLE eq %SERVER_WINDOW_TITLE%*" >nul 2>&1

rem Catch Paper instances started another way, then wait until Java is truly gone.
powershell -NoProfile -ExecutionPolicy Bypass -Command "$deadline = (Get-Date).AddSeconds(20); do { $servers = @(Get-CimInstance Win32_Process -ErrorAction SilentlyContinue | Where-Object { ($_.Name -eq 'java.exe' -or $_.Name -eq 'javaw.exe') -and $_.CommandLine -and $_.CommandLine.ToLower().Replace([char]34, '').Contains('-jar paper.jar') }); foreach ($server in $servers) { Write-Host ('Stopping Paper server PID ' + $server.ProcessId + '...'); Stop-Process -Id $server.ProcessId -Force -ErrorAction SilentlyContinue }; if ($servers.Count -gt 0) { Start-Sleep -Milliseconds 500 } } while ($servers.Count -gt 0 -and (Get-Date) -lt $deadline); $remaining = @(Get-CimInstance Win32_Process -ErrorAction SilentlyContinue | Where-Object { ($_.Name -eq 'java.exe' -or $_.Name -eq 'javaw.exe') -and $_.CommandLine -and $_.CommandLine.ToLower().Replace([char]34, '').Contains('-jar paper.jar') }); if ($remaining.Count -gt 0) { Write-Host 'The Paper Java process is still running.'; exit 1 }; Write-Host 'Paper server is fully stopped.'"

if errorlevel 1 (
    echo.
    echo The server process could not be stopped.
    echo Try right-clicking this script and choosing "Run as administrator".
    pause
    exit /b 1
)

rem Wait until Windows releases the old plugin JAR before building or copying.
powershell -NoProfile -ExecutionPolicy Bypass -Command "$path = $env:TARGET_JAR; if (-not (Test-Path -LiteralPath $path)) { exit 0 }; $deadline = (Get-Date).AddSeconds(20); do { try { $stream = [System.IO.File]::Open($path, [System.IO.FileMode]::Open, [System.IO.FileAccess]::ReadWrite, [System.IO.FileShare]::None); $stream.Close(); exit 0 } catch { Start-Sleep -Milliseconds 500 } } while ((Get-Date) -lt $deadline); Write-Host ('The plugin JAR is still in use: ' + $path); exit 1"

if errorlevel 1 (
    echo.
    echo The old plugin JAR is still locked and cannot be replaced.
    pause
    exit /b 1
)

echo.
echo ========================================
echo  Building BannerfallCharters
echo ========================================
echo.

call gradlew.bat clean build
if errorlevel 1 (
    echo.
    echo Build failed. The server remains stopped so the old plugin is not restarted accidentally.
    pause
    exit /b 1
)

if not exist "%BUILT_JAR%" (
    echo.
    echo Could not find the built plugin:
    echo %BUILT_JAR%
    pause
    exit /b 1
)

echo.
echo Copying plugin to the server...
copy /Y "%BUILT_JAR%" "%TARGET_JAR%" >nul
if errorlevel 1 (
    echo.
    echo Failed to copy the plugin JAR.
    echo The server is stopped, but Windows still prevented the replacement.
    pause
    exit /b 1
)

echo Plugin copied successfully.
echo.
echo Starting Paper server...
echo ========================================
echo.

pushd "%SERVER_DIR%"
if exist "start.bat" (
    start "%SERVER_WINDOW_TITLE%" cmd /c call start.bat
) else (
    start "%SERVER_WINDOW_TITLE%" cmd /k "java -Xms4G -Xmx4G -jar paper.jar --nogui"
)
popd

endlocal
exit /b 0
