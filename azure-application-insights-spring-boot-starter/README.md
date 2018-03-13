
**Application Insights Spring Boot Starter**

This Starter provides you the minimal and required configuration to use Application Insights in your Spring Boot application.

**Requirements**
Spring Boot 1.5+ or 2.0+

**Quick Start**

*1. Add dependency*
Gradle:
```groovy
compile "com.microsoft.azure:azure-application-insights-spring-boot-starter:${version}"
```

Maven:
```xml
<dependency>
  <groupId>com.microsoft.azure</groupId>
  <artifactId>azure-application-insights-spring-boot-starter</artifactId>
  <version>${version}</version>
</dependency>
```

*2. Provide Instrumentation Key*

Add property
```
azure.application-insights.instrumentation-key=<key from the Azure Portal>
```
into your `application.properties`

*3. Run your application*

Start your spring boot application as usual and in few minutes you'll start getting events.

**Additional Configuration**

#### Sending custom telemetry
```java
@RestController
public class TelemetryController {

    @Autowired
    private TelemetryClient telemetryClient;

    @RequestMapping("/telemetry")
    public void telemetry() {
        telemetryClient.trackEvent("my event");
    }
}
```


#### Sending logs to the application insight

Follow the instructions from [Spring Boot logging documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/howto-logging.html) to configure custom logback or log4j2 appender.

`logback-spring.xml`:
```xml
<appender name="aiAppender"
  class="com.microsoft.applicationinsights.logback.ApplicationInsightsAppender">
</appender>
<root level="trace">
  <appender-ref ref="aiAppender" />
</root>
```

`log4j2.xml`:
```xml
<Configuration packages="com.microsoft.applicationinsights.log4j.v2">
  <Appenders>
    <ApplicationInsightsAppender name="aiAppender" />
  </Appenders>
  <Loggers>
    <Root level="trace">
      <AppenderRef ref="aiAppender"/>
    </Root>
  </Loggers>
</Configuration>
```

#### Register own telemetry module, processor or initializer by defining it as a bean in the configuration
```java
@SpringBootApplication
public class MyApplication {

    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }

    @Bean
    public TelemetryModule myTelemetryModule() {
        return new MyTelemetryModule();
    }

    @Bean
    public TelemetryInitializer myTelemetryInitializer() {
        return new MyTelemetryInitializer();
    }

    @Bean
    public TelemetryProcessor myTelemetryProcessor() {
        return new MyTelemetryProcessor();
    }

    @Bean
    public ContextInitializer myContextInitializer() {
        return new MyContextInitializer();
    }
}
```


#### Configure more parameters using `application.properties`
```properties
# Instrumentation key from the Azure Portal. Required.
azure.application-insights.instrumentation-key=00000000-0000-0000-0000-000000000000

# Enable/Disable tracking. Default value: true.
azure.application-insights.enabled=true

# Enable/Disable web modules. Default value: true.
azure.application-insights.web.enabled=true

# Logging type [console, file]. Default value: console.
azure.application-insights.logger.type=console
# Logging level [all, trace, info, warn, error, off]. Default value: error.
azure.application-insights.logger.level=error

# Enable/Disable developer mode, all telemetry will be sent immediately without batching. Significantly affects performance and should be used only in developer environment. Default value: false.
azure.application-insights.channel.in-process.developer-mode=false
# Endpoint address, Default value: https://dc.services.visualstudio.com/v2/track.
azure.application-insights.channel.in-process.endpoint-address=https://dc.services.visualstudio.com/v2/track
# Maximum count of telemetries that will be batched before sending. Must be between 1 and 1000. Default value: 500.
azure.application-insights.channel.in-process.max-telemetry-buffer-capacity=500
# Interval to send telemetry. Must be between 1 and 300. Default value: 5 seconds.
azure.application-insights.channel.in-process.flush-interval-in-seconds=5
# Size of disk that we can use. Must be between 1 and 1000. Default value: 10 megabytes.
azure.application-insights.channel.in-process.max-transmission-storage-files-capacity-in-mb=10
# Enable/Disable throttling on sending telemetry data. Default value: true.
azure.application-insights.channel.in-process.throttling=true

# Percent of telemetry events that will be sent to Application Insights. Must be between 0.0 and 100.0. Default value: 100 (all telemetry events).
azure.application-insights.telemetry-processor.sampling.percentage=100
# If set only telemetry of specified types will be included. Default value: all telemetries are included;
azure.application-insights.telemetry-processor.sampling.include=
# If set telemetry of specified type will be excluded. Default value: none telemetries are excluded.
azure.application-insights.telemetry-processor.sampling.exclude=

# Enable/Disable default telemetry modules. Default value: true.
azure.application-insights.default-modules.ProcessPerformanceCountersModule.enabled=true
azure.application-insights.default-modules.WebRequestTrackingTelemetryModule.enabled=true
azure.application-insights.default-modules.WebSessionTrackingTelemetryModule.enabled=true
azure.application-insights.default-modules.WebUserTrackingTelemetryModule.enabled=true
azure.application-insights.default-modules.WebPerformanceCounterModule.enabled=true
azure.application-insights.default-modules.WebOperationIdTelemetryInitializer.enabled=true
azure.application-insights.default-modules.WebOperationNameTelemetryInitializer.enabled=true
azure.application-insights.default-modules.WebSessionTelemetryInitializer.enabled=true
azure.application-insights.default-modules.WebUserTelemetryInitializer.enabled=true
azure.application-insights.default-modules.WebUserAgentTelemetryInitializer.enabled=true
```
