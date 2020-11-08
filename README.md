# Application Insights for Java
[![Hits](https://hits.seeyoufarm.com/api/count/incr/badge.svg?url=https%3A%2F%2Fgithub.com%2Fmicrosoft%2FApplicationInsights-Java&count_bg=%2379C83D&title_bg=%23555555&icon=&icon_color=%23E7E7E7&title=PAGE+VIEWS&edge_flat=false)](https://hits.seeyoufarm.com)

| Build & Unit Tests | Smoke Tests |
:-:|:-:
| ![Build + Unit Tests](https://mseng.visualstudio.com/_apis/public/build/definitions/96a62c4a-58c2-4dbb-94b6-5979ebc7f2af/5311/badge "Build & Unit Tests' Status") | ![Smoke Tests](https://mseng.visualstudio.com/_apis/public/build/definitions/96a62c4a-58c2-4dbb-94b6-5979ebc7f2af/6159/badge "Smoke Tests' Status") |

## Introduction

This is the repository of the Java SDK for [Azure Application Insights](https://azure.microsoft.com/en-us/services/application-insights/). Application Insights is a service that monitors the availability, performance and usage of your application. The SDK sends telemetry about the performance and usage of your app to the Application Insights service where your data can be visualized in the [Azure Portal](https://portal.azure.com). The SDK automatically collects telemetry about HTTP requests, dependencies, and exceptions. You can also use the SDK to send your own events and trace logs.

For more information please refer to:

* [Getting started with Application Insights in a Java web project](https://azure.microsoft.com/documentation/articles/app-insights-java-get-started/)
* [Application Insights overview](https://azure.microsoft.com/services/application-insights/)
* [Application Insights with SpringBoot](https://docs.microsoft.com/en-us/java/azure/spring-framework/configure-spring-boot-java-applicationinsights)

The following packages are built in this repository:

| Base API and channel: | Web applications instrumentation: | Application Insights SpringBoot Starter: |
|-|-|-|
[![applicationinsights-core](https://img.shields.io/maven-central/v/com.microsoft.azure/applicationinsights-core.svg)](https://search.maven.org/remote_content?g=com.microsoft.azure&a=applicationinsights-core&v=latest) | [![applicationinsights-web](https://img.shields.io/maven-central/v/com.microsoft.azure/applicationinsights-web.svg)](https://search.maven.org/remote_content?g=com.microsoft.azure&a=applicationinsights-web&v=latest) | [![applicationinsights-spring-boot-starter](https://img.shields.io/maven-central/v/com.microsoft.azure/applicationinsights-spring-boot-starter.svg)](https://search.maven.org/remote_content?g=com.microsoft.azure&a=applicationinsights-spring-boot-starter&v=latest) |

| Logback adaptor: | Log4J 2 adaptor: | Log4J 1.2 adaptor: |
|-|-|-|
| [![applicationinsights-logging-logback](https://img.shields.io/maven-central/v/com.microsoft.azure/applicationinsights-logging-logback.svg)](https://search.maven.org/remote_content?g=com.microsoft.azure&a=applicationinsights-logging-logback&v=latest) | [![applicationinsights-logging-log4j2](https://img.shields.io/maven-central/v/com.microsoft.azure/applicationinsights-logging-log4j2.svg)](https://search.maven.org/remote_content?g=com.microsoft.azure&a=applicationinsights-logging-log4j2&v=latest) | [![applicationinsights-logging-log4j1_2](https://img.shields.io/maven-central/v/com.microsoft.azure/applicationinsights-logging-log4j1_2.svg)](https://search.maven.org/remote_content?g=com.microsoft.azure&a=applicationinsights-logging-log4j1_2&v=latest) |

## To upgrade to the latest SDK

After you upgrade, you'll need to merge back any customizations you made to `ApplicationInsights.xml`. Take a copy of it to compare with the new file.

*If you're using Maven or Gradle*

1. If you specified a particular version number in `pom.xml` or `build.gradle`, update it.
2. Refresh your project's dependencies.

*Otherwise*

* Download the latest version of [Application Insights Java SDK](https://docs.microsoft.com/en-us/azure/application-insights/app-insights-java-get-started), [scroll down to the getting started section](https://docs.microsoft.com/en-us/azure/application-insights/app-insights-java-get-started) and follow the instructions to manually download the SDK and replace the old `.jar` files.

Compare the old and new `ApplicationInsights.xml`. Many of the changes you see are because we added and removed modules. Reinstate any customizations that you made.

## Microsoft Open Source Code of Conduct

This project has adopted the [Microsoft Open Source Code of Conduct](https://opensource.microsoft.com/codeofconduct/). For more information see the [Code of Conduct FAQ](https://opensource.microsoft.com/codeofconduct/faq/) or contact [opencode@microsoft.com](mailto:opencode@microsoft.com) with any additional questions or comments.
