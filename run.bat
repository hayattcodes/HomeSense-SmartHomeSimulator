@echo off
cd /d "%~dp0"
if not exist out mkdir out
dir /s /b src\*.java > sources.txt
javac -d out @sources.txt
if errorlevel 1 exit /b 1
echo.
echo Starting HomeSense on http://localhost:8080
java -cp out smarthome.web.WebApp
