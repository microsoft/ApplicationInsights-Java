# SDKLogger
This library is utilized by the SDK for troubleshooting SDK issues.
It is off by default and should only be turned on if experiencing issues with the SDK.

## Note: internal use only
This library is intended for use only within the Application Insights Java SDK itself.

To direct application logs to Application Insights, follow 
[this documentation](https://docs.microsoft.com/en-us/azure/azure-monitor/app/java-trace-logs) for integration into an existing logging framework.
Alternatively, [use the TelemetryClient to send TraceTelemetry](https://docs.microsoft.com/en-us/azure/azure-monitor/app/api-custom-events-metrics#tracktrace).

## Troubleshooting the SDK with the SDKLogger
The SDKLogger can be enabled in two ways:
* via configuration file
    * [ApplciationInsights.xml](https://github.com/microsoft/ApplicationInsights-Java/wiki/ApplicationInsights.XML#sdklogger)
    * [applciation.properties (using azure-application-insights-spring-boot-starter)](https://github.com/microsoft/ApplicationInsights-Java/tree/master/azure-application-insights-spring-boot-starter#configure-more-parameters-using-applicationproperties)
* [via system properties](https://docs.microsoft.com/en-us/azure/azure-monitor/app/java-troubleshoot#java-command-line-properties)

For more troubleshooting tips, see https://docs.microsoft.com/en-us/azure/azure-monitor/app/java-troubleshoot
