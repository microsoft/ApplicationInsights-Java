

@echo off

echo "Installing Java 7 JRE..."
powershell -NoProfile -ExecutionPolicy Unrestricted -File "%~dp0install-java.ps1" -Url "https://repos.azul.com/azure-only/zulu/packages/zulu-7/7u262/zulu-7-azure-jre_7.38.0.11-7.0.262-win_x64.zip" -Destination "C:\Program Files\Java" -EnvVar "JAVA_JRE_7" -Checksum "833600bbc34dba79dcfed88ff6878d6c37a3e32525b75e829b7057c091cc703e" -CleanOnFinish || exit /B 1

echo "C:\Program Files\Java\zulu-7-azure-jre_7.38.0.11-7.0.262-win_x64"
dir "C:\Program Files\Java\zulu-7-azure-jre_7.38.0.11-7.0.262-win_x64"

echo "JAVA_JRE_7=%JAVA_JRE_7%"

echo "Installed Java 7 JRE Successfully."
