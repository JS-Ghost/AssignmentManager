@echo off
REM ==============================================================================
REM Launch Assignment Manager on Windows
REM ==============================================================================
title Assignment Manager

echo [1/2] Compiling Java sources...
if not exist "build\classes" mkdir "build\classes"
javac -encoding UTF-8 -cp "lib/*" -d build\classes src\assignmentmanager\*.java

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Compilation failed. Please ensure JDK 17+ is installed.
    pause
    exit /b %ERRORLEVEL%
)

echo [2/2] Launching Assignment Manager...
start javaw -cp "build\classes;lib\*" assignmentmanager.AssignmentManager

exit /b 0
