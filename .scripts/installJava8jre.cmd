

@echo off

echo "Installing Java 8 JRE..."
powershell -NoProfile -ExecutionPolicy Unrestricted -File "%~dp0install-java.ps1" -Url "https://repos.azul.com/azure-only/zulu/packages/zulu-8/8u282/zulu-8-azure-jdk_8.52.0.23-8.0.282-win_x64.zip " -Destination "C:\Program Files\Java" -EnvVar "JAVA_JRE_8" -Checksum "833600bbc34dba79dcfed88ff6878d6c37a3e32525b75e829b7057c091cc703e" -CleanOnFinish || exit /B 1

echo "C:\Program Files\Java\zulu-8-azure-jdk_8.52.0.23-8.0.282-win_x64"
dir "C:\Program Files\Java\zulu-8-azure-jdk_8.52.0.23-8.0.282-win_x64.zip"

echo "JAVA_JRE_8=%JAVA_JRE_8%"

echo "Installed Java 8 JRE Successfully."
