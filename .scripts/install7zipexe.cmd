@echo off

echo "Installing 7-Zip Windows..."
set SEVEN_ZIP=C:\7-Zip\7z.exe

echo "7Zip root path: %SEVEN_ZIP%"
set ZIP_URL=https://www.7-zip.org/a/7z2101-x64.exe

IF NOT EXIST %SEVEN_ZIP% (
    echo "%SEVEN_ZIP% does not exist yet."
    powershell -NoProfile -ExecutionPolicy Unrestricted -File "%~dp0install-azcopy.ps1" -Url "%ZIP_URL%" -Destination "C:\7-Zip" -SkipUnzip || exit /B 1

    IF EXIST %SEVEN_ZIP% (
      echo "Installed 7-Zip Successfully."
    ) ELSE (
      echo "Failed to install 7-Zip."
    )
) ELSE (
    echo "%SEVEN_ZIP% exists, no installation is needed."
)
