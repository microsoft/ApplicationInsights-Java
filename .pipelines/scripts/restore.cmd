@echo off

echo "Installing Java..."
powershell -file "%~dp0install-java.ps1" || exit /b %ERRORLEVEL%

echo "C:\Program Files\Java\zulu-11-azure-jdk_11.39.15-11.0.7-win_x64"
dir "C:\Program Files\Java\zulu-11-azure-jdk_11.39.15-11.0.7-win_x64"

echo "Installed Java Successfully."