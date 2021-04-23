@echo off

echo "Installing Windows 10 SDK..."

:: ---------------------------
:: Windows Software Development Kit - Windows 10.0.19041.1
:: ---------------------------
:: Available features: (Features marked with * can be downloaded but cannot be installed on this computer)
::    OptionId.WindowsPerformanceToolkit
::    OptionId.WindowsDesktopDebuggers
::    OptionId.AvrfExternal
::    OptionId.NetFxSoftwareDevelopmentKit
::    OptionId.WindowsSoftwareLogoToolkit
::    OptionId.IpOverUsb
::    OptionId.MSIInstallTools
::    OptionId.SigningTools
::    OptionId.UWPManaged
::    OptionId.UWPCPP
::    OptionId.UWPLocalized
::    OptionId.DesktopCPPx86
::    OptionId.DesktopCPPx64
::    OptionId.DesktopCPParm
::    OptionId.DesktopCPParm64

set "WINSDK_URL=https://download.microsoft.com/download/5/C/3/5C3770A3-12B4-4DB4-BAE7-99C624EB32AD/windowssdk/winsdksetup.exe"
set "SDK_FEATURES=OptionId.WindowsPerformanceToolkit OptionId.NetFxSoftwareDevelopmentKit OptionId.DesktopCPPx64 OptionId.DesktopCPPx86"

powershell -NoProfile -ExecutionPolicy Unrestricted -File "%~dp0install-something.ps1" -Install -Url "%WINSDK_URL%" -InstallerArgs "/features %SDK_FEATURES% /log %CDP_USER_SOURCE_FOLDER_CONTAINER_PATH%\WinSdkInstall\winsdk10install.log /norestart /q" -InstallationDirectory "C:\Program Files (x86)\Windows Kits\10" -HomeVar "APPINSIGHTS_WIN10_SDK_PATH" -CleanOnFinish || exit /B 1

echo "C:\Program Files (x86)\Windows Kits\10"
dir "C:\Program Files (x86)\Windows Kits\10"

echo "%CDP_USER_SOURCE_FOLDER_CONTAINER_PATH%\WinSdkInstall"
dir "%CDP_USER_SOURCE_FOLDER_CONTAINER_PATH%\WinSdkInstall"

echo "Windows 10 SDK Installed Successfully"