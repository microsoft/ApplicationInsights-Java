param (
    [Parameter(Mandatory=$true, HelpMessage="azuresdkpartnerdrops service principal key")]
    [ValidateNotNullOrEmpty()]
    [string]$ServicePrincipalKey,

    [Parameter(Mandatory=$true, HelpMessage="azuresdkpartnerdrops Service principal client ID")]
    [ValidateNotNullOrEmpty()]
    [string]$ServicePrincipleClientId,

    [Parameter(Mandatory=$true, HelpMessage="Agent Jar Path")]
    [ValidateNotNullOrEmpty()]
    [string]$AgentJarPath,

#    [Parameter(Mandatory=$true, HelpMessage="Collectd Jar Path")]
#    [ValidateNotNullOrEmpty()]
#    [string]$CollectdJarPath,

    [Parameter(Mandatory=$true, HelpMessage="Core Jar Path")]
    [ValidateNotNullOrEmpty()]
    [string]$CoreJarPath,

    [Parameter(Mandatory=$true, HelpMessage="Logging-log4j1_2 Jar Path")]
    [ValidateNotNullOrEmpty()]
    [string]$Log4j1_2JarPath,

    [Parameter(Mandatory=$true, HelpMessage="Logging-log4j2 Jar Path")]
    [ValidateNotNullOrEmpty()]
    [string]$Log4j2JarPath,

    [Parameter(Mandatory=$true, HelpMessage="Logging-logback Jar Path")]
    [ValidateNotNullOrEmpty()]
    [string]$LogbackJarPath,

    [Parameter(Mandatory=$true, HelpMessage="Spring Boot Starter Jar Path")]
    [ValidateNotNullOrEmpty()]
    [string]$SpringBootStarterJarPath,

    [Parameter(Mandatory=$true, HelpMessage="Web Jar Path")]
    [ValidateNotNullOrEmpty()]
    [string]$WebJarPath,

    [Parameter(Mandatory=$true, HelpMessage="Web Auto Jar Path")]
    [ValidateNotNullOrEmpty()]
    [string]$WebAutoJarPath,

    [Parameter(Mandatory=$true, HelpMessage="applicationinsights-java version")]
    [ValidateNotNullOrEmpty()]
    [string]$SDKVersionNumber
)

$Env:AZCOPY_SPA_CLIENT_SECRET=$ServicePrincipalKey
azcopy login --service-principal --application-id $ServicePrincipleClientId
azcopy copy "$AgentJarPath/applicationinsights-agent-$SDKVersionNumber.jar" "https://azuresdkpartnerdrops.blob.core.windows.net/drops/applicationinsights-sdk/java/$SDKVersionNumber/"
azcopy copy "$AgentJarPath/applicationinsights-agent-$SDKVersionNumber-javadoc.jar" "https://azuresdkpartnerdrops.blob.core.windows.net/drops/applicationinsights-sdk/java/$SDKVersionNumber/"
azcopy copy "$AgentJarPath/applicationinsights-agent-$SDKVersionNumber-sources.jar" "https://azuresdkpartnerdrops.blob.core.windows.net/drops/applicationinsights-sdk/java/$SDKVersionNumber/"
azcopy copy "$AgentJarPath/applicationinsights-agent-$SDKVersionNumber.pom" "https://azuresdkpartnerdrops.blob.core.windows.net/drops/applicationinsights-sdk/java/$SDKVersionNumber/"
#azcopy copy "$CollectdJarPath/applicationinsights-collectd-$SDKVersionNumber.jar" "https://azuresdkpartnerdrops.blob.core.windows.net/drops/applicationinsights-sdk/java/$SDKVersionNumber/"
#azcopy copy "$CollectdJarPath/applicationinsights-collectd-$SDKVersionNumber-javadoc.jar" "https://azuresdkpartnerdrops.blob.core.windows.net/drops/applicationinsights-sdk/java/$SDKVersionNumber/"
#azcopy copy "$CollectdJarPath/applicationinsights-collectd-$SDKVersionNumber-sources.jar" "https://azuresdkpartnerdrops.blob.core.windows.net/drops/applicationinsights-sdk/java/$SDKVersionNumber/"
#azcopy copy "$CollectdJarPath/applicationinsights-collectd-$SDKVersionNumber.pom" "https://azuresdkpartnerdrops.blob.core.windows.net/drops/applicationinsights-sdk/java/$SDKVersionNumber/"
azcopy copy "$CoreJarPath/applicationinsights-core-$SDKVersionNumber.jar" "https://azuresdkpartnerdrops.blob.core.windows.net/drops/applicationinsights-sdk/java/$SDKVersionNumber/"
azcopy copy "$CoreJarPath/applicationinsights-core-$SDKVersionNumber-javadoc.jar" "https://azuresdkpartnerdrops.blob.core.windows.net/drops/applicationinsights-sdk/java/$SDKVersionNumber/"
azcopy copy "$CoreJarPath/applicationinsights-core-$SDKVersionNumber-sources.jar" "https://azuresdkpartnerdrops.blob.core.windows.net/drops/applicationinsights-sdk/java/$SDKVersionNumber/"
azcopy copy "$CoreJarPath/applicationinsights-core-$SDKVersionNumber.pom" "https://azuresdkpartnerdrops.blob.core.windows.net/drops/applicationinsights-sdk/java/$SDKVersionNumber/"
azcopy copy "$Log4j1_2JarPath/applicationinsights-logging-log4j1_2-$SDKVersionNumber.jar" "https://azuresdkpartnerdrops.blob.core.windows.net/drops/applicationinsights-sdk/java/$SDKVersionNumber/"
azcopy copy "$Log4j1_2JarPath/applicationinsights-logging-log4j1_2-$SDKVersionNumber-javadoc.jar" "https://azuresdkpartnerdrops.blob.core.windows.net/drops/applicationinsights-sdk/java/$SDKVersionNumber/"
azcopy copy "$Log4j1_2JarPath/applicationinsights-logging-log4j1_2-$SDKVersionNumber-sources.jar" "https://azuresdkpartnerdrops.blob.core.windows.net/drops/applicationinsights-sdk/java/$SDKVersionNumber/"
azcopy copy "$Log4j1_2JarPath/applicationinsights-logging-log4j1_2-$SDKVersionNumber.pom" "https://azuresdkpartnerdrops.blob.core.windows.net/drops/applicationinsights-sdk/java/$SDKVersionNumber/"
azcopy copy "$Log4j2JarPath/applicationinsights-logging-log4j2-$SDKVersionNumber.jar" "https://azuresdkpartnerdrops.blob.core.windows.net/drops/applicationinsights-sdk/java/$SDKVersionNumber/"
azcopy copy "$Log4j2JarPath/applicationinsights-logging-log4j2-$SDKVersionNumber-javadoc.jar" "https://azuresdkpartnerdrops.blob.core.windows.net/drops/applicationinsights-sdk/java/$SDKVersionNumber/"
azcopy copy "$Log4j2JarPath/applicationinsights-logging-log4j2-$SDKVersionNumber-sources.jar" "https://azuresdkpartnerdrops.blob.core.windows.net/drops/applicationinsights-sdk/java/$SDKVersionNumber/"
azcopy copy "$Log4j2JarPath/applicationinsights-logging-log4j2-$SDKVersionNumber.pom" "https://azuresdkpartnerdrops.blob.core.windows.net/drops/applicationinsights-sdk/java/$SDKVersionNumber/"
azcopy copy "$LogbackJarPath/applicationinsights-logging-logback-$SDKVersionNumber.jar" "https://azuresdkpartnerdrops.blob.core.windows.net/drops/applicationinsights-sdk/java/$SDKVersionNumber/"
azcopy copy "$LogbackJarPath/applicationinsights-logging-logback-$SDKVersionNumber-javadoc.jar" "https://azuresdkpartnerdrops.blob.core.windows.net/drops/applicationinsights-sdk/java/$SDKVersionNumber/"
azcopy copy "$LogbackJarPath/applicationinsights-logging-logback-$SDKVersionNumber-sources.jar" "https://azuresdkpartnerdrops.blob.core.windows.net/drops/applicationinsights-sdk/java/$SDKVersionNumber/"
azcopy copy "$LogbackJarPath/applicationinsights-logging-logback-$SDKVersionNumber.pom" "https://azuresdkpartnerdrops.blob.core.windows.net/drops/applicationinsights-sdk/java/$SDKVersionNumber/"
azcopy copy "$SpringBootStarterJarPath/applicationinsights-spring-boot-starter-$SDKVersionNumber.jar" "https://azuresdkpartnerdrops.blob.core.windows.net/drops/applicationinsights-sdk/java/$SDKVersionNumber/"
azcopy copy "$SpringBootStarterJarPath/applicationinsights-spring-boot-starter-$SDKVersionNumber-javadoc.jar" "https://azuresdkpartnerdrops.blob.core.windows.net/drops/applicationinsights-sdk/java/$SDKVersionNumber/"
azcopy copy "$SpringBootStarterJarPath/applicationinsights-spring-boot-starter-$SDKVersionNumber-sources.jar" "https://azuresdkpartnerdrops.blob.core.windows.net/drops/applicationinsights-sdk/java/$SDKVersionNumber/"
azcopy copy "$SpringBootStarterJarPath/applicationinsights-spring-boot-starter-$SDKVersionNumber.pom" "https://azuresdkpartnerdrops.blob.core.windows.net/drops/applicationinsights-sdk/java/$SDKVersionNumber/"
azcopy copy "$WebJarPath/applicationinsights-web-$SDKVersionNumber.jar" "https://azuresdkpartnerdrops.blob.core.windows.net/drops/applicationinsights-sdk/java/$SDKVersionNumber/"
azcopy copy "$WebJarPath/applicationinsights-web-$SDKVersionNumber-javadoc.jar" "https://azuresdkpartnerdrops.blob.core.windows.net/drops/applicationinsights-sdk/java/$SDKVersionNumber/"
azcopy copy "$WebJarPath/applicationinsights-web-$SDKVersionNumber-sources.jar" "https://azuresdkpartnerdrops.blob.core.windows.net/drops/applicationinsights-sdk/java/$SDKVersionNumber/"
azcopy copy "$WebJarPath/applicationinsights-web-$SDKVersionNumber.pom" "https://azuresdkpartnerdrops.blob.core.windows.net/drops/applicationinsights-sdk/java/$SDKVersionNumber/"
azcopy copy "$WebAutoJarPath/applicationinsights-web-auto-$SDKVersionNumber.jar" "https://azuresdkpartnerdrops.blob.core.windows.net/drops/applicationinsights-sdk/java/$SDKVersionNumber/"
azcopy copy "$WebAutoJarPath/applicationinsights-web-auto-$SDKVersionNumber-javadoc.jar" "https://azuresdkpartnerdrops.blob.core.windows.net/drops/applicationinsights-sdk/java/$SDKVersionNumber/"
azcopy copy "$WebAutoJarPath/applicationinsights-web-auto-$SDKVersionNumber-sources.jar" "https://azuresdkpartnerdrops.blob.core.windows.net/drops/applicationinsights-sdk/java/$SDKVersionNumber/"
azcopy copy "$WebAutoJarPath/applicationinsights-web-auto-$SDKVersionNumber.pom" "https://azuresdkpartnerdrops.blob.core.windows.net/drops/applicationinsights-sdk/java/$SDKVersionNumber/"
Remove-Item Env:AZCOPY_SPA_CLIENT_SECRET