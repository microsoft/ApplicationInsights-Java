@echo off

set JDK_VERSION=8
set JDK_NAME=zulu-8-azure-jdk_8.52.0.23-8.0.282-win_x64
set JDK_ZIP_URL=https://repos.azul.com/azure-only/zulu/packages/zulu-8/8u282/zulu-8-azure-jdk_8.52.0.23-8.0.282-win_x64.zip

echo "Installing Java %JDK_VERSION% ()%JDK_NAME%)..."
powershell -NoProfile -ExecutionPolicy Unrestricted -File "%~dp0install-java.ps1" -Url "%JDK_ZIP_URL%" -Destination "C:\Program Files\Java" -EnvVar "JAVA_HOME" -Checksum "604124be1b4f6a627523c0f06e2b29c0e0dc4d202ca06a09ac0cb972d124b936" -CleanOnFinish || exit /B 1

echo "C:\Program Files\Java\%JDK_NAME%"
dir "C:\Program Files\Java\%JDK_NAME%"

echo "JAVA_HOME=%JAVA_HOME%"

echo "Installed Java %JDK_VERSION% JDK Successfully."
