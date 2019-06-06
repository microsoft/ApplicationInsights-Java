REM gradlew --info --stacktrace build

where docker.exe > %TEMP%\dockerpath.tmp
set /p DOCKER_EXE=<%TEMP%\dockerpath.tmp
set /p DOCKER_CLI_EXE=<%TEMP%\dockerpath.tmp

REM Stop all currently running docker instances
FOR /f "tokens=*" %%i IN ('docker ps -q') DO docker stop %%i

docker network rm aismoke-net
docker network prune --force

gradlew --info --stacktrace smokeTest >log 2>err
REM test:smoke:appServers
REM gradlew smokeTest
