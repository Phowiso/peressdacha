@echo off
cd /d "%~dp0"
call lein deps
if errorlevel 1 exit /b 1
call lein test
if errorlevel 1 exit /b 1
call lein uberjar
if errorlevel 1 exit /b 1
echo Build completed.
