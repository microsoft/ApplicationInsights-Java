@echo off

echo "Installing AzCopy V10 Windows..."

set ZIP_URL_64=https://azcopyvnext.azureedge.net/release20210226/azcopy_windows_amd64_10.9.0.zip
set ZIP_URL_32=https://azcopyvnext.azureedge.net/release20210226/azcopy_windows_386_10.9.0.zip
reg Query "HKLM\Hardware\Description\System\CentralProcessor\0" | find /i "x86" > NUL && set OS=32BIT || set OS=64BIT

if %OS%==64BIT (
    powershell -NoProfile -ExecutionPolicy Unrestricted -File "%~dp0install-azcopy.ps1" -Url "%ZIP_URL_64%" -Destination "C:\Program Files\AzCopy" -UpdatePath -PathTo7Zip "C:\7-Zip\7z.exe" -CleanOnFinish || exit /B 1
) else (
    powershell -NoProfile -ExecutionPolicy Unrestricted -File "%~dp0install-azcopy.ps1" -Url "%ZIP_URL_32%" -Destination "C:\Program Files\AzCopy" -UpdatePath -PathTo7Zip "C:\7-Zip\7z.exe" -CleanOnFinish || exit /B 1
)

echo "C:\Program Files\AzCopy"
dir "C:\Program Files\AzCopy"

echo "C:\Program Files\AzCopy\azcopy_windows_amd64_10.9.0"
dir "C:\Program Files\AzCopy\azcopy_windows_amd64_10.9.0"

echo "Installed AzCopy v10.9.0 Successfully."