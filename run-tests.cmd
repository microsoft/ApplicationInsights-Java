@echo off
REM
REM Unit tests
REM 
gradlew --info --stacktrace build test

REM
REM Smoke tests
REM
where docker.exe > %TEMP%\dockerpath.tmp
set /p DOCKER_EXE=<%TEMP%\dockerpath.tmp
set /p DOCKER_CLI_EXE=<%TEMP%\dockerpath.tmp
for /f "tokens=*" %%i IN ('docker ps -q') DO docker stop %%i
docker network rm aismoke-net
docker network prune --force

gradlew --info --stacktrace smokeTest
