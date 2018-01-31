# ApplicationInsights-Java

## Introduction

This is the repository of the Java SDK for [Visual Studio Application Insights](https://acom-prod-uswest-01.azurewebsites.net/documentation/articles/app-insights-overview/). Application Insights is a service that monitors the availability, performance and usage of your application. The SDK sends telemetry about the performance and usage of your app to the Application Insights service where your data can be visualized in the [Azure Portal](https://portal.azure.com). The SDK automatically collects telemetry about HTTP requests, dependencies, and exceptions. You can also use the SDK to send your own events and trace logs. 

Please refer to:

* [Get started with Application Insights in a Java web project](https://azure.microsoft.com/documentation/articles/app-insights-java-get-started/) 
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

* Download the latest version of [Application Insights Java SDK](https://aka.ms/aijavasdk) and replace the old ones. 
 
Compare the old and new `ApplicationInsights.xml`. Many of the changes you see are because we added and removed modules. Reinstate any customizations that you made.


## Microsoft Open Source Code of Conduct

This project has adopted the [Microsoft Open Source Code of Conduct](https://opensource.microsoft.com/codeofconduct/). For more information see the [Code of Conduct FAQ](https://opensource.microsoft.com/codeofconduct/faq/) or contact [opencode@microsoft.com](mailto:opencode@microsoft.com) with any additional questions or comments.
