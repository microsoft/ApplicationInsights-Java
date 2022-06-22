pushd %~dp0..

mkdir published

xcopy /E %USERPROFILE%\.m2\repository\com\microsoft\azure\applicationinsights-agent\* published /EXCLUDE:.scripts\list-of-excluded-files.txt
xcopy /E %USERPROFILE%\.m2\repository\com\microsoft\azure\applicationinsights-runtime-attach\* published /EXCLUDE:.scripts\list-of-excluded-files.txt

popd
