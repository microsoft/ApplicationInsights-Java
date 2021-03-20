param (
    [Parameter(Mandatory=$true, HelpMessage="azuresdkpartnerdrops service principal key")]
    [ValidateNotNullOrEmpty()]
    [string]$ServicePrincipalKey,

    [Parameter(Mandatory=$true, HelpMessage="azuresdkpartnerdrops Service principal client ID")]
    [ValidateNotNullOrEmpty()]
    [string]$ServicePrincipleClientId,

    [Parameter(Mandatory=$true, HelpMessage="applicationinsights-java version")]
    [ValidateNotNullOrEmpty()]
    [string]$SDKVersionNumber
)

Write-Host $ServicePrincipalKey
Write-Host $ServicePrincipleClientId
Write-Host $SDKVersionNumber

$Env:AZCOPY_SPA_CLIENT_SECRET=$ServicePrincipalKey
azcopy login --service-principal --application-id $ServicePrincipleClientId
azcopy copy "$(System.DefaultWorkingDirectory)/_applicationinsights-java-Windows-Official-master/drop/outputs/build/Artifacts/agent/agent/build/libs/applicationinsights-agent-$SDKVersionNumber.jar" "https://azuresdkpartnerdrops.blob.core.windows.net/drops/applicationinsights-sdk/java/$SDKVersionNumber/"
Remove-Item Env:AZCOPY_SPA_CLIENT_SECRET