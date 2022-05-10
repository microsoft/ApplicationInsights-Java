echo "debug start"

echo %USERPROFILE%

dir %USERPROFILE%

mkdir %USERPROFILE%\.m2\repository\com\microsoft\azure\applicationinsights-agent\1.2.3

echo test1 > %USERPROFILE%\.m2\repository\com\microsoft\azure\applicationinsights-agent\1.2.3\test1.pom
echo test2 > %USERPROFILE%\.m2\repository\com\microsoft\azure\applicationinsights-agent\1.2.3\test2.pom
echo test3 > %USERPROFILE%\.m2\repository\com\microsoft\azure\applicationinsights-agent\1.2.3\test3.pom

pushd %~dp0..

mkdir repository\com\microsoft\azure\applicationinsights-agent

xcopy /E %USERPROFILE%\.m2\repository\com\microsoft\azure\applicationinsights-agent repository\com\microsoft\azure\applicationinsights-agent

dir %USERPROFILE%\.m2\repository\com\microsoft\azure\applicationinsights-agent\1.2.3

dir repository\com\microsoft\azure\applicationinsights-agent\1.2.3

popd

echo "debug end"
