@echo off
setlocal EnableExtensions EnableDelayedExpansion
cd /d "%~dp0"

set "SERVER_WINDOW_TITLE=Bannersbane Server"
set "PLUGIN_NAME=Bannersbane"
set "BUILD_LIBS=%CD%\build\libs"

for %%I in ("%CD%\..\..\server") do set "SERVER_DIR=%%~fI"
set "PLUGIN_DIR=%SERVER_DIR%\plugins"

if not exist "%SERVER_DIR%" (
    echo.
    echo Could not find the server folder:
    echo %SERVER_DIR%
    pause
    exit /b 1
)

if not exist "%PLUGIN_DIR%" mkdir "%PLUGIN_DIR%"

echo ========================================
echo  Stopping Bannersbane Server
echo ========================================
echo.

taskkill /F /T /FI "WINDOWTITLE eq %SERVER_WINDOW_TITLE%*" >nul 2>&1

powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "$deadline = (Get-Date).AddSeconds(20); " ^
    "do { " ^
    "  $servers = @(Get-CimInstance Win32_Process -ErrorAction SilentlyContinue | Where-Object { " ^
    "    ($_.Name -eq 'java.exe' -or $_.Name -eq 'javaw.exe') -and " ^
    "    $_.CommandLine -and " ^
    "    $_.CommandLine.ToLower().Replace([char]34, '').Contains('-jar paper.jar') " ^
    "  }); " ^
    "  foreach ($server in $servers) { " ^
    "    Write-Host ('Stopping Paper server PID ' + $server.ProcessId + '...'); " ^
    "    Stop-Process -Id $server.ProcessId -Force -ErrorAction SilentlyContinue " ^
    "  }; " ^
    "  if ($servers.Count -gt 0) { Start-Sleep -Milliseconds 500 } " ^
    "} while ($servers.Count -gt 0 -and (Get-Date) -lt $deadline); " ^
    "$remaining = @(Get-CimInstance Win32_Process -ErrorAction SilentlyContinue | Where-Object { " ^
    "  ($_.Name -eq 'java.exe' -or $_.Name -eq 'javaw.exe') -and " ^
    "  $_.CommandLine -and " ^
    "  $_.CommandLine.ToLower().Replace([char]34, '').Contains('-jar paper.jar') " ^
    "}); " ^
    "if ($remaining.Count -gt 0) { Write-Host 'The Paper Java process is still running.'; exit 1 }; " ^
    "Write-Host 'Paper server is fully stopped.'"

if errorlevel 1 (
    echo.
    echo The server process could not be stopped.
    echo Try right-clicking this script and choosing "Run as administrator".
    pause
    exit /b 1
)

echo.
echo ========================================
echo  Building Bannersbane
echo ========================================
echo.

call gradlew.bat clean build

if errorlevel 1 (
    echo.
    echo Build failed. The server remains stopped so the old plugin is not restarted accidentally.
    pause
    exit /b 1
)

set "BUILT_JAR="

for /f "delims=" %%F in ('dir /b /a-d /o-d "%BUILD_LIBS%\%PLUGIN_NAME%-*.jar" 2^>nul') do (
    set "CANDIDATE=%%F"

    echo !CANDIDATE! | findstr /I /R "\-sources\.jar$ \-javadoc\.jar$" >nul
    if errorlevel 1 (
        if /I "%%~xF"==".jar" (
            set "BUILT_JAR=%BUILD_LIBS%\%%F"
            goto :FoundBuiltJar
        )
    )
)

:FoundBuiltJar
if not defined BUILT_JAR (
    echo.
    echo Could not find a built Bannersbane JAR in:
    echo %BUILD_LIBS%
    echo.
    echo Expected something like:
    echo Bannersbane-1.0.jar
    pause
    exit /b 1
)

for %%I in ("%BUILT_JAR%") do set "BUILT_FILENAME=%%~nxI"

if /I not "%BUILT_JAR:~-4%"==".jar" (
    echo.
    echo Safety check failed: built file is not a .jar:
    echo %BUILT_JAR%
    pause
    exit /b 1
)

echo Found built plugin:
echo %BUILT_FILENAME%
echo.

powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "$files = @(Get-ChildItem -LiteralPath $env:PLUGIN_DIR -File -ErrorAction SilentlyContinue | Where-Object { " ^
    "  $_.Extension -ieq '.jar' -and $_.BaseName -like ($env:PLUGIN_NAME + '-*') " ^
    "}); " ^
    "$deadline = (Get-Date).AddSeconds(20); " ^
    "foreach ($file in $files) { " ^
    "  do { " ^
    "    try { " ^
    "      $stream = [System.IO.File]::Open($file.FullName, [System.IO.FileMode]::Open, [System.IO.FileAccess]::ReadWrite, [System.IO.FileShare]::None); " ^
    "      $stream.Close(); break " ^
    "    } catch { Start-Sleep -Milliseconds 500 } " ^
    "  } while ((Get-Date) -lt $deadline); " ^
    "  try { " ^
    "    $stream = [System.IO.File]::Open($file.FullName, [System.IO.FileMode]::Open, [System.IO.FileAccess]::ReadWrite, [System.IO.FileShare]::None); " ^
    "    $stream.Close() " ^
    "  } catch { " ^
    "    Write-Host ('The plugin JAR is still in use: ' + $file.FullName); exit 1 " ^
    "  } " ^
    "}"

if errorlevel 1 (
    echo.
    echo An old Bannersbane plugin JAR is still locked and cannot be replaced.
    pause
    exit /b 1
)

echo Removing old Bannersbane plugin versions...

for %%F in ("%PLUGIN_DIR%\%PLUGIN_NAME%-*.jar") do (
    if exist "%%~fF" (
        if /I "%%~xF"==".jar" (
            echo Deleting %%~nxF
            del /F /Q "%%~fF"
            if errorlevel 1 (
                echo.
                echo Failed to delete:
                echo %%~fF
                pause
                exit /b 1
            )
        )
    )
)

echo.
echo Copying %BUILT_FILENAME% to the server...
copy /Y "%BUILT_JAR%" "%PLUGIN_DIR%\%BUILT_FILENAME%" >nul

if errorlevel 1 (
    echo.
    echo Failed to copy the plugin JAR.
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
