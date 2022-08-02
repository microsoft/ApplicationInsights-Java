pushd %~dp0..

mkdir published\agent
mkdir published\runtime-attach
mkdir published\core
mkdir published\web

xcopy /E %USERPROFILE%\.m2\repository\com\microsoft\azure\applicationinsights-agent\* published\agent
xcopy /E %USERPROFILE%\.m2\repository\com\microsoft\azure\applicationinsights-runtime-attach\* published\runtime-attach
xcopy /E %USERPROFILE%\.m2\repository\com\microsoft\azure\applicationinsights-core\* published\core
xcopy /E %USERPROFILE%\.m2\repository\com\microsoft\azure\applicationinsights-web\* published\web

popd
