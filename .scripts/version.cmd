@echo off
setlocal

pushd %~dp0
set SCRIPT_ROOT=%CD%
popd

pushd %~dp0..
set PROJECT_ROOT=%CD%

:: Update this to the tasks you want to run
set GRADLE_TASKS=:core:generateVersionProperties
:: Add any additonal options
set GRADLE_OPTIONS=--info
set GRADLE_CMD=gradlew.bat %GRADLE_TASKS% %GRADLE_OPTIONS%
echo Running '%GRADLE_CMD%' in '%PROJECT_ROOT%'
call %GRADLE_CMD%
if errorlevel 1 (
    echo Error running '%GRADLE_CMD%' in '%PROJECT_ROOT%'
    exit /b 1
)
echo Running using version file to udpate build number...
powershell.exe -NoProfile -ExecutionPolicy Unrestricted -File "%~dp0GenerateVersion.ps1"
exit /b %ERRORLEVEL%

popd
endlocal
