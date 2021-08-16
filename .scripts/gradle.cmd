@echo off
setlocal

pushd %~dp0
set SCRIPT_ROOT=%CD%
popd

pushd %~dp0..
set PROJECT_ROOT=%CD%

set DEFAULT_OPTIONS=--info --stacktrace -DisBuildServer=true --warning-mode=all
set GRADLE_CMD=gradlew.bat %DEFAULT_OPTIONS% %*
echo Running '%GRADLE_CMD%' in '%PROJECT_ROOT%'
call %GRADLE_CMD%
if errorlevel 1 (
    echo Error running '%GRADLE_CMD%' in '%PROJECT_ROOT%'
    exit /b 1
)

def names = []
fileTree("%PROJECT_ROOT%\agent\agent-tooling\build\classes\java\test\section-records").visit {
  echo "List Session-Records content"
  FileVisitDetails details ->
    names << details.file.path
}

popd
endlocal