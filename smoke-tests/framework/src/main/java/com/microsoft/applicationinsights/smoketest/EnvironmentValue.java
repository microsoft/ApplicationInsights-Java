// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import javax.annotation.Nullable;

public enum EnvironmentValue {
  TOMCAT_8_JAVA_8(
      "ghcr.io/open-telemetry/opentelemetry-java-instrumentation/smoke-test-servlet-tomcat:8.5.72-jdk8-20221129.3575751672",
      "/server/webapps"),
  TOMCAT_8_JAVA_8_OPENJ9(
      "ghcr.io/open-telemetry/opentelemetry-java-instrumentation/smoke-test-servlet-tomcat:8.5.72-jdk8-openj9-20221129.3575751672",
      "/server/webapps"),
  TOMCAT_8_JAVA_11(
      "ghcr.io/open-telemetry/opentelemetry-java-instrumentation/smoke-test-servlet-tomcat:8.5.72-jdk11-20221129.3575751672",
      "/server/webapps"),
  TOMCAT_8_JAVA_11_OPENJ9(
      "ghcr.io/open-telemetry/opentelemetry-java-instrumentation/smoke-test-servlet-tomcat:8.5.72-jdk11-openj9-20221129.3575751672",
      "/server/webapps"),
  TOMCAT_8_JAVA_17(
      "ghcr.io/open-telemetry/opentelemetry-java-instrumentation/smoke-test-servlet-tomcat:8.5.72-jdk17-20221129.3575751672",
      "/server/webapps"),
  TOMCAT_8_JAVA_19(
      "ghcr.io/open-telemetry/opentelemetry-java-instrumentation/smoke-test-servlet-tomcat:8.5.72-jdk19-20221129.3575751672",
      "/server/webapps"),
  TOMCAT_8_JAVA_20(
      "ghcr.io/open-telemetry/opentelemetry-java-instrumentation/smoke-test-servlet-tomcat:8.5.72-jdk20-20221129.3575751672",
      "/server/webapps"),
  TOMCAT_8_JAVA_21(
      "ghcr.io/open-telemetry/opentelemetry-java-instrumentation/smoke-test-servlet-tomcat:8.5.98-jdk21-20240419.8749862078",
      "/server/webapps"),
      
  WILDFLY_13_JAVA_8(
      "ghcr.io/open-telemetry/opentelemetry-java-instrumentation/smoke-test-servlet-wildfly:13.0.0.Final-jdk8-20221129.3575751672",
      "/opt/jboss/wildfly/standalone/deployments"),
  WILDFLY_13_JAVA_8_OPENJ9(
      "ghcr.io/open-telemetry/opentelemetry-java-instrumentation/smoke-test-servlet-wildfly:13.0.0.Final-jdk8-openj9-20221129.3575751672",
      "/opt/jboss/wildfly/standalone/deployments"),

  LIBERTY_20_JAVA_8(
      "ghcr.io/open-telemetry/opentelemetry-java-instrumentation/smoke-test-servlet-liberty:21.0.0.10-jdk8-20221129.3575751672",
      "/config/apps",
      "app.war"),

  JAVA_8("eclipse-temurin:8", ""),
  JAVA_8_OPENJ9("ibm-semeru-runtimes:open-8-jdk", ""),
  JAVA_11("eclipse-temurin:11", ""),
  JAVA_11_OPENJ9("ibm-semeru-runtimes:open-11-jdk", ""),
  JAVA_17("eclipse-temurin:17", ""),
  JAVA_17_OPENJ9("ibm-semeru-runtimes:open-17-jdk", ""),
  JAVA_20("eclipse-temurin:20", ""),
  JAVA_20_OPENJ9("ibm-semeru-runtimes:open-20-jdk", ""),
  JAVA_21("openjdk:21", ""); // still in preview

  private final String imageName;
  private final String imageAppDir;
  // TODO (trask) remove and use "app.jar" or "app.war" everywhere
  @Nullable private final String imageAppFileName;

  EnvironmentValue(String imageName, String imageAppDir) {
    this(imageName, imageAppDir, null);
  }

  EnvironmentValue(String imageName, String imageAppDir, @Nullable String imageAppFileName) {
    this.imageName = imageName;
    this.imageAppDir = imageAppDir;
    this.imageAppFileName = imageAppFileName;
  }

  public String getImageName() {
    return imageName;
  }

  public String getImageAppDir() {
    return imageAppDir;
  }

  @Nullable
  public String getImageAppFileName() {
    return imageAppFileName;
  }
}
