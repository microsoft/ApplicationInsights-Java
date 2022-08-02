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
azcopy copy "$JarPath/agent/$SDKVersionNumber/applicationinsights-agent-$SDKVersionNumber.jar" "https://azuresdkpartnerdrops.blob.core.windows.net/drops/applicationinsights-sdk/java/$SDKVersionNumber/"
azcopy copy "$JarPath/agent/$SDKVersionNumber/applicationinsights-agent-$SDKVersionNumber-javadoc.jar" "https://azuresdkpartnerdrops.blob.core.windows.net/drops/applicationinsights-sdk/java/$SDKVersionNumber/"
azcopy copy "$JarPath/agent/$SDKVersionNumber/applicationinsights-agent-$SDKVersionNumber-sources.jar" "https://azuresdkpartnerdrops.blob.core.windows.net/drops/applicationinsights-sdk/java/$SDKVersionNumber/"
azcopy copy "$JarPath/agent/$SDKVersionNumber/applicationinsights-agent-$SDKVersionNumber.pom" "https://azuresdkpartnerdrops.blob.core.windows.net/drops/applicationinsights-sdk/java/$SDKVersionNumber/"
azcopy copy "$JarPath/runtime-attach/$SDKVersionNumber/applicationinsights-runtime-attach-$SDKVersionNumber.jar" "https://azuresdkpartnerdrops.blob.core.windows.net/drops/applicationinsights-sdk/java/$SDKVersionNumber/"
azcopy copy "$JarPath/runtime-attach/$SDKVersionNumber/applicationinsights-runtime-attach-$SDKVersionNumber-javadoc.jar" "https://azuresdkpartnerdrops.blob.core.windows.net/drops/applicationinsights-sdk/java/$SDKVersionNumber/"
azcopy copy "$JarPath/runtime-attach/$SDKVersionNumber/applicationinsights-runtime-attach-$SDKVersionNumber-sources.jar" "https://azuresdkpartnerdrops.blob.core.windows.net/drops/applicationinsights-sdk/java/$SDKVersionNumber/"
azcopy copy "$JarPath/runtime-attach/$SDKVersionNumber/applicationinsights-runtime-attach-$SDKVersionNumber.pom" "https://azuresdkpartnerdrops.blob.core.windows.net/drops/applicationinsights-sdk/java/$SDKVersionNumber/"
azcopy copy "$JarPath/core/$SDKVersionNumber/applicationinsights-core-$SDKVersionNumber.jar" "https://azuresdkpartnerdrops.blob.core.windows.net/drops/applicationinsights-sdk/java/$SDKVersionNumber/"
azcopy copy "$JarPath/core/$SDKVersionNumber/applicationinsights-core-$SDKVersionNumber-javadoc.jar" "https://azuresdkpartnerdrops.blob.core.windows.net/drops/applicationinsights-sdk/java/$SDKVersionNumber/"
azcopy copy "$JarPath/core/$SDKVersionNumber/applicationinsights-core-$SDKVersionNumber-sources.jar" "https://azuresdkpartnerdrops.blob.core.windows.net/drops/applicationinsights-sdk/java/$SDKVersionNumber/"
azcopy copy "$JarPath/core/$SDKVersionNumber/applicationinsights-core-$SDKVersionNumber.pom" "https://azuresdkpartnerdrops.blob.core.windows.net/drops/applicationinsights-sdk/java/$SDKVersionNumber/"
azcopy copy "$JarPath/web/$SDKVersionNumber/applicationinsights-web-$SDKVersionNumber.jar" "https://azuresdkpartnerdrops.blob.core.windows.net/drops/applicationinsights-sdk/java/$SDKVersionNumber/"
azcopy copy "$JarPath/web/$SDKVersionNumber/applicationinsights-web-$SDKVersionNumber-javadoc.jar" "https://azuresdkpartnerdrops.blob.core.windows.net/drops/applicationinsights-sdk/java/$SDKVersionNumber/"
azcopy copy "$JarPath/web/$SDKVersionNumber/applicationinsights-web-$SDKVersionNumber-sources.jar" "https://azuresdkpartnerdrops.blob.core.windows.net/drops/applicationinsights-sdk/java/$SDKVersionNumber/"
azcopy copy "$JarPath/web/$SDKVersionNumber/applicationinsights-web-$SDKVersionNumber.pom" "https://azuresdkpartnerdrops.blob.core.windows.net/drops/applicationinsights-sdk/java/$SDKVersionNumber/"
Remove-Item Env:AZCOPY_SPA_CLIENT_SECRET
