pushd %~dp0..

mkdir published

xcopy /E %USERPROFILE%\.m2\repository\com\microsoft\azure\applicationinsights-agent\* published
xcopy /E %USERPROFILE%\.m2\repository\com\microsoft\azure\applicationinsights-runtime-attach\* published

popd
