echo "copy ~/.m2/repository/com/microsoft/azure/applicationinsights-agent/ to build/output"

echo %~dp0
pushd %~dp0..
mkdir build/output
copy ~/.m2/repository/com/microsoft/azure/applicationinsights-agent/* build/output/

popd