# ApplicationInsights-Java

## Introduction

This is the repository of the Java SDK for [Visual Studio Application Insights](https://acom-prod-uswest-01.azurewebsites.net/documentation/articles/app-insights-overview/). Application Insights is a service that monitors the availability, performance and usage of your application. The SDK sends telemetry about the performance and usage of your app to the Application Insights service where your data can be visualized in the [Azure Portal](https://portal.azure.com). The SDK automatically collects telemetry about HTTP requests, dependencies, and exceptions. You can also use the SDK to send your own events and trace logs. 

Please refer to:

* [Get started with Application Insights in a Java web project](https://azure.microsoft.com/documentation/articles/app-insights-java-get-started/) 
* [Application Insights overview](https://azure.microsoft.com/services/application-insights/)

## Prerequisites

1.  Java SDK 1.6 or higher
2.  Sign-in to [Microsoft Azure](https://azure.com)

## Getting started

1.  Set JAVA_HOME environment variable to point to the JDK installation directory.
2.  To build run ./gradlew build on Linux systems or gradlew.bat build on Windows systems.

### Using Eclipse IDE

1.  Install gradle from http://www.gradle.org/installation
2.  Add GRADLE_HOME/bin to your PATH environment variable
3.  In build.gradle add line [apply plugin: "eclipse"]
4.  In Eclipse used File->Import Existing Project in a workspace.
5.  Use [gradle build] to build the project from the command line.

### CollectD Plugin - Optional

To build Application Insights CollectD writer plugin, please do the following:

1.  Download CollectD Java API sources and compile them using JDK 1.6.
    The output jar should be named: 'collectd-api.jar'.
    More info on compiling CollectD sources can be found here: https://collectd.org/dev-info.shtml
2.  Create a new directory for CollectD library you just created, and set a new environment variable 'COLLECTD_HOME'
    pointing to that folder.   
3.  Copy the new jar into %COLLECTD_HOME%/lib
4.  Reload Application Insights project. CollectD writer plugin sub-project should now be loaded.
    IDE restart may be required in order to identify the new environment variable.

### Notes

* To create a Java 6 compatible build you need to either have JAVA_HOME point to "Java 6 SDK" path or set JAVA_JRE_6 environment variable to point to [JRE 6 JRE installation directory]



## To upgrade to the latest SDK 

After you upgrade, you'll need to merge back any customizations you made to ApplicationInsights.xml. Take a copy of it to compare with the new file.

*If you're using Maven or Gradle*

1. If you specified a particular version number in pom.xml or build.gradle, update it.
2. Refresh your project's dependencies.

*Otherwise*

* Download the latest version of [Application Insights Java SDK](https://azuredownloads.blob.core.windows.net/applicationinsights/sdk.html) and replace the old ones. 
 
Compare the old and new ApplicationInsights.xml. Many of the changes you see are because we added and removed modules. Reinstate any customizations that you made.


##Release Notes

#### Version 1.0.5
- [Added proxy support](https://github.com/Microsoft/ApplicationInsights-Java/pull/290).
- [Treat HTTP response codes below 400 (and 401) as success](https://github.com/Microsoft/ApplicationInsights-Java/pull/295).
- [Fix agent load problem on Linux](https://github.com/Microsoft/ApplicationInsights-Java/pull/287).
- [Improve cleanup and exception handling in WebRequestTrackingFilter](https://github.com/Microsoft/ApplicationInsights-Java/pull/294).
- [Propagate log context properties in custom properties](https://github.com/Microsoft/ApplicationInsights-Java/pull/288).
- [Ability to filter out certain telemetry from being sent](https://github.com/Microsoft/ApplicationInsights-Java/pull/296).
- Some other miscellaneous fixes and improvements.
 
#### Version 1.0.4
- Interim version replaced by 1.0.5 on August 2016

#### Version 1.0.3
- Align to a new BOND schema used by the Application Insights data collection endpoints.

#### Version 1.0.2
- Prevent from overriding the instrumentation key using the one specified in the config when it's provided explicitly in code.
- Handle all successfull HTTP status codes and report the relevant HTTP Requests as successful.
- Handle all exceptions thrown by the ConfigurationFileLocator .

#### Version 1.0.1
- The [Java agent](app-insights-java-agent.md) collects dependency information about the following:
	- HTTP calls made via HttpClient, OkHttp and RestTemplate (Spring).
	- Calls to Redis made via the Jedis client. When a configurable threshold is passed, the SDK will also fetch the call arguments.
	- JDBC calls made with Oracle DB and Apache Derby DB clients.
	- Support the 'executeBatch' query type for prepared statements – The SDK will show the statement with the number of batches.
	- Provide the query plan for JDBC clients that has support for that (MySql, PostgreSql) – The query plan is fetched only when a configurable threshold is crossed

#### Version 1.0.0
- Adding support for the Application Insights writer plugin for CollectD.
- Adding support for the Application Insights Java agent.
- Fix for a compatibility issue with supporting HttpClient versions 4.2 and later.

#### Version 0.9.6
- Make the Java SDK compatible with servlet v2.5 and HttpClient pre-v4.3.
- Adding support for Java EE interceptors.
- Removing redundant dependencies from the Logback appender.

#### Version 0.9.5  
- Fix for an issue where custom events are not correlated with Users/Sessions due to cookie parsing errors.  
- Improved logic for resolving the location of the ApplicationInsights.xml configuration file.
- Anonymous User and Session cookies will not be generated on the server side. To implement user and session tracking for web apps, instrumentation with the JavaScript SDK is now required – cookies from the JavaScript SDK are still respected. Note that this change may cause a significant restatement of user and session counts as only user-originated sessions are being counted now.

#### Version 0.9.4
- Support collecting performance counters from 32-bit Windows machines.
- Support manual tracking of dependencies using a new ```trackDependency``` method API.
- Ability to tag a telemetry item as synthetic, by adding a ```SyntheticSource``` property to the reported item.


##Microsoft Open Source Code of Conduct



This project has adopted the [Microsoft Open Source Code of Conduct](https://opensource.microsoft.com/codeofconduct/). For more information see the [Code of Conduct FAQ](https://opensource.microsoft.com/codeofconduct/faq/) or contact [opencode@microsoft.com](mailto:opencode@microsoft.com) with any additional questions or comments.
