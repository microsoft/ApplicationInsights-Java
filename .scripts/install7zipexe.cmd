@echo off

echo "Installing 7-Zip Windows..."

set ZIP_URL=https://www.7-zip.org/a/7z2101-x64.exe

powershell -NoProfile -ExecutionPolicy Unrestricted -File "%~dp0install-azcopy.ps1" -Url "%ZIP_URL%" -Destination "C:\7-Zip" -SkipUnzip -CleanOnFinish || exit /B 1

IF EXIST "C:\7-Zip\7z.exe" (
  echo "Installed 7-Zip Successfully."
) ELSE (
  echo "Failed to install 7-Zip."
)

