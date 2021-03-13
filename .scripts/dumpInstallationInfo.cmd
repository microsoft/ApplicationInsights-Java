@echo off

echo "Listing installed software..."
powershell -NoProfile -ExecutionPolicy Unrestricted -File "%~dp0List-Programs.ps1" || exit /B 1
echo "Finished listing installed software."