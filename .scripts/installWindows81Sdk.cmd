@echo off

echo "Installing Windows 8.1 SDK..."

powershell -NoProfile -ExecutionPolicy Unrestricted -File "%~dp0Install-Something.ps1" -Install -Url "http://download.microsoft.com/download/B/0/C/B0C80BA3-8AD6-4958-810B-6882485230B5/standalonesdk/sdksetup.exe" -InstallerArgs "/q","/norestart","/features","+" -CleanOnFinish || exit /B 1

echo "C:\Program Files (x86)\Windows Kits\8.1"
dir "C:\Program Files (x86)\Windows Kits\8.1"

echo "Windows 8.1 SDK Installed Successfully"