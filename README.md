# Application Insights for Java
Build & Unit Tests: ![aibuild](https://mseng.visualstudio.com/_apis/public/build/definitions/96a62c4a-58c2-4dbb-94b6-5979ebc7f2af/5311/badge)

Smoke Tests: ![aismoke](https://mseng.visualstudio.com/_apis/public/build/definitions/96a62c4a-58c2-4dbb-94b6-5979ebc7f2af/6159/badge)

## Introduction

This is the repository of the Java SDK for [Azure Application Insights](https://azure.microsoft.com/en-us/services/application-insights/). Application Insights is a service that monitors the availability, performance and usage of your application. The SDK sends telemetry about the performance and usage of your app to the Application Insights service where your data can be visualized in the [Azure Portal](https://portal.azure.com). The SDK automatically collects telemetry about HTTP requests, dependencies, and exceptions. You can also use the SDK to send your own events and trace logs. 

For more information please refer to:

* [Getting started with Application Insights in a Java web project](https://azure.microsoft.com/documentation/articles/app-insights-java-get-started/) 
* [Application Insights overview](https://azure.microsoft.com/services/application-insights/)

The following packages are built in this repository:

- Base API and channel: [![applicationinsights-core](https://img.shields.io/maven-central/v/com.microsoft.azure/applicationinsights-core.svg)](https://search.maven.org/remote_content?g=com.microsoft.azure&a=applicationinsights-core&v=latest)
- Web applications instrumentation: [![applicationinsights-web](https://img.shields.io/maven-central/v/com.microsoft.azure/applicationinsights-web.svg)](https://search.maven.org/remote_content?g=com.microsoft.azure&a=applicationinsights-web&v=latest)
- Logback adaptor: [![applicationinsights-logging-logback](https://img.shields.io/maven-central/v/com.microsoft.azure/applicationinsights-logging-logback.svg)](https://search.maven.org/remote_content?g=com.microsoft.azure&a=applicationinsights-logging-logback&v=latest)
- Log4J 2 adaptor: [![applicationinsights-logging-log4j2](https://img.shields.io/maven-central/v/com.microsoft.azure/applicationinsights-logging-log4j2.svg)](https://search.maven.org/remote_content?g=com.microsoft.azure&a=applicationinsights-logging-log4j2&v=latest)
- Log4J 1.2 adaptor: [![applicationinsights-logging-log4j1_2](https://img.shields.io/maven-central/v/com.microsoft.azure/applicationinsights-logging-log4j1_2.svg)](https://search.maven.org/remote_content?g=com.microsoft.azure&a=applicationinsights-logging-log4j1_2&v=latest)

## To upgrade to the latest SDK 

After you upgrade, you'll need to merge back any customizations you made to `ApplicationInsights.xml`. Take a copy of it to compare with the new file.

*If you're using Maven or Gradle*

1. If you specified a particular version number in `pom.xml` or `build.gradle`, update it.
2. Refresh your project's dependencies.

*Otherwise*

* Download the latest version of [Application Insights Java SDK](https://docs.microsoft.com/en-us/azure/application-insights/app-insights-java-get-started), [scroll down to the getting started section](https://docs.microsoft.com/en-us/azure/application-insights/app-insights-java-get-started) and follow the instructions to manually download the SDK and replace the old `.jar` files.
 
Compare the old and new `ApplicationInsights.xml`. Many of the changes you see are because we added and removed modules. Reinstate any customizations that you made.

## Application Insights for Java Roadmap
The Application Insights team have been hard at work to deliver the next wave of features for our Java support and experience. Below is an outline of the features and improvements that are planned for our next several releases, which are targeted to be completed for mid-year 2018.

### Application Insights for Java SDK 2.0
In December [we released the beta version of the Application Insights for Java 2.0 SDK](https://github.com/Microsoft/ApplicationInsights-Java/releases/tag/v2.0.0-BETA). That release introduced support for cross-component telemetry correlation and fixed rate sampling. For the final release, we'll be addressing any reliability issues that are found or reported.

### Documentation Improvements
The Application Insights team believes that documentation is important to the overall successfulness of our users. As such, we strive to continually improve our documentation. The full list of [documentation changes](https://github.com/Microsoft/ApplicationInsights-Java/issues?q=is%3Aissue+is%3Aopen+label%3A%22Documentation+Changes%22) can be found in our [issue tracker](https://github.com/Microsoft/ApplicationInsights-Java/issues). Feel free to [open a new issue](https://github.com/Microsoft/ApplicationInsights-Java/issues/new) to report incorrect or unclear documentation.

### Spring Boot Starter
The Spring Boot framework is wildly popular, and while Application Insights for Java already supports Spring Boot, we'd like to make it easier to get started with. To do that, we're going to be releasing a [Spring Boot Starter](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#using-boot-starter) in the coming months.

### Adaptive Sampling
While our 2.0 SDK will provide full support for fixed rate sampling, we plan to add support for adaptive sampling. Adaptive sampling provides fine-grained controls over a variable sampling rate when traffic to an application fluctuates and finding a fixed rate to sample with would otherwise be difficult.

### Support for Java 9
The Application Insights for Java SDK currently supports Java 7 and 8. We are working to bring full support for customers who have upgraded their applications to Java 9 as well.


## Microsoft Open Source Code of Conduct

This project has adopted the [Microsoft Open Source Code of Conduct](https://opensource.microsoft.com/codeofconduct/). For more information see the [Code of Conduct FAQ](https://opensource.microsoft.com/codeofconduct/faq/) or contact [opencode@microsoft.com](mailto:opencode@microsoft.com) with any additional questions or comments.
