@echo off
setlocal

pushd %~dp0
set SCRIPT_ROOT=%CD%
popd

pushd %~dp0..
set PROJECT_ROOT=%CD%
call gradlew.bat :core:clean
if errorlevel 1 (
    echo Error running 'gradlew.bat clean' from '%PROJECT_ROOT%'
    exit /b 1
)
popd
endlocal
