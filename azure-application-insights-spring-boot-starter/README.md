
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

Sending custom telemetry:
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

To configure application to send logs to the application insights, follow the instructions from (Spring Boot logging documentation)[https://docs.spring.io/spring-boot/docs/current/reference/html/howto-logging.html] to configure custom logback or log4j2 appender.
`logback-spring.xml`
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

You can register own telemetry module, processor or initializer by defining it as a bean in your configuration:
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

Configure more parameters using `application.properties`:
```properties
# Enable/Disable tracking
azure.application-insights.enabled=true

# Instrumentation key from the Azure Portal
azure.application-insights.instrumentation-key=00000000-0000-0000-0000-000000000000

# Logging type [console, file]
azure.application-insights.logger.type=console
# Logging level [all, trace, info, warn, error, off]
azure.application-insights.logger.level=info

# Enable/Disable default telemetry modules
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
