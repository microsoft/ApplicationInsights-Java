# Run Application with SpringBoot Application

This is very simple example demonstrating how to run instrument Springboot application with Azure Application Insights
Springboot Starter and Application Insights Java Agent to track dependencies.


### How to build the application?
1. Add the instrumentation key in the application.properties by updating the your-ikey section with instrumentation key.
`azure.application-insights.instrumentation-key=<your-ikey>` 

2. In order to build the application run the command:
`gradlew :springbootsample:clean :springbootsample:build`

This will clean and build the sample application. 

### How to run the application?

In order to run the application navigate to the directory

`java -javaagent:./springbootsample/src/main/resources/applicationinsights-agent-2.4.0-BETA-SNAPSHOT.jar -jar ./spring
 bootsample/build/libs/springbootsample-0.0.1-SNAPSHOT.jar`