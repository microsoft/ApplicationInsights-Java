pushd %~dp0..

mkdir published\agent
mkdir published\runtime-attach

xcopy /E %USERPROFILE%\.m2\repository\com\microsoft\azure\applicationinsights-agent\* published\agent
xcopy /E %USERPROFILE%\.m2\repository\com\microsoft\azure\applicationinsights-runtime-attach\* published\runtime-attach

popd
