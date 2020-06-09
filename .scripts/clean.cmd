@echo off
setlocal

pushd %~dp0
set SCRIPT_ROOT=%CD%
popd

pushd %~dp0..
set PROJECT_ROOT=%CD%
echo dir of '%PROJECT_ROOT%'
dir
set GRADLE_CMD=gradlew.bat :core:clean --info
echo Running '%GRADLE_CMD%' from '%PROJECT_ROOT%'
call %GRADLE_CMD%
if errorlevel 1 (
    echo Error running '%GRADLE_CMD%' from '%PROJECT_ROOT%'
    exit /b 1
)
popd
endlocal
