param (
    [Parameter(Mandatory=$true, HelpMessage="azuresdkpartnerdrops service principal key")]
    [ValidateNotNullOrEmpty()]
    [string]$ServicePrincipalKey,

    [Parameter(Mandatory=$true, HelpMessage="azuresdkpartnerdrops Service principal client ID")]
    [ValidateNotNullOrEmpty()]
    [string]$ServicePrincipleClientId,

    [Parameter(Mandatory=$true, HelpMessage="Jar location")]
    [ValidateNotNullOrEmpty()]
    [string]$JarPath,

    [Parameter(Mandatory=$true, HelpMessage="applicationinsights-java version")]
    [ValidateNotNullOrEmpty()]
    [string]$SDKVersionNumber
)

Write-Host "Jar path: $JarPath"
$Env:AZCOPY_SPA_CLIENT_SECRET=$ServicePrincipalKey
azcopy login --service-principal --application-id $ServicePrincipleClientId
azcopy copy "$JarPath/applicationinsights-agent-$SDKVersionNumber.jar" "https://azuresdkpartnerdrops.blob.core.windows.net/drops/applicationinsights-sdk/java/$SDKVersionNumber/"
azcopy copy "$JarPath/applicationinsights-agent-$SDKVersionNumber-devtest.jar" "https://azuresdkpartnerdrops.blob.core.windows.net/drops/applicationinsights-sdk/java/$SDKVersionNumber/"
azcopy copy "$JarPath/applicationinsights-agent-$SDKVersionNumber-javadoc.jar" "https://azuresdkpartnerdrops.blob.core.windows.net/drops/applicationinsights-sdk/java/$SDKVersionNumber/"
azcopy copy "$JarPath/applicationinsights-agent-$SDKVersionNumber-sources.jar" "https://azuresdkpartnerdrops.blob.core.windows.net/drops/applicationinsights-sdk/java/$SDKVersionNumber/"
azcopy copy "$JarPath/applicationinsights-agent-$SDKVersionNumber.pom" "https://azuresdkpartnerdrops.blob.core.windows.net/drops/applicationinsights-sdk/java/$SDKVersionNumber/"
Remove-Item Env:AZCOPY_SPA_CLIENT_SECRET
