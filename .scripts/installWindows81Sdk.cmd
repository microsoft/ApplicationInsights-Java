@echo off

echo "Installing Windows 8.1 SDK..."

:: ---------------------------
:: Windows Software Development Kit for Windows 8.1
:: ---------------------------
:: Available features: (Features marked with * can be downloaded but cannot be installed on this computer)
::    OptionId.WindowsDesktopSoftwareDevelopmentKit
::    OptionId.WindowsPerformanceToolkit
::    OptionId.WindowsDesktopDebuggers
::    OptionId.AvrfExternal
::    OptionId.NetFxSoftwareDevelopmentKit
::    OptionId.WindowsSoftwareLogoToolkit
::    OptionId.MSIInstallTools
::  * OptionId.Netfx

powershell -NoProfile -ExecutionPolicy Unrestricted -File "%~dp0Install-Something.ps1" -Install -Url "http://download.microsoft.com/download/B/0/C/B0C80BA3-8AD6-4958-810B-6882485230B5/standalonesdk/sdksetup.exe" -InstallerArgs "/features OptionId.WindowsDesktopSoftwareDevelopmentKit OptionId.WindowsPerformanceToolkit OptionId.NetFxSoftwareDevelopmentKit /log %CDP_USER_SOURCE_FOLDER_CONTAINER_PATH%\WinSdkInstall\winsdkinstall.log /norestart /q" -InstallationDirectory "C:\Program Files (x86)\Windows Kits\8.1" -HomeVar "APPINSIGHTS_WIN_SDK_PATH" -CleanOnFinish || exit /B 1

echo "C:\Program Files (x86)\Windows Kits\8.1"
dir "C:\Program Files (x86)\Windows Kits\8.1"

echo "%CDP_USER_SOURCE_FOLDER_CONTAINER_PATH%\WinSdkInstall"
dir "%CDP_USER_SOURCE_FOLDER_CONTAINER_PATH%\WinSdkInstall"

echo "Windows 8.1 SDK Installed Successfully"