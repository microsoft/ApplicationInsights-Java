// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import javax.annotation.Nullable;

public enum WarEnvironmentValue {
  TOMCAT_8_JAVA_8(
      "ghcr.io/open-telemetry/opentelemetry-java-instrumentation/smoke-test-servlet-tomcat:8.5.72-jdk8-20220731.2770161172",
      "/server/webapps"),
  TOMCAT_8_JAVA_8_OPENJ9(
      "ghcr.io/open-telemetry/opentelemetry-java-instrumentation/smoke-test-servlet-tomcat:8.5.72-jdk8-openj9-20220731.2770161172",
      "/server/webapps"),
  TOMCAT_8_JAVA_11(
      "ghcr.io/open-telemetry/opentelemetry-java-instrumentation/smoke-test-servlet-tomcat:8.5.72-jdk11-20220731.2770161172",
      "/server/webapps"),
  TOMCAT_8_JAVA_11_OPENJ9(
      "ghcr.io/open-telemetry/opentelemetry-java-instrumentation/smoke-test-servlet-tomcat:8.5.72-jdk11-openj9-20220731.2770161172",
      "/server/webapps"),
  TOMCAT_8_JAVA_17(
      "ghcr.io/open-telemetry/opentelemetry-java-instrumentation/smoke-test-servlet-tomcat:8.5.72-jdk17-20220731.2770161172",
      "/server/webapps"),
  TOMCAT_8_JAVA_18(
      "ghcr.io/open-telemetry/opentelemetry-java-instrumentation/smoke-test-servlet-tomcat:8.5.72-jdk18-20220731.2770161172",
      "/server/webapps"),
  TOMCAT_8_JAVA_19(
      "ghcr.io/open-telemetry/opentelemetry-java-instrumentation/smoke-test-servlet-tomcat:8.5.72-jdk19-20220731.2770161172",
      "/server/webapps"),

  WILDFLY_13_JAVA_8(
      "ghcr.io/open-telemetry/opentelemetry-java-instrumentation/smoke-test-servlet-wildfly:13.0.0.Final-jdk8-20220731.2770161172",
      "/opt/jboss/wildfly/standalone/deployments"),
  WILDFLY_13_JAVA_8_OPENJ9(
      "ghcr.io/open-telemetry/opentelemetry-java-instrumentation/smoke-test-servlet-wildfly:13.0.0.Final-jdk8-openj9-20220731.2770161172",
      "/opt/jboss/wildfly/standalone/deployments"),

  LIBERTY_20_JAVA_8(
      "ghcr.io/open-telemetry/opentelemetry-java-instrumentation/smoke-test-servlet-liberty:21.0.0.10-jdk8-20220731.2770161172",
      "/config/apps",
      "app.war"),

  JAVA_8("openjdk:8", ""),
  JAVA_8_OPENJ9("ibm-semeru-runtimes:open-8-jdk", ""),
  JAVA_11("openjdk:11", ""),
  JAVA_11_OPENJ9("ibm-semeru-runtimes:open-11-jdk", ""),
  JAVA_17("openjdk:17", ""),
  JAVA_17_OPENJ9("ibm-semeru-runtimes:open-17-jdk", ""),
  JAVA_18("openjdk:18", ""),
  JAVA_18_OPENJ9("ibm-semeru-runtimes:open-18-jdk", ""),
  JAVA_19("openjdk:19", "");

  private final String imageName;
  private final String imageAppDir;
  // TODO (trask) remove and use "app.jar" or "app.war" everywhere
  @Nullable private final String imageAppFileName;

  WarEnvironmentValue(String imageName, String imageAppDir) {
    this(imageName, imageAppDir, null);
  }

  WarEnvironmentValue(String imageName, String imageAppDir, @Nullable String imageAppFileName) {
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
