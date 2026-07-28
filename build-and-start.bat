@echo off
setlocal
cd /d "%~dp0"

set "SERVER_WINDOW_TITLE=Bannerfall Server"

echo ========================================
echo  Building BannerfallCharters
echo ========================================
echo.

call gradlew.bat clean build
if errorlevel 1 (
    echo.
    echo Build failed. The running server was left untouched.
    pause
    exit /b 1
)

set "BUILT_JAR=%CD%\build\libs\BannerfallCharters-1.0-SNAPSHOT.jar"
for %%I in ("%CD%\..\..\server") do set "SERVER_DIR=%%~fI"
set "PLUGIN_DIR=%SERVER_DIR%\plugins"
set "TARGET_JAR=%PLUGIN_DIR%\BannerfallCharters-1.0-SNAPSHOT.jar"

if not exist "%BUILT_JAR%" (
    echo.
    echo Could not find the built plugin:
    echo %BUILT_JAR%
    pause
    exit /b 1
)

if not exist "%SERVER_DIR%" (
    echo.
    echo Could not find the server folder:
    echo %SERVER_DIR%
    pause
    exit /b 1
)

if not exist "%PLUGIN_DIR%" mkdir "%PLUGIN_DIR%"

echo.
echo Closing any existing Paper server...

rem Close servers started by this script, including their Java child process.
taskkill /F /T /FI "WINDOWTITLE eq %SERVER_WINDOW_TITLE%*" >nul 2>&1

rem Also catch an older server instance that was started without the window title.
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "$servers = @(Get-CimInstance Win32_Process -ErrorAction SilentlyContinue).Where({ ($_.Name -eq 'java.exe' -or $_.Name -eq 'javaw.exe') -and $_.CommandLine -and $_.CommandLine.ToLower().Replace([char]34, '').Contains('-jar paper.jar') }); if ($servers.Count -eq 0) { Write-Host 'No matching Paper Java process found.'; exit 0 }; foreach ($server in $servers) { Write-Host ('Stopping Paper server PID ' + $server.ProcessId + '...'); Stop-Process -Id $server.ProcessId -Force -ErrorAction Stop }; Start-Sleep -Seconds 2"

if errorlevel 1 (
    echo.
    echo The server process could not be stopped.
    echo Try right-clicking this script and choosing "Run as administrator".
    pause
    exit /b 1
)

echo.
echo Copying plugin to the server...
copy /Y "%BUILT_JAR%" "%TARGET_JAR%" >nul
if errorlevel 1 (
    echo.
    echo Failed to copy the plugin JAR.
    echo Make sure the old server is fully closed.
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
