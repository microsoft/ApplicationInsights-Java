
@echo Generating version properties...
@cd "%~dp0.."
call gradlew.bat :core:generateVersionProperties || exit /B 1

powershell.exe -NoProfile -ExecutionPolicy Unrestricted -File "%~dp0GenerateVersion.ps1"
exit /B %ERRORLEVEL%