@echo off

echo "Installing 7-Zip Windows..."
set SEVEN_ZIP=D:\7-Zip\7z.exe

echo "7Zip root path: %SEVEN_ZIP%"
set ZIP_URL_64=https://www.7-zip.org/a/7z2101-x64.exe
set ZIP_URL_32=https://www.7-zip.org/a/7z2101.exe

reg Query "HKLM\Hardware\Description\System\CentralProcessor\0" | find /i "x86" > NUL && set OS=32BIT || set OS=64BIT

IF NOT EXIST %SEVEN_ZIP% (
    echo "%SEVEN_ZIP% does not exist yet."
    if %OS%==64BIT (
        powershell -NoProfile -ExecutionPolicy Unrestricted -File "%~dp0install-azcopy.ps1" -Url "%ZIP_URL_64%" -Destination "D:\7-Zip" -SkipUnzip || exit /B 1
    ) else (
        powershell -NoProfile -ExecutionPolicy Unrestricted -File "%~dp0install-azcopy.ps1" -Url "%ZIP_URL_32%" -Destination "D:\7-Zip" -SkipUnzip || exit /B 1
    )

    IF EXIST %SEVEN_ZIP% (
      echo "Installed 7-Zip Successfully."
    ) ELSE (
      echo "Failed to install 7-Zip."
    )
) ELSE (
    echo "%SEVEN_ZIP% exists, no installation is needed."
)
