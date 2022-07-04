package com.microsoft.applicationinsights.smoketest;

public enum WarEnvironmentValue {
  TOMCAT_8_JAVA_8(
      "ghcr.io/open-telemetry/opentelemetry-java-instrumentation/smoke-test-servlet-tomcat:8.5.72-jdk8-20211216.1584506476",
      "/server/webapps"),
  TOMCAT_8_JAVA_8_OPENJ9(
      "ghcr.io/open-telemetry/opentelemetry-java-instrumentation/smoke-test-servlet-tomcat:8.5.72-jdk8-openj9-20211216.1584506476",
      "/server/webapps"),
  TOMCAT_8_JAVA_11(
      "ghcr.io/open-telemetry/opentelemetry-java-instrumentation/smoke-test-servlet-tomcat:8.5.72-jdk11-20211216.1584506476",
      "/server/webapps"),
  TOMCAT_8_JAVA_11_OPENJ9(
      "ghcr.io/open-telemetry/opentelemetry-java-instrumentation/smoke-test-servlet-tomcat:8.5.72-jdk11-openj9-20211216.1584506476",
      "/server/webapps"),
  TOMCAT_8_JAVA_17(
      "ghcr.io/open-telemetry/opentelemetry-java-instrumentation/smoke-test-servlet-tomcat:8.5.72-jdk17-20211216.1584506476",
      "/server/webapps"),

  WILDFLY_13_JAVA_8(
      "ghcr.io/open-telemetry/opentelemetry-java-instrumentation/smoke-test-servlet-wildfly:13.0.0.Final-jdk8-20211216.1584506476",
      "/opt/jboss/wildfly/standalone/deployments"),
  WILDFLY_13_JAVA_8_OPENJ9(
      "ghcr.io/open-telemetry/opentelemetry-java-instrumentation/smoke-test-servlet-wildfly:13.0.0.Final-jdk8-openj9-20211216.1584506476",
      "/opt/jboss/wildfly/standalone/deployments"),

  JAVA_8("azul/zulu-openjdk:8", ""),
  JAVA_11("azul/zulu-openjdk:11", ""),
  JAVA_17("azul/zulu-openjdk:17", "");

  private final String imageName;
  private final String imageAppDir;

  WarEnvironmentValue(String imageName, String imageAppDir) {
    this.imageName = imageName;
    this.imageAppDir = imageAppDir;
  }

  public String getImageName() {
    return imageName;
  }

  public String getImageAppDir() {
    return imageAppDir;
  }
}
