@echo Generating version properties...
cd "%~dp0.."
call %~dp0..\gradlew.bat :core:generateVersionProperties --project-dir="%~dp0.." || exit /B 1

powershell.exe -NoProfile -ExecutionPolicy Unrestricted -File "%~dp0GenerateVersion.ps1"
exit /B %ERRORLEVEL%
