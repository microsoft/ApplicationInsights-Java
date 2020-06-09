@echo off

echo "Installing Java..."
powershell -NoProfile -ExecutionPolicy Unrestricted -File "%~dp0install-java.ps1" || exit /B 1

echo "C:\Program Files\Java\zulu-11-azure-jdk_11.39.15-11.0.7-win_x64"
dir "C:\Program Files\Java\zulu-11-azure-jdk_11.39.15-11.0.7-win_x64"

echo "Installed Java Successfully."
