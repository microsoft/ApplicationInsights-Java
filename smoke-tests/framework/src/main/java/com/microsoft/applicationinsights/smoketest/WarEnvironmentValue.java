/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.microsoft.applicationinsights.smoketest;

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

  WILDFLY_13_JAVA_8(
      "ghcr.io/open-telemetry/opentelemetry-java-instrumentation/smoke-test-servlet-wildfly:13.0.0.Final-jdk8-20220731.2770161172",
      "/opt/jboss/wildfly/standalone/deployments"),
  WILDFLY_13_JAVA_8_OPENJ9(
      "ghcr.io/open-telemetry/opentelemetry-java-instrumentation/smoke-test-servlet-wildfly:13.0.0.Final-jdk8-openj9-20220731.2770161172",
      "/opt/jboss/wildfly/standalone/deployments"),

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
