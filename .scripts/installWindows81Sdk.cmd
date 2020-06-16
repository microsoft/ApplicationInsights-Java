@echo off

echo "Installing Windows 8.1 SDK..."

powershell -NoProfile -ExecutionPolicy Unrestricted -File "%~dp0Install-Something.ps1" -Install -Url "http://download.microsoft.com/download/B/0/C/B0C80BA3-8AD6-4958-810B-6882485230B5/standalonesdk/sdksetup.exe" -InstallerArgs "/features OptionId.WindowsDesktopSoftwareDevelopmentKit OptionId.WindowsPerformanceToolkit OptionId.NetFxSoftwareDevelopmentKit /log %CDP_USER_SOURCE_FOLDER_CONTAINER_ACCESS_PATH%\AIJ-BuildLogs\Restore\WinSdk\winsdkinstall.log /norestart /q" -InstallationDirectory "C:\Program Files (x86)\Windows Kits\8.1" -HomeVar "APPINSIGHTS_WIN_SDK_PATH" -CleanOnFinish || exit /B 1

echo "C:\Program Files (x86)\Windows Kits\8.1"
dir "C:\Program Files (x86)\Windows Kits\8.1"

echo "%CDP_USER_SOURCE_FOLDER_CONTAINER_ACCESS_PATH%\AIJ-BuildLogs\WinSdk"
dir "%CDP_USER_SOURCE_FOLDER_CONTAINER_ACCESS_PATH%\AIJ-BuildLogs\WinSdk"

echo "Windows 8.1 SDK Installed Successfully"