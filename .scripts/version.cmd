
echo Generating version properties...
call gradlew.bat :core:generateVersionProperties || exit /B 1

powershell.exe -NoProfile -ExecutionPolicy Unrestricted -File "%~dp0GenerateVersion.ps1"
exit /B %ERRORLEVEL%