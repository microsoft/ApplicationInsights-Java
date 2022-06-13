# Java runtime attachment for Application Insights

If you can't update the JVM arguments to attach the Application Insights agent for Java (_
-javaagent:path/to/applicationinsights-agent-3.2.11.jar_), this project allows you to do attachment
programmatically.

The ```com.microsoft.applicationinsights.attach.ApplicationInsights``` class has an ```attach()```
method that triggers the attachment of the agent. _The attachment must be requested at the beginning
of the ```main``` method._ The runtime attachment feature is initially developed for Spring Boot
applications:

```java

@SpringBootApplication
public class SpringBootApp {

  public static void main(String[] args) {
    ApplicationInsights.attach();
    SpringApplication.run(SpringBootApp.class, args);
  }

}
```
