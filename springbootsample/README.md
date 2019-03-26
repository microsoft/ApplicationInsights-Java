# Run SpringBoot Application with ApplicationInsights

This example shows how to instrument Spring Boot application with Azure Application Insights
Springboot Starter and Application Insights Java Agent to track dependencies.

### Update the configuration

Add the instrumentation key in the `application.properties` file:

`azure.application-insights.instrumentation-key=<your-ikey>` 

### Build
`gradlew :springbootsample:clean :springbootsample:build`

### Run

`java -javaagent:./springbootsample/src/main/resources/applicationinsights-agent-2.4.0-BETA-SNAPSHOT.jar -jar ./spring
 bootsample/build/libs/springbootsample-0.0.1-SNAPSHOT.jar`
