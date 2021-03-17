@echo off

set JRE_VERSION=7
set JRE_NAME=zulu-7-azure-jre_7.44.0.11-7.0.292-win_x64.zip
set JRE_ZIP_URL=http://repos.azul.com/azure-only/zulu/packages/zulu-7/7u292/zulu-7-azure-jre_7.44.0.11-7.0.292-win_x64.zip

echo "Installing Java %JRE_VERSION% (%JRE_NAME%)..."
powershell -NoProfile -ExecutionPolicy Unrestricted -File "%~dp0install-java.ps1" -Url "%JRE_ZIP_URL%" -Destination "C:\Program Files\Java" -EnvVar "JAVA_JRE_7" -Checksum "7b359e21201b04090bb3ca6d3eb1112c26bb97d1d8b07145f4883de7573532bf" -CleanOnFinish || exit /B 1

echo "C:\Program Files\Java\%JDK_NAME%"
dir "C:\Program Files\Java\%JDK_NAME%"

echo "JAVA_JRE_7=%JAVA_JRE_7%"

echo "Installed Java %JDK_VERSION% JDK Successfully."
