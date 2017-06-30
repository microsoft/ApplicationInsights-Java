#CHANGELOG

## Version 1.0.9
- Fix the issue of infinite retry and connection drain on certificate error by updating the version of http client packaged with the SDK.

## Version 1.0.8
- #367 Updated the agent project not to be transitive when resolved for core
- #374 Updated the core JAR to remove the transitive dependencies for guava, removed the transitive dependency from core and agent, & updated to the latest version of the httpclient
- #376 Delaying retry on send when no connection is found
- #377 Bug fixes for Live Metrics Stream integration
- #378 Added Live Metrics Stream URL filter and fixed duration
- #379 Updated Gradle to 3.5
- #380 Delaying retry on send when ConnectionException is thrown
- #381 Delaying retry on send when HostUnknownException is thrown

## Version 1.0.7
- Use [Shadow plugin](http://imperceptiblethoughts.com/shadow/) to create self contained JAR files in the SDK
- Allow marking a metric as custome performance counter. See [here](https://github.com/Microsoft/ApplicationInsights-Java/wiki/Mark-Metric-as-Custom-Performace-Counter)
- Better support for Visual Studio Code for edting the Application Insights Java SDK Code
- Added sampling support. See [here](https://github.com/Microsoft/ApplicationInsights-Java/wiki/Sampling)
- Allow changing the performance counter collection frequency and added hooks before and after sending the PC see [here](https://github.com/Microsoft/ApplicationInsights-Java/wiki/Perfomance-Counters-Collection:-Setting-collection-frequency) and [here](https://github.com/Microsoft/ApplicationInsights-Java/wiki/Perfomance-Counters-Collection:-Plugin)
- Agent built-in types are now off by default. Support wildcard to monitor multiple classes in the agent.
- Add depenecy type in the agent configuration see [here](https://github.com/Microsoft/ApplicationInsights-Java/wiki/Configure-Dependecy-Type-in-the-Agent-Configuration)


## Version 1.0.6
- [Allow reporting runtime exceptions](https://github.com/Microsoft/ApplicationInsights-Java/pull/302)
- [Collect JVM heap memory usage and automatic deadlock detection](https://github.com/Microsoft/ApplicationInsights-Java/pull/313)
- [Make frequency of performance counter collection configurable](https://github.com/Microsoft/ApplicationInsights-Java/pull/316)
- Plugin to allow intervening before and after performance counters collection [change](https://github.com/Microsoft/ApplicationInsights-Java/pull/328) [docs](https://github.com/Microsoft/ApplicationInsights-Java/wiki/Perfomance-Counters-Collection:-Plugin)
- [Comply to server side throttling](https://github.com/Microsoft/ApplicationInsights-Java/pull/321)
- [Upgrade to Gradle 3.0 build](https://github.com/Microsoft/ApplicationInsights-Java/pull/325)
- [Allow running Java agent outside of webapp context](https://github.com/Microsoft/ApplicationInsights-Java/pull/326)
- [Built in instrumentation for MuleESB client](https://github.com/Microsoft/ApplicationInsights-Java/pull/330)
- [Built in instrumentation for current Apache Derby DB](https://github.com/Microsoft/ApplicationInsights-Java/pull/333)

## Version 1.0.5
- [Added proxy support](https://github.com/Microsoft/ApplicationInsights-Java/pull/290).
- [Treat HTTP response codes below 400 (and 401) as success](https://github.com/Microsoft/ApplicationInsights-Java/pull/295).
- [Fix agent load problem on Linux](https://github.com/Microsoft/ApplicationInsights-Java/pull/287).
- [Improve cleanup and exception handling in WebRequestTrackingFilter](https://github.com/Microsoft/ApplicationInsights-Java/pull/294).
- [Propagate log context properties in custom properties](https://github.com/Microsoft/ApplicationInsights-Java/pull/288).
- [Ability to filter out certain telemetry from being sent](https://github.com/Microsoft/ApplicationInsights-Java/pull/296).
- Some other miscellaneous fixes and improvements.
 
## Version 1.0.4
- Interim version replaced by 1.0.5 on August 2016

## Version 1.0.3
- Align to a new BOND schema used by the Application Insights data collection endpoints.

## Version 1.0.2
- Prevent from overriding the instrumentation key using the one specified in the config when it's provided explicitly in code.
- Handle all successfull HTTP status codes and report the relevant HTTP Requests as successful.
- Handle all exceptions thrown by the ConfigurationFileLocator .

## Version 1.0.1
- The [Java agent](app-insights-java-agent.md) collects dependency information about the following:
	- HTTP calls made via HttpClient, OkHttp and RestTemplate (Spring).
	- Calls to Redis made via the Jedis client. When a configurable threshold is passed, the SDK will also fetch the call arguments.
	- JDBC calls made with Oracle DB and Apache Derby DB clients.
	- Support the 'executeBatch' query type for prepared statements – The SDK will show the statement with the number of batches.
	- Provide the query plan for JDBC clients that has support for that (MySql, PostgreSql) – The query plan is fetched only when a configurable threshold is crossed

## Version 1.0.0
- Adding support for the Application Insights writer plugin for CollectD.
- Adding support for the Application Insights Java agent.
- Fix for a compatibility issue with supporting HttpClient versions 4.2 and later.

## Version 0.9.6
- Make the Java SDK compatible with servlet v2.5 and HttpClient pre-v4.3.
- Adding support for Java EE interceptors.
- Removing redundant dependencies from the Logback appender.

## Version 0.9.5  
- Fix for an issue where custom events are not correlated with Users/Sessions due to cookie parsing errors.  
- Improved logic for resolving the location of the ApplicationInsights.xml configuration file.
- Anonymous User and Session cookies will not be generated on the server side. To implement user and session tracking for web apps, instrumentation with the JavaScript SDK is now required – cookies from the JavaScript SDK are still respected. Note that this change may cause a significant restatement of user and session counts as only user-originated sessions are being counted now.

## Version 0.9.4
- Support collecting performance counters from 32-bit Windows machines.
- Support manual tracking of dependencies using a new ```trackDependency``` method API.
- Ability to tag a telemetry item as synthetic, by adding a ```SyntheticSource``` property to the reported item.

