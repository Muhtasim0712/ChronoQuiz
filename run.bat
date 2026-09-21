@echo off
title ChronoQuiz Runner
echo ========================================================
echo Starting ChronoQuiz Desktop Application...
echo ========================================================
"C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.1\plugins\maven-plugin\lib\maven3\bin\mvn.cmd" compile exec:java -Dexec.mainClass="com.chronoquiz.Launcher"
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo Application exited or encountered an error. Press any key to close.
    pause >nul
)
