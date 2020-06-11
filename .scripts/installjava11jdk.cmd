@echo off

echo "Installing Java 11..."
powershell -NoProfile -ExecutionPolicy Unrestricted -File "%~dp0install-java.ps1" -Url "https://repos.azul.com/azure-only/zulu/packages/zulu-11/11.0.7/zulu-11-azure-jdk_11.39.15-11.0.7-win_x64.zip" -Destination "C:\Program Files\Java" -EnvVar "JAVA_HOME" -UpdatePath -Checksum "1c043724066eee6fd90cd932670347cf7a6b9318f4a68b5ffbe71627b1fc4f53" -CleanOnFinish || exit /B 1

echo "C:\Program Files\Java\zulu-11-azure-jdk_11.39.15-11.0.7-win_x64"
dir "C:\Program Files\Java\zulu-11-azure-jdk_11.39.15-11.0.7-win_x64"

echo "JAVA_HOME=%JAVA_HOME%"

echo "Installed Java 11 Successfully."
