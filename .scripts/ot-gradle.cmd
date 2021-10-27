@echo off
setlocal

pushd %~dp0
set SCRIPT_ROOT=%CD%
popd

pushd %~dp0..
set PROJECT_ROOT=%CD%\otel

pushd %PROJECT_ROOT%

set DEFAULT_OPTIONS=--info --stacktrace --warning-mode=all
rem one branch build has been getting sporadic metaspace out of memory errors 
powershell -Command "(gc gradle.properties) -replace '-XX:MaxMetaspaceSize=512m', '-XX:MaxMetaspaceSize=768m' | Out-File -encoding ASCII gradle.properties"
rem this is just for debugging
type gradle.properties
set GRADLE_CMD=gradlew.bat %DEFAULT_OPTIONS% %*
echo Running '%GRADLE_CMD%' in '%PROJECT_ROOT%'
call %GRADLE_CMD%
if errorlevel 1 (
    echo Error running '%GRADLE_CMD%' in '%PROJECT_ROOT%'
    exit /b 1
)
popd
popd
endlocal