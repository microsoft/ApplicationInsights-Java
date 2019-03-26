# Run SpringBoot Application with ApplicationInsights

This example shows how to instrument Spring Boot application with Azure Application Insights
Springboot Starter and Application Insights Java Agent to track dependencies.


### How to Build the application?
1. Add the instrumentation key in the application.properties by updating the your-ikey section with instrumentation key.
`azure.application-insights.instrumentation-key=<your-ikey>` 

2. In order to build the application run the following command:
`gradlew :springbootsample:clean :springbootsample:build`

### How to Run the application?

To start the application run the following command:

`java -javaagent:./springbootsample/src/main/resources/applicationinsights-agent-2.4.0-BETA-SNAPSHOT.jar -jar ./spring
 bootsample/build/libs/springbootsample-0.0.1-SNAPSHOT.jar`